/*
 * Project: Timeriffic
 * Copyright (C) 2009 ralfoide gmail com,
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

package com.rdrrlabs.timeriffic.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.alfray.timeriffic.R;
import com.rdrrlabs.timeriffic.app.TimerifficApp;
import com.rdrrlabs.timeriffic.app.UpdateReceiver;
import com.rdrrlabs.timeriffic.app.UpdateService;
import com.rdrrlabs.timeriffic.core.app.AppId;
import com.rdrrlabs.timeriffic.core.app.ApplySettings;
import com.rdrrlabs.timeriffic.core.app.BackupWrapper;
import com.rdrrlabs.timeriffic.core.app.CoreStrings;
import com.rdrrlabs.timeriffic.core.app.CoreStrings.Strings;
import com.rdrrlabs.timeriffic.core.error.ExceptionHandler;
import com.rdrrlabs.timeriffic.core.prefs.PrefsStorage;
import com.rdrrlabs.timeriffic.core.prefs.PrefsValues;
import com.rdrrlabs.timeriffic.core.profiles1.BaseHolder;
import com.rdrrlabs.timeriffic.core.profiles1.ProfileHeaderHolder;
import com.rdrrlabs.timeriffic.core.profiles1.ProfilesDB;
import com.rdrrlabs.timeriffic.core.profiles1.ProfilesUiImpl;
import com.rdrrlabs.timeriffic.core.profiles1.TimedActionHolder;
import com.rdrrlabs.timeriffic.core.serial.SerialReader;
import com.rdrrlabs.timeriffic.core.settings.AirplaneSetting;
import com.rdrrlabs.timeriffic.core.settings.ApnDroidSetting;
import com.rdrrlabs.timeriffic.core.settings.BluetoothSetting;
import com.rdrrlabs.timeriffic.core.settings.BrightnessSetting;
import com.rdrrlabs.timeriffic.core.settings.DataSetting;
import com.rdrrlabs.timeriffic.core.settings.RingerSetting;
import com.rdrrlabs.timeriffic.core.settings.SettingFactory;
import com.rdrrlabs.timeriffic.core.settings.SyncSetting;
import com.rdrrlabs.timeriffic.core.settings.VibrateSetting;
import com.rdrrlabs.timeriffic.core.settings.VolumeSetting;
import com.rdrrlabs.timeriffic.core.settings.WifiSetting;
import com.rdrrlabs.timeriffic.core.utils.AgentWrapper;
import com.rdrrlabs.timeriffic.core.utils.ApplyVolumeReceiver;
import com.rdrrlabs.timeriffic.core.utils.SettingsHelper;
import com.rdrrlabs.timeriffic.core.utils.VolumeChange;

/**
 * Screen to generate an error report.
 */
public class ErrorReporterUI extends ExceptionHandlerUI {

    private static final boolean DEBUG = true;
    public static final String TAG = ErrorReporterUI.class.getSimpleName();

    /** Boolean extra: True if this is generated from an exception, false
     * if generated from a user request. */
    public static final String EXTRA_IS_EXCEPTION =
        ErrorReporterUI.class.getPackage().getName() + "_isException";

    // TODO more UTs

    private static final int MSG_REPORT_COMPLETE = 1;

    private AgentWrapper mAgentWrapper;
    private Handler mHandler;
    private boolean mAbortReport;
    private String mAppName;
    private String mAppVersion;
    private boolean mIsException;
    private Button mButtonGen;
    private Button mButtonPrev;
    private Button mButtonNext;
    private View mUserFrame;
    private RadioGroup mRadioGroup;
    private WebView mWebView;
    private EditText mUserText;

    private class JSErrorInfo {

        private final int mNumExceptions;
        private final int mNumActions;

        public JSErrorInfo(int numExceptions, int numActions) {
            mNumExceptions = numExceptions;
            mNumActions = numActions;
        }

        @SuppressWarnings("unused")
        public int getNumExceptions() {
            return mNumExceptions;
        }

        @SuppressWarnings("unused")
        public int getNumActions() {
            return mNumActions;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.error_report);

        mAppName = getString(R.string.app_name);
        setTitle(getString(R.string.errorreport_title).replaceAll("\\$APP", mAppName));

        Intent i = getIntent();
        mIsException = i == null ? false : i.getBooleanExtra(EXTRA_IS_EXCEPTION, false);

        mButtonGen = (Button) findViewById(R.id.generate);
        mButtonPrev = (Button) findViewById(R.id.prev);
        mButtonNext = (Button) findViewById(R.id.next);
        mUserFrame = findViewById(R.id.user_frame);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
        mWebView = (WebView) findViewById(R.id.web);
        mUserText = (EditText) findViewById(R.id.user_text);

        adjustUserHint(mUserText);

        PackageManager pm = getPackageManager();
        if (pm != null) {
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo(getPackageName(), 0);
                mAppVersion = pi.versionName;
                if (mAppVersion == null) {
                    mAppVersion = "";
                } else {
                    // Remove anything after the first space
                    int pos = mAppVersion.indexOf(' ');
                    if (pos > 0 && pos < mAppVersion.length() - 1) {
                        mAppVersion = mAppVersion.substring(0, pos);
                    }
                }
            } catch (Exception ignore) {
                // getPackageName typically throws NameNotFoundException
            }
        }

        if (mWebView == null) {
            if (DEBUG) Log.e(TAG, "Missing web view");
            finish();
        }

        // Make the webview transparent (for background gradient)
        mWebView.setBackgroundColor(0x00000000);

        String file = selectFile("error_report");
        file = file.replaceAll("\\$APP", mAppName);
        loadFile(mWebView, file);
        setupJavaScript(mWebView);
        setupListeners();
        setupHandler();

        int page = mIsException ? 2 : 1;
        selectPage(page);
        updateButtons();
    }

    /**
     * Get the text hint from the text field. If we current translation has a hint2 text
     * (which typically says "Please write comment in English" and is going to be empty
     * in English), then we append that text to the hint.
     */
    private void adjustUserHint(EditText userText) {
        String str2 = getString(R.string.errorreport_user_hint_english);
        if (str2 == null) return;
        str2 = str2.trim();
        if (str2.length() == 0) return;

        String str1 = userText.getHint().toString();
        str1 = str1.trim();
        if (str1.length() > 0) str1 += " ";
        userText.setHint(str1 + str2);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAgentWrapper = new AgentWrapper();
        mAgentWrapper.start(this);
        mAgentWrapper.event(AgentWrapper.Event.OpenIntroUI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // If the generator thread is still running, just set the abort
        // flag and let the thread terminate itself.
        mAbortReport = true;

        mAgentWrapper.stop(this);
    }

    private String selectFile(String baseName) {
        String file;

        // Compute which file we want to display, i.e. try to select
        // one that matches baseName-LocaleCountryName.html or default
        // to intro.html
        Locale lo = Locale.getDefault();
        String lang = lo.getLanguage();
        String country = lo.getCountry();
        if (lang != null && lang.length() > 2) {
            // There's a bug in the SDK "Locale Setup" app in Android 1.5/1.6
            // where it sets the full locale such as "en_US" in the languageCode
            // field of the Locale instead of splitting it correctly. So we do it
            // here.
            int pos = lang.indexOf('_');
            if (pos > 0 && pos < lang.length() - 1) {
                country = lang.substring(pos + 1);
                lang = lang.substring(0, pos);
            }
        }
        if (lang != null && lang.length() == 2) {
            AssetManager am = getResources().getAssets();

            // Try with both language and country, e.g. -en-US, -zh-CN
            if (country != null && country.length() == 2) {
                file = baseName + "-" + lang.toLowerCase() + "-" + country.toUpperCase() + ".html";
                if (checkFileExists(am, file)) {
                    return file;
                }
            }

            // Try to fall back on just language, e.g. -zh, -fr
            file = baseName + "-" + lang.toLowerCase() + ".html";
            if (checkFileExists(am, file)) {
                return file;
            }
        }

        if (!"en".equals(lang)) {
            if (DEBUG) Log.d(TAG, "Language not found: " + lang + "+" + country);
        }

        // This one just has to exist or we'll crash n' burn on the 101.
        return baseName + ".html";
    }

    private boolean checkFileExists(AssetManager am, String filename) {
        InputStream is = null;
        try {
            is = am.open(filename);
            return is != null;
        } catch (IOException e) {
            // pass
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // pass
                }
            }
        }
        return false;
    }

    private void loadFile(final WebView wv, String file) {
        wv.loadUrl("file:///android_asset/" + file);
        wv.setFocusable(true);
        wv.setFocusableInTouchMode(true);
        wv.requestFocus();
    }

    private void setupJavaScript(final WebView wv) {

        // TODO get numbers
        int num_ex = ExceptionHandler.getNumExceptionsInLog(this);
        int num_act = ApplySettings.getNumActionsInLog(this);

        // Inject a JS method to set the version
        JSErrorInfo js = new JSErrorInfo(num_ex, num_act);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(js, "JSErrorInfo");
    }

    private void setupListeners() {
        if (mButtonGen != null) {
            mButtonGen.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {

                    // Start inderterminate progress bar
                    ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
                    if (progress != null) {
                        progress.setVisibility(View.VISIBLE);
                        progress.setIndeterminate(true);
                    }

                    // Gray generate button (to avoid user repeasting it)
                    mButtonGen.setEnabled(false);

                    mAbortReport = false;

                    try {
                        Thread t = new Thread(new ReportGenerator(), "ReportGenerator");
                        t.start();
                    } catch (Throwable t) {
                        // We can possibly get a VerifyError from Dalvik here
                        // if the thread can't link (e.g. because it's using
                        // an unsupported API.). Normally we wouldn't care but
                        // we don't want the error reporter to crash itself.
                        Toast.makeText(ErrorReporterUI.this,
                                "Failed to generate report: " + t.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        if (mButtonPrev != null) {
            mButtonPrev.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!mIsException) selectPage(1);
                }
            });
        }

        if (mButtonNext != null) {
            mButtonNext.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    selectPage(2);
                }
            });
        }

        if (mRadioGroup != null) {
            mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    updateButtons();
                }
            });
        }

        if (mUserText != null) {
            mUserText.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // pass
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // pass
                }

                public void afterTextChanged(Editable s) {
                    updateButtons();
                }
            });
        }
    }

    private void setupHandler() {
        mHandler = new Handler(new Callback() {

            public boolean handleMessage(Message msg) {

                if (msg.what == MSG_REPORT_COMPLETE) {

                    try {
                        // Get the report associated with the message
                        String report = (String) msg.obj;

                        // Stop inderterminate progress bar
                        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
                        if (progress != null) {
                            progress.setIndeterminate(false);
                            progress.setVisibility(View.GONE);
                        }

                        if (report != null) {
                            TimerifficApp ta = TimerifficApp.getInstance(getApplicationContext());
                            CoreStrings str = ta.getCore().getCoreStrings();

                            // Prepare mailto and subject.
                            String to = str.format(Strings.ERR_UI_MAILTO, mAppName).trim();
                            to += "@";
                            to += str.get(Strings.ERR_UI_DOMTO).replace("/", ".");
                            to = to.replaceAll("[ _]", "").toLowerCase();

                            StringBuilder sb = new StringBuilder();
                            sb.append('[').append(mAppName.trim()).append("] ");
                            sb.append(getReportType().trim());

                            sb.append(" [").append(ta.getIssueId());
                            PrefsStorage ps = ta.getPrefsStorage();
                            if (ps.endReadAsync()) {
                                sb.append('-').append(ps.getInt("iss_id_count", -1));
                            }
                            sb.append(']');

                            String subject = sb.toString();

                            // Generate the intent to send an email
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
                            i.putExtra(Intent.EXTRA_SUBJECT, subject);
                            i.putExtra(Intent.EXTRA_TEXT, report);
                            i.setType("message/rfc822");

                            try {
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                // This is likely to happen if there's no mail app.
                                Toast.makeText(getApplicationContext(),
                                        R.string.errorreport_nomailapp,
                                        Toast.LENGTH_LONG).show();
                                Log.d(TAG, "No email/gmail app found", e);
                            } catch (Exception e) {
                                // This is unlikely to happen.
                                Toast.makeText(getApplicationContext(),
                                        "Send email activity failed: " + e.toString(),
                                        Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Send email activity failed", e);
                            }

                            // Finish this activity.
                            finish();
                        }
                    } finally {
                        // We're not supposed to get there since there's a finish
                        // above. So maybe something failed and we should let the
                        // user retry, so in any case, ungray generate button.

                        if (mButtonGen != null) {
                            mButtonGen.setEnabled(true);
                        }
                    }
                }
                return false;
            }
        });
    }

    private void updateButtons() {
        if (mButtonNext != null && mUserText != null && mRadioGroup != null) {
            mButtonNext.setEnabled(
                    mRadioGroup.getCheckedRadioButtonId() != -1 &&
                    mUserText.getText().length() > 0);
        }
    }

    private void selectPage(int i) {
        if (i < 0 || i > 2) return;

        // Page 1:
        // - scrollview "user_frame"
        // - button "next" (enabled if radio choice + text not empty)
        // - hide prev, gen, web
        //
        // Page 2:
        // - show gen, web
        // - hide prev if mIsException, otherwise show
        // - hide user_frame, next

        int visPage1 = i == 1 ? View.VISIBLE : View.GONE;
        int visPage2 = i == 2 ? View.VISIBLE : View.GONE;

        mButtonPrev.setVisibility(mIsException ? View.GONE : visPage2);
        mButtonNext.setVisibility(visPage1);
        mButtonGen.setVisibility(visPage2);
        mUserFrame.setVisibility(visPage1);
        mWebView.setVisibility(visPage2);
    }


    /** Returns a non-translated string for report type. */
    private String getReportType() {

        if (mIsException) {
            return "Exception Report (Force Close)";
        }

        int id = mRadioGroup == null ? -1 : mRadioGroup.getCheckedRadioButtonId();

        if (id == R.id.radio_err) {
            return "User Error Report";

        } else if (id == R.id.radio_fr) {
            return "User Feature Request";

        }

        return "Unknown Report Type";
    }

    /**
     * Generates the error report, with the following sections:
     * - Request user to enter some information (translated, rest is not)
     * - Device information (nothing user specific)
     * - Profile list summary
     * - Recent Exceptions
     * - Recent actions
     * - Recent logcat
     */
    private class ReportGenerator implements Runnable {
        public void run() {

            Context c = ErrorReporterUI.this.getApplicationContext();
            PrefsValues pv = new PrefsValues(c);

            StringBuilder sb = new StringBuilder();

            addHeader(sb, c);
            addUserFeedback(sb);
            addTopInfo(sb);
            addIssueId(sb);
            addAndroidBuildInfo(sb);

            if (!mAbortReport) addProfiles(sb, c);
            if (!mAbortReport) addLastExceptions(sb, pv);
            if (!mAbortReport) addLastActions(sb, pv);
            if (!mAbortReport) addLogcat(sb);

            // -- Report complete

            // We're done with the report. Ping back the activity using
            // a message to get back to the UI thread.
            if (!mAbortReport) {
                Message msg = mHandler.obtainMessage(MSG_REPORT_COMPLETE, sb.toString());
                mHandler.sendMessage(msg);
            }
        }

        private void addHeader(StringBuilder sb, Context c) {
            try {
                String s = c.getString(R.string.errorreport_emailheader);
                s = s.replaceAll(Pattern.quote("$APP"), mAppName);
                sb.append(s.trim().replace('/', '\n')).append("\n");
            } catch (Exception ignore) {
            }
        }

        private void addIssueId(StringBuilder sb) {
            Application app = getApplication();
            if (app instanceof TimerifficApp) {
                TimerifficApp ta = (TimerifficApp) app;
                sb.append("\n## Issue Id: ").append(ta.getIssueId());

                PrefsStorage ps = ta.getPrefsStorage();
                if (ps.endReadAsync()) {
                    int count = ps.getInt("iss_id_count", 0);
                    count += 1;
                    sb.append(" - ").append(count);
                    ps.putInt("iss_id_count", count);
                    ps.flushSync(getApplicationContext());
                }
            }
        }

        private void addUserFeedback(StringBuilder sb) {
            sb.append("\n## Report Type: ").append(getReportType());

            if (!mIsException) {
                sb.append("\n\n## User Comments:\n");
                if (mUserText != null) sb.append(mUserText.getText());
                sb.append("\n");
            }
        }


        private void addTopInfo(StringBuilder sb) {
            sb.append(String.format("\n## App: %s %s\n",
                    mAppName,
                    mAppVersion));
            sb.append(String.format("## Locale: %s\n",
                    Locale.getDefault().toString()));

            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
            String date = df.format(new Date(System.currentTimeMillis()));

            sb.append(String.format("## Log Date: %s\n", date));
        }

        private void addAndroidBuildInfo(StringBuilder sb) {

            sb.append("\n## Android Device ##\n\n");

            try {
                // Build.MANUFACTURER is only available starting at API 4
                String manufacturer = "--";
                try {
                    Field f = Build.class.getField("MANUFACTURER");
                    manufacturer = (String) f.get(null /*static*/);
                } catch (Exception ignore) {
                }

                sb.append(String.format("Device : %s (%s %s)\n",
                        Build.MODEL,
                        manufacturer,
                        Build.DEVICE));

                sb.append(String.format("Android: %s (API %s)\n",
                        Build.VERSION.RELEASE, Build.VERSION.SDK));

                sb.append(String.format("Build  : %s\n",
                        Build.FINGERPRINT));
            } catch (Exception ignore) {
            }
        }

        private void addLastExceptions(StringBuilder sb, PrefsValues prefs) {
            sb.append("\n## Recent Exceptions ##\n\n");

            String s = prefs.getLastExceptions();
            if (s == null) {
                sb.append("None\n");
            } else {
                sb.append(s);
            }
        }

        private void addLastActions(StringBuilder sb, PrefsValues prefs) {
            sb.append("\n## Recent Actions ##\n\n");

            String s = prefs.getLastActions();
            if (s == null) {
                sb.append("None\n");
            } else {
                sb.append(s);
            }
        }

        private void addProfiles(StringBuilder sb, Context c) {
            sb.append("\n## Profiles Summary ##\n\n");

            try {
                ProfilesDB profilesDb = new ProfilesDB();
                try {
                    profilesDb.onCreate(c);

                    String[] p = profilesDb.getProfilesDump();
                    for (String s : p) {
                        sb.append(s);
                    }

                } finally {
                    profilesDb.onDestroy();
                }
            } catch (Exception ignore) {
                // ignore
            }
        }

        private void addLogcat(StringBuilder sb) {
            sb.append(String.format("\n## %s Logcat ##\n\n", mAppName));

            BackupWrapper backupWrapper = new BackupWrapper(getApplicationContext());
            String sTimerifficBackupAgent_TAG = backupWrapper.getTAG_TimerifficBackupAgent();
            if (sTimerifficBackupAgent_TAG == null) {
                sTimerifficBackupAgent_TAG = "";
            } else {
                sTimerifficBackupAgent_TAG += ":D";
            }

            String[] cmd = new String[] {
                    "logcat",
                    "-d",       // dump log and exits

                    // actions package

                    // app package
                    ApplySettings.TAG + ":D",
                    UpdateReceiver.TAG + ":D",
                    UpdateService.TAG + ":D",

                    AppId.TAG + ":D",
                    BackupWrapper.TAG + ":D",
                    sTimerifficBackupAgent_TAG,

                    // error package
                    ExceptionHandler.TAG + ":D",

                    // prefs package

                    // profiles package
                    BaseHolder.TAG + ":D",
                    ProfileHeaderHolder.TAG + ":D",
                    ProfilesDB.TAG + ":D",
                    TimedActionHolder.TAG + ":D",
                    ProfilesUiImpl.TAG + ":D",

                    // serial package
                    SerialReader.TAG + ":D",

                    // settings package
                    AirplaneSetting.TAG + ":D",
                    ApnDroidSetting.TAG + ":D",
                    BluetoothSetting.TAG + ":D",
                    BrightnessSetting.TAG + ":D",
                    DataSetting.TAG + ":D",
                    RingerSetting.TAG + ":D",
                    SettingFactory.TAG + ":D",
                    SyncSetting.TAG + ":D",
                    VibrateSetting.TAG + ":D",
                    VolumeSetting.TAG + ":D",
                    WifiSetting.TAG + ":D",

                    // utils package
                    AgentWrapper.TAG + ":D",
                    SettingsHelper.TAG + ":D",
                    VolumeChange.TAG + ":D",
                    ApplyVolumeReceiver.TAG + ":D",

                    // ui package
                    ChangeBrightnessUI.TAG + ":D",
                    EditActionUI.TAG + ":D",
                    EditProfileUI.TAG + ":D",
                    ErrorReporterUI.TAG + ":D",
                    IntroUI.TAG + ":D",

                    "FlurryAgent:W",

                    //-- too verbose --"*:I",      // all other tags in info mode or better
                    "*:S",      // silence all tags we don't want
            };

            try {
                Process p = Runtime.getRuntime().exec(cmd);

                InputStreamReader isr = null;
                BufferedReader br = null;

                try {
                    InputStream is = p.getInputStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);

                    String line = null;

                    // Make sure this doesn't take forever.
                    // We cut after 30 seconds which is already very long.
                    long maxMs = System.currentTimeMillis() + 30*1000;
                    int count = 0;

                    while (!mAbortReport && (line = br.readLine()) != null) {
                        sb.append(line).append("\n");

                        // check time limit once in a while
                        count++;
                        if (count > 50) {
                            if (System.currentTimeMillis() > maxMs) {
                                // This may or may not work.
                                // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4485742
                                p.destroy();
                                break;
                            }
                            count = 0;
                        }
                    }

                } finally {
                    if (br != null) {
                        br.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                }

            } catch (IOException e) {
                Log.d(TAG, "Logcat exec failed", e);
            } catch (Throwable ignore) {
            }
        }

    }
}
