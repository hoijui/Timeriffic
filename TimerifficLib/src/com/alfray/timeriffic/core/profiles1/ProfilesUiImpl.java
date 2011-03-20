/*
 * Project: Timeriffic
 * Copyright (C) 2011 rdrr labs gmail com,
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alfray.timeriffic.core.profiles1;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.app.TimerifficApp;
import com.alfray.timeriffic.app.UpdateReceiver;
import com.alfray.timeriffic.core.app.BackupWrapper;
import com.alfray.timeriffic.core.prefs.PrefsValues;
import com.alfray.timeriffic.core.settings.SettingFactory;
import com.alfray.timeriffic.core.utils.AgentWrapper;
import com.alfray.timeriffic.ui.EditProfileUI;
import com.alfray.timeriffic.ui.ErrorReporterUI;
import com.alfray.timeriffic.ui.GlobalStatus;
import com.alfray.timeriffic.ui.GlobalToggle;
import com.alfray.timeriffic.ui.IntroUI;
import com.alfray.timeriffic.ui.PrefsUI;

public class ProfilesUiImpl {

    private static final boolean DEBUG = true;
    public static final String TAG = ProfilesUiImpl.class.getSimpleName();

    public static final int DATA_CHANGED = 42;
    static final int SETTINGS_UPDATED = 43;
    static final int CHECK_SERVICES   = 44;

    static final int DIALOG_RESET_CHOICES = 0;
    public static final int DIALOG_DELETE_ACTION  = 1;
    public static final int DIALOG_DELETE_PROFILE = 2;
    static final int DIALOG_CHECK_SERVICES = 3;

    private final Activity mActivity;

    private ListView mProfilesList;
    private ProfileCursorAdapter mAdapter;
    private LayoutInflater mLayoutInflater;
    private ProfilesDB mProfilesDb;

    private AgentWrapper mAgentWrapper;
    private PrefsValues mPrefsValues;
    private Drawable mGrayDot;
    private Drawable mGreenDot;
    private Drawable mPurpleDot;
    private Drawable mCheckOn;
    private Drawable mCheckOff;

    private GlobalToggle mGlobalToggle;
    private GlobalStatus mGlobalStatus;

    private long mTempDialogRowId;
    private String mTempDialogTitle;

    private Cursor mCursor;

    public static class ColIndexes {
        public int mIdColIndex;
        public int mTypeColIndex;
        public int mDescColIndex;
        public int mEnableColIndex;
        public int mProfIdColIndex;
    };

    private ColIndexes mColIndexes = new ColIndexes();
    private BackupWrapper mBackupWrapper;

    public ProfilesUiImpl(Activity profileActivity) {
        mActivity = profileActivity;
    }

    /**
     * Called when the activity is created.
     * <p/>
     * Initializes row indexes and buttons.
     * Profile list & db is initialized in {@link #onResume()}.
     */
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, String.format("Started %s", getClass().getSimpleName()));

        mActivity.setContentView(R.layout.profiles_screen);
        mLayoutInflater = mActivity.getLayoutInflater();

        mPrefsValues = new PrefsValues(mActivity);
        mGrayDot   = mActivity.getResources().getDrawable(R.drawable.dot_gray);
        mGreenDot  = mActivity.getResources().getDrawable(R.drawable.dot_green);
        mPurpleDot = mActivity.getResources().getDrawable(R.drawable.dot_purple);
        mCheckOn   = mActivity.getResources().getDrawable(R.drawable.btn_check_on);
        mCheckOff  = mActivity.getResources().getDrawable(R.drawable.btn_check_off);

        initButtons();
        showIntroAtStartup();

        mAgentWrapper = new AgentWrapper();
        mAgentWrapper.start(mActivity);
        mAgentWrapper.event(AgentWrapper.Event.OpenProfileUI);
    }

    private void showIntroAtStartup() {
        final TimerifficApp tapp = TimerifficApp.getInstance(mActivity);
        if (tapp.isFirstStart() && mGlobalToggle != null) {
            final Runnable action = new Runnable() {
                @Override
                public void run() {
                    showIntro(false, true);
                    tapp.setFirstStart(false);
                }
            };

            final ViewTreeObserver obs = mGlobalToggle.getViewTreeObserver();
            obs.addOnPreDrawListener(new OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mGlobalToggle.postDelayed(action, 200 /*delayMillis*/);
                    ViewTreeObserver obs2 = mGlobalToggle.getViewTreeObserver();
                    obs2.removeOnPreDrawListener(this);
                    return true;
                }
            });
        }
    }

    private void showIntro(boolean force, boolean checkServices) {

        // force is set when this comes from Menu > About
        boolean showIntro = force;

        // if not forcing, does the user wants to see the intro?
        // true by default, unless disabled in the prefs
        if (!showIntro) {
            showIntro = !mPrefsValues.isIntroDismissed();
        }

        // user doesn't want to see it... but we force it anyway if this is
        // a version upgrade
        int currentVersion = -1;
        try {
            currentVersion = mActivity.getPackageManager().getPackageInfo(
                    mActivity.getPackageName(), 0).versionCode;
            // the version number is in format n.m.kk where n.m is the
            // actual version number, incremented for features and kk is
            // a sub-minor index of minor fixes. We clear these last digits
            // out and don't force to see the intro for these minor fixes.
            currentVersion = (currentVersion / 100) * 100;
        } catch (NameNotFoundException e) {
            // ignore. should not happen.
        }
        if (!showIntro && currentVersion > 0) {
            showIntro = currentVersion > mPrefsValues.getLastIntroVersion();
        }

        if (showIntro) {
            // mark it as seen
            if (currentVersion > 0) {
                mPrefsValues.setLastIntroVersion(currentVersion);
            }

            Intent i = new Intent(mActivity, IntroUI.class);
            if (force) i.putExtra(IntroUI.EXTRA_NO_CONTROLS, true);
            mActivity.startActivityForResult(i, CHECK_SERVICES);
            return;
        }

        if (checkServices) {
            onCheckServices();
        }
    }

    public Activity getActivity() {
        return mActivity;
    }

    public Cursor getCursor() {
        return mCursor;
    };

    public ColIndexes getColIndexes() {
        return mColIndexes;
    }

    public ProfilesDB getProfilesDb() {
        return mProfilesDb;
    }

    public Drawable getGrayDot() {
        return mGrayDot;
    }

    public Drawable getGreenDot() {
        return mGreenDot;
    }

    public Drawable getPurpleDot() {
        return mPurpleDot;
    }

    public Drawable getCheckOff() {
        return mCheckOff;
    }

    public Drawable getCheckOn() {
        return mCheckOn;
    }

    /**
     * Initializes the profile list widget with a cursor adapter.
     * Creates a db connection.
     */
    private void initProfileList() {

        Log.d(TAG, "init profile list");

        if (mProfilesList == null) {
            mProfilesList = (ListView) mActivity.findViewById(R.id.profilesList);
            mProfilesList.setEmptyView(mActivity.findViewById(R.id.empty));

            mProfilesList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
                    if (DEBUG) Log.d(TAG, String.format("onItemClick: pos %d, id %d", position, id));
                    BaseHolder h = null;
                    h = getHolder(null, clickedView);
                    if (h != null) h.onItemSelected();
                }
            });

            mProfilesList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View listview, ContextMenuInfo menuInfo) {
                    if (DEBUG) Log.d(TAG, "onCreateContextMenu");
                    BaseHolder h = null;
                    h = getHolder(menuInfo, null);
                    if (h != null) h.onCreateContextMenu(menu);
                }
            });
        }

        if (mProfilesDb == null) {
            mProfilesDb = new ProfilesDB();
            mProfilesDb.onCreate(mActivity);

            String next = mPrefsValues.getStatusNextTS();
            if (next == null) {
                // schedule a profile check to initialize the last/next status
                requestSettingsCheck(UpdateReceiver.TOAST_NONE);
            }
        }

        if (mAdapter == null) {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            mCursor = mProfilesDb.query(
                    -1, //id
                    new String[] {
                        Columns._ID,
                        Columns.TYPE,
                        Columns.DESCRIPTION,
                        Columns.IS_ENABLED,
                        Columns.PROFILE_ID,
                        // enable these only if they are actually used here
                        //Columns.HOUR_MIN,
                        //Columns.DAYS,
                        //Columns.ACTIONS,
                        //Columns.NEXT_MS
                    } , //projection
                    null, //selection
                    null, //selectionArgs
                    null //sortOrder
                    );

            mColIndexes.mIdColIndex = mCursor.getColumnIndexOrThrow(Columns._ID);
            mColIndexes.mTypeColIndex = mCursor.getColumnIndexOrThrow(Columns.TYPE);
            mColIndexes.mDescColIndex = mCursor.getColumnIndexOrThrow(Columns.DESCRIPTION);
            mColIndexes.mEnableColIndex = mCursor.getColumnIndexOrThrow(Columns.IS_ENABLED);
            mColIndexes.mProfIdColIndex = mCursor.getColumnIndexOrThrow(Columns.PROFILE_ID);

            mAdapter = new ProfileCursorAdapter(this, mColIndexes, mLayoutInflater);
            mProfilesList.setAdapter(mAdapter);

            Log.d(TAG, String.format("adapter count: %d", mProfilesList.getCount()));
        }
    }

    /**
     * Called when activity is resumed, or just after creation.
     * <p/>
     * Initializes the profile list & db.
     */
    public void onResume() {
        initOnResume();
    }

    private void initOnResume() {
        initProfileList();
        setDataListener();
    }

    /**
     * Called when the activity is getting paused. It might get destroyed
     * at any point.
     * <p/>
     * Reclaim all views (so that they tag's cursor can be cleared).
     * Destroys the db connection.
     */
    public void onPause() {
        removeDataListener();
    }

    public void onStop() {
        mAgentWrapper.stop(mActivity);
    }

    private void setDataListener() {
        TimerifficApp app = TimerifficApp.getInstance(mActivity);
        if (app != null) {
            app.setDataListener(new Runnable() {
                @Override
                public void run() {
                    onDataChanged(true /*backup*/);
                }
            });
            // No backup on the first init
            onDataChanged(false /*backup*/);
        }
    }

    private void removeDataListener() {
        TimerifficApp app = TimerifficApp.getInstance(mActivity);
        if (app != null) {
            app.setDataListener(null);
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mTempDialogRowId = savedInstanceState.getLong("dlg_rowid");
        mTempDialogTitle = savedInstanceState.getString("dlg_title");
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("dlg_rowid", mTempDialogRowId);
        outState.putString("dlg_title", mTempDialogTitle);
    }

    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
            mAdapter = null;
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (mProfilesDb != null) {
            mProfilesDb.onDestroy();
            mProfilesDb = null;
        }
        if (mProfilesList != null) {
            ArrayList<View> views = new ArrayList<View>();
            mProfilesList.reclaimViews(views);
            mProfilesList.setAdapter(null);
            mProfilesList = null;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case DATA_CHANGED:
            onDataChanged(true /*backup*/);
            requestSettingsCheck(UpdateReceiver.TOAST_IF_CHANGED);
            break;
        case SETTINGS_UPDATED:
            updateGlobalState();
            requestSettingsCheck(UpdateReceiver.TOAST_IF_CHANGED);
            break;
        case CHECK_SERVICES:
            onCheckServices();
        }
    }

    private void onDataChanged(boolean backup) {
        if (mCursor != null) mCursor.requery();
        mAdapter = null;
        initProfileList();
        updateGlobalState();

        if (backup) {
            if (mBackupWrapper == null) mBackupWrapper = new BackupWrapper(mActivity);
            mBackupWrapper.dataChanged();
        }
    }

    public Dialog onCreateDialog(int id) {

        // In case of configuration change (e.g. screen rotation),
        // the activity is restored but onResume hasn't been called yet
        // so we do it now.
        initOnResume();

        switch(id) {
        case DIALOG_RESET_CHOICES:
            return createDialogResetChoices();
        case DIALOG_DELETE_PROFILE:
            return createDeleteProfileDialog();
        case DIALOG_DELETE_ACTION:
            return createDialogDeleteTimedAction();
        case DIALOG_CHECK_SERVICES:
            return createDialogCheckServices();
        default:
            return null;
        }
    }


    private void onCheckServices() {
        String msg = getCheckServicesMessage();
        if (DEBUG) Log.d(TAG, "Check Services: " + msg == null ? "null" : msg);
        if (msg.length() > 0 && mPrefsValues.getCheckService()) {
            mActivity.showDialog(DIALOG_CHECK_SERVICES);
        }
    }

    private String getCheckServicesMessage() {

        SettingFactory factory = SettingFactory.getInstance();
        StringBuilder sb = new StringBuilder();

        if (!factory.getSetting(Columns.ACTION_RING_VOLUME).isSupported(mActivity)) {
            sb.append("\n- ").append(mActivity.getString(R.string.checkservices_miss_audio_service));
        }
        if (!factory.getSetting(Columns.ACTION_WIFI).isSupported(mActivity)) {
            sb.append("\n- ").append(mActivity.getString(R.string.checkservices_miss_wifi_service));
        }
        if (!factory.getSetting(Columns.ACTION_AIRPLANE).isSupported(mActivity)) {
            sb.append("\n- ").append(mActivity.getString(R.string.checkservices_miss_airplane));
        }
        if (!factory.getSetting(Columns.ACTION_BRIGHTNESS).isSupported(mActivity)) {
            sb.append("\n- ").append(mActivity.getString(R.string.checkservices_miss_brightness));
        }

        // Bluetooth and APNDroid are not essential settings. We can't just bug the
        // user at start if they are missing (which is also highly probably, especially for
        // APNDroid). So here is not the right place to check for them.
        //
        // if (!factory.getSetting(Columns.ACTION_BLUETOOTH).isSupported(this)) {
        //     sb.append("\n- ").append(getString(R.string.checkservices_miss_bluetooh));
        // }
        // if (!factory.getSetting(Columns.ACTION_APN_DROID).isSupported(this)) {
        //     sb.append("\n- ").append(getString(R.string.checkservices_miss_apndroid));
        // }

        if (sb.length() > 0) {
            sb.insert(0, mActivity.getString(R.string.checkservices_warning));
        }

        return sb.toString();
    }

    private Dialog createDialogCheckServices() {
        Builder b = new AlertDialog.Builder(mActivity);

        b.setTitle(R.string.checkservices_dlg_title);
        b.setMessage(getCheckServicesMessage());
        b.setPositiveButton(R.string.checkservices_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.removeDialog(DIALOG_CHECK_SERVICES);
            }
        });
        b.setNegativeButton(R.string.checkservices_skip_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefsValues.setCheckService(false);
                mActivity.removeDialog(DIALOG_CHECK_SERVICES);
            }
        });


        b.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mActivity.removeDialog(DIALOG_CHECK_SERVICES);
            }
        });

        return b.create();
    }

    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuInfo info = item.getMenuInfo();
        BaseHolder h = getHolder(info, null);
        if (h != null) {
            h.onContextMenuSelected(item);
            return true;
        }

        return false;
    }

    private BaseHolder getHolder(ContextMenuInfo menuInfo, View selectedView) {
        if (selectedView == null && menuInfo instanceof AdapterContextMenuInfo) {
            selectedView = ((AdapterContextMenuInfo) menuInfo).targetView;
        }

        Object tag = selectedView.getTag();
        if (tag instanceof BaseHolder) {
            return (BaseHolder) tag;
        }

        Log.d(TAG, "Holder missing");
        return null;
    }

    /**
     * Initializes the list-independent buttons: global toggle, check now.
     */
    private void initButtons() {
        mGlobalToggle = (GlobalToggle) mActivity.findViewById(R.id.global_toggle);
        mGlobalStatus = (GlobalStatus) mActivity.findViewById(R.id.global_status);

        updateGlobalState();

        mGlobalToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrefsValues.setServiceEnabled(!mPrefsValues.isServiceEnabled());
                updateGlobalState();
                requestSettingsCheck(UpdateReceiver.TOAST_ALWAYS);
            }
        });

        mGlobalStatus.setWindowVisibilityChangedCallback(new Runnable() {
            @Override
            public void run() {
                updateGlobalState();
            }
        });
    }

    private void updateGlobalState() {
        boolean isEnabled = mPrefsValues.isServiceEnabled();
        mGlobalToggle.setActive(isEnabled);

        mGlobalStatus.setTextLastTs(mPrefsValues.getStatusLastTS());
        if (isEnabled) {
            mGlobalStatus.setTextNextTs(mPrefsValues.getStatusNextTS());
            mGlobalStatus.setTextNextDesc(mPrefsValues.getStatusNextAction());
        } else {
            mGlobalStatus.setTextNextTs(mActivity.getString(R.string.globalstatus_disabled));
            mGlobalStatus.setTextNextDesc(mActivity.getString(R.string.help_to_enable));
        }
        mGlobalStatus.invalidate();
    }

    public void onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.string.menu_append_profile,
                 0, R.string.menu_append_profile).setIcon(R.drawable.ic_menu_add);
        menu.add(0, R.string.menu_settings,
                 0, R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, R.string.menu_about,
                 0, R.string.menu_about).setIcon(R.drawable.ic_menu_help);
        menu.add(0, R.string.menu_report_error,
                 0, R.string.menu_report_error).setIcon(R.drawable.ic_menu_report);
        menu.add(0, R.string.menu_check_now,
                 0, R.string.menu_check_now).setIcon(R.drawable.ic_menu_rotate);
        menu.add(0, R.string.menu_reset,
                 0, R.string.menu_reset).setIcon(R.drawable.ic_menu_revert);
        // TODO save to sd
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.string.menu_settings:
            mAgentWrapper.event(AgentWrapper.Event.MenuSettings);
            showPrefs();
            break;
        case R.string.menu_check_now:
            requestSettingsCheck(UpdateReceiver.TOAST_ALWAYS);
            break;
        case R.string.menu_about:
            mAgentWrapper.event(AgentWrapper.Event.MenuAbout);
            showIntro(true /*force*/, false /* checkService */);
            break;
        case R.string.menu_reset:
            mAgentWrapper.event(AgentWrapper.Event.MenuReset);
            showResetChoices();
            break;
        // TODO save to sd
        case R.string.menu_append_profile:
            appendNewProfile();
            break;
        case R.string.menu_report_error:
            showErrorReport();
            break;
        default:
            return false;
        }
        return true; // handled
    }

    private void showPrefs() {
        mActivity.startActivityForResult(new Intent(mActivity, PrefsUI.class), SETTINGS_UPDATED);
    }

    private void showErrorReport() {
        mActivity.startActivity(new Intent(mActivity, ErrorReporterUI.class));
    }

    /**
     * Requests a setting check.
     *
     * @param displayToast Must be one of {@link UpdateReceiver#TOAST_ALWAYS},
     *                     {@link UpdateReceiver#TOAST_IF_CHANGED} or {@link UpdateReceiver#TOAST_NONE}
     */
    private void requestSettingsCheck(int displayToast) {
        if (DEBUG) Log.d(TAG, "Request settings check");
        Intent i = new Intent(UpdateReceiver.ACTION_UI_CHECK);
        i.putExtra(UpdateReceiver.EXTRA_TOAST_NEXT_EVENT, displayToast);
        mActivity.sendBroadcast(i);
    }

    protected void showResetChoices() {
        mActivity.showDialog(DIALOG_RESET_CHOICES);
    }

    // TODO save to sd

    private Dialog createDialogResetChoices() {
        Builder d = new AlertDialog.Builder(mActivity);

        d.setCancelable(true);
        d.setTitle(R.string.resetprofiles_msg_confirm_delete);
        d.setIcon(R.drawable.app_icon);
        d.setItems(mProfilesDb.getResetLabels(),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mProfilesDb.resetProfiles(which);
                    mActivity.removeDialog(DIALOG_RESET_CHOICES);
                    onDataChanged(true /*backup*/);
                    requestSettingsCheck(UpdateReceiver.TOAST_IF_CHANGED);
                }
        });

        d.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mActivity.removeDialog(DIALOG_RESET_CHOICES);
            }
        });

        d.setNegativeButton(R.string.resetprofiles_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.removeDialog(DIALOG_RESET_CHOICES);
            }
        });

        return d.create();
    }


    //--------------

    public void showTempDialog(long row_id, String title, int dlg_id) {
        mTempDialogRowId = row_id;
        mTempDialogTitle = title;
        mActivity.showDialog(dlg_id);
    }

    //--------------

    private Dialog createDeleteProfileDialog() {
        final long row_id = mTempDialogRowId;
        final String title = mTempDialogTitle;

        Builder d = new AlertDialog.Builder(mActivity);

        d.setCancelable(true);
        d.setTitle(R.string.deleteprofile_title);
        d.setIcon(R.drawable.app_icon);
        d.setMessage(String.format(
                mActivity.getString(R.string.deleteprofile_msgbody), title));

        d.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mActivity.removeDialog(DIALOG_DELETE_PROFILE);
            }
        });

        d.setNegativeButton(R.string.deleteprofile_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.removeDialog(DIALOG_DELETE_PROFILE);
            }
        });

        d.setPositiveButton(R.string.deleteprofile_button_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int count = mProfilesDb.deleteProfile(row_id);
                if (count > 0) {
                    mAdapter.notifyDataSetChanged();
                    onDataChanged(true /*backup*/);
                }
                mActivity.removeDialog(DIALOG_DELETE_PROFILE);
            }
        });

        return d.create();
    }

    private Dialog createDialogDeleteTimedAction() {

        final long row_id = mTempDialogRowId;
        final String description = mTempDialogTitle;

        Builder d = new AlertDialog.Builder(mActivity);

        d.setCancelable(true);
        d.setTitle(R.string.deleteaction_title);
        d.setIcon(R.drawable.app_icon);
        d.setMessage(mActivity.getString(R.string.deleteaction_msgbody, description));

        d.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mActivity.removeDialog(DIALOG_DELETE_ACTION);
            }
        });

        d.setNegativeButton(R.string.deleteaction_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.removeDialog(DIALOG_DELETE_ACTION);
            }
        });

        d.setPositiveButton(R.string.deleteaction_button_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int count = mProfilesDb.deleteAction(row_id);
                if (count > 0) {
                    mAdapter.notifyDataSetChanged();
                    onDataChanged(true /*backup*/);
                }
                mActivity.removeDialog(DIALOG_DELETE_ACTION);
            }
        });

        return d.create();
    }


    public void appendNewProfile() {
        long prof_index = mProfilesDb.insertProfile(0,
                        mActivity.getString(R.string.insertprofile_new_profile_title),
                        true /*isEnabled*/);

        Intent intent = new Intent(mActivity, EditProfileUI.class);
        intent.putExtra(EditProfileUI.EXTRA_PROFILE_ID, prof_index << Columns.PROFILE_SHIFT);

        mActivity.startActivityForResult(intent, DATA_CHANGED);
    }

}
