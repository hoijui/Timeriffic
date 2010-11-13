/*
 * Project: Timeriffic
 * Copyright (C) 2010 ralfoide gmail com,
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

package com.alfray.timeriffic.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;

//-----------------------------------------------

/* package */ class Storage {

    private static final String FILENAME = "timeriffic_profiles.txt";
    private static final String HEADER = "TFC.1";

    private String mLastError = null;
    private final List<Profile> mProfiles = new ArrayList<Profile>();

    /**
     * Creates storage instance. Empty till {@link #load(Context)} is invoked.
     */
    public Storage() {
    }

    public List<Profile> getProfiles() {
        return mProfiles;
    }

    /**
     * Tries to load profiles from a previous save.
     * <p/>
     * First this tries to load from the internal app-specific storage.
     * If this fails, we try to load a file from the sdcard.
     * <p/>
     * If the files fail to load, keep an error to display it to the user
     * but don't load invalid data.
     */
    public boolean load(Context context) {
        FileInputStream fis = null;
        try {
            // Try loading internal file
            fis = context.openFileInput(FILENAME);
            if (loadStream(fis)) {
                return true;
            }
        } catch (Exception e) {
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
            }
        }

        return tryLoadingFromSD(context);
    }

    public boolean save(Context context) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_WORLD_READABLE);
            return saveStream(fos);
        } catch (Exception e) {
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
            }
        }

        return false;
    }

    public boolean saveToSD(Context context) {
        FileOutputStream fos = null;
        try {
            File dir = Environment.getExternalStorageDirectory();
            if (dir.isDirectory()) {
                File sdfile = new File(dir, FILENAME);
                if (sdfile.isFile()) {
                    fos = new FileOutputStream(sdfile);
                    return saveStream(fos);
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
            }
        }

        return false;
    }

    private boolean tryLoadingFromSD(Context context) {
        // Check if we can find an external file at the root of the SDCard
        // (which is where most users expect it.)
        try {
            File dir = Environment.getExternalStorageDirectory();
            if (dir.isDirectory()) {
                File sdfile = new File(dir, FILENAME);
                if (sdfile.isFile()) {
                    FileInputStream fis = new FileInputStream(sdfile);
                    if (loadStream(fis)) {
                        // If loading from SD worked, save internally
                        return save(context);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return false;
    }

    private boolean loadStream(InputStream is) {
        try {
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr, 4096);

            // get header
            String line = br.readLine();
            if (!HEADER.equals(line)) {
                mLastError = "Invalid file format, header missing.";
                return false;
            }

            mProfiles.clear();
            Profile p = null;

            while((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() <= 1) continue;
                char c = line.charAt(0);
                line = line.substring(1);

                if (c == 'A' && p != null) {
                    p.addAction(line.substring(1));
                } else if (c == 'P') {
                    p = new Profile(line.substring(1));
                    mProfiles.add(p);
                } else if (c == 'F') {
                    // TODO compute final CRC
                    mLastError = null;
                    return true;
                }
            }

            mLastError = "Invalid file format, checksum missing.";
            return true;

        } catch(Exception e) {
            mLastError = e.getMessage();
            if (mLastError == null) mLastError = e.toString();
        }

        return false;
    }

    private boolean saveStream(OutputStream os) {
        BufferedWriter bw = null;
        try {
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            bw = new BufferedWriter(osw, 4096);

            bw.write(HEADER);
            bw.newLine();

            for (Profile p : mProfiles) {
                bw.write(p.getData());
                bw.newLine();

                for (Action a : p.getActions()) {
                    bw.write(a.getData());
                    bw.newLine();
                }
            }

            bw.write("F0");
            bw.newLine();
            return true;

        } catch (Exception e) {
            mLastError = e.getMessage();
            if (mLastError == null) mLastError = e.toString();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
        }

        return false;
    }


}


