/*
 * Project: Timeriffic
 * Copyright (C) 2008 ralfoide gmail com,
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

package com.alfray.timeriffic.profiles;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.error.ExceptionHandlerActivity;

public class EditProfileUI extends ExceptionHandlerActivity {

    public static String TAG = "TFC-EditProfUI";

    /** Extra long with the profile id (not index) to edit. */
    public static final String EXTRA_PROFILE_ID = "prof_id";
    private EditText mNameField;
    private CheckBox mEnabledCheck;
    private long mProfId;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_profile);
        setTitle(R.string.editprofile_title);

        Intent intent = getIntent();
        mProfId = intent.getExtras().getLong(EXTRA_PROFILE_ID);

        Log.d(TAG, String.format("edit prof_id: %08x", mProfId));

        if (mProfId == 0) {
            Log.e(TAG, "profile id not found in intent.");
            finish();
            return;
        }


        // get profiles db helper
        ProfilesDB profilesDb = new ProfilesDB();
        profilesDb.onCreate(this);

        // get cursor
        String prof_id_select = String.format("%s=%d", Columns.PROFILE_ID, mProfId);
        Cursor c = profilesDb.query(
                -1, // id
                // projection, a.k.a. the list of columns to retrieve from the db
                new String[] {
                        Columns.PROFILE_ID,
                        Columns.DESCRIPTION,
                        Columns.IS_ENABLED
                },
                prof_id_select, // selection
                null, // selectionArgs
                null // sortOrder
                );
        try {
            if (!c.moveToFirst()) {
                Log.e(TAG, "cursor is empty: " + prof_id_select);
                finish();
                return;
            }

            // get UI widgets
            mNameField = (EditText) findViewById(R.id.name);
            mEnabledCheck = (CheckBox) findViewById(R.id.enabled);

            // get column indexes
            int descColIndex = c.getColumnIndexOrThrow(Columns.DESCRIPTION);
            int enColIndex = c.getColumnIndexOrThrow(Columns.IS_ENABLED);

            // fill in UI from cursor data
            mNameField.setText(c.getString(descColIndex));
            mEnabledCheck.setChecked(c.getInt(enColIndex) != 0);
        } finally {
            c.close();
            profilesDb.onDestroy();
        }

        Button accept = (Button) findViewById(R.id.ok);
        accept.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                accept();
            }
        });
    }

    private void accept() {
        ProfilesDB profilesDb = new ProfilesDB();
        try {
            profilesDb.onCreate(this);
            profilesDb.updateProfile(
                    mProfId,
                    mNameField.getText().toString(),
                    mEnabledCheck.isChecked());
        } finally {
            profilesDb.onDestroy();
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // do nothing, discard changes
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
