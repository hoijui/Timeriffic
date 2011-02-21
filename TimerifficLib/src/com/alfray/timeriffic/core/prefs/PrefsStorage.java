/**
 *
 */
package com.alfray.timeriffic.core.prefs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.alfray.timeriffic.core.serial.SerialKey;
import com.alfray.timeriffic.core.serial.SerialReader;
import com.alfray.timeriffic.core.serial.SerialWriter;

/**
 * Wrapper around {@link SerialWriter} and {@link SerialReader} to deal with app prefs.
 * <p/>
 * Supported types are the minimal required for hour needs: boolean, string and int.
 * Callers need to ensure that only one instance exists for the same file.
 * <p/>
 * Caller initial cycle should be:
 * - begingReadAsync
 * - endReadAsync ... this waits for the read the finish.
 * - read, add or modify data.
 * - modifying data generates a delayed write (or delays an existing one)
 * - flushSync must be called by the owner at least once, typically when an activity/app
 *   is paused or about to finish. It forces a write or wait for an existing one to finish.
 * <p/>
 * Values cannot be null.
 * Affecting a value to null is equivalent to removing it from the storage map.
 *
 */
public class PrefsStorage {

    public static class TypeMismatchException extends RuntimeException {
        private static final long serialVersionUID = -6386235026748640081L;
        public TypeMismatchException(String key, String expected, Object actual) {
            super(String.format("Key '%1$s' excepted type %2$s, got %3$s",
                    key, expected, actual.getClass().getSimpleName()));
        }
    }

    private static final String FOOTER = "F0";
    private static final String TAG = PrefsStorage.class.getSimpleName();
    private static final String HEADER = "SPREFS.1";

    private final SerialKey mKeyer = new SerialKey();
    private final SparseArray<Object> mData = new SparseArray<Object>();
    private final String mFilename;
    private boolean mDataChanged;
    private volatile Thread mLoadThread;
    private boolean mLoadResult;

    /**
     * Opens a serial prefs for "filename.sprefs" in the app's dir.
     * Caller must still read the file before anything happens.
     *
     * @param filename Filename. Must not be null or empty.
     */
    public PrefsStorage(String filename) {
        mFilename = filename;
    }

    /**
     * Starts reading an existing prefs file asynchronously.
     * Callers <em>must</em> call {@link #endReadAsync()}.
     *
     * @param context The {@link Context} to use.
     */
    public void beginReadAsync(Context context) {

        final Context appContext = context.getApplicationContext();

        if (mLoadThread != null) {
            throw new RuntimeException("Load already pending.");
        }

        mLoadThread = new Thread() {
            @Override
            public void run() {
                FileInputStream fis = null;
                try {
                    fis = appContext.openFileInput(mFilename);
                    mLoadResult = loadStream(fis);
                } catch (FileNotFoundException e) {
                    // This is an expected error.
                    Log.d(TAG, "fileNotFound");
                    mLoadResult = true;
                } catch (Exception e) {
                    Log.d(TAG, "endReadAsync failed", e);
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        };
        mLoadThread.start();
    }

    /**
     * Makes sure the asynchronous read has finished.
     * Callers must call this at least once before they access
     * the underlying storage.
     * @return The result from the last load operation.
     */
    public boolean endReadAsync() {
        Thread t = null;
        synchronized(this) {
            t = mLoadThread;
            if (t != null) mLoadThread = null;
        }
        if (t != null) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Log.w(TAG, e);
            }
        }
        return mLoadResult;
    }

    /**
     * Saves the prefs if they have changed.
     * @param context The app context.
     * @return True if prefs could be failed, false otherwise.
     */
    public boolean flushSync(Context context) {
        if (!mDataChanged) return true;
        synchronized(this) {
            if (mDataChanged) {
                mDataChanged = false;
                FileOutputStream fos = null;
                try {
                    fos = context.openFileOutput(mFilename, Context.MODE_PRIVATE);
                    return saveStream(fos);
                } catch (Exception e) {
                    Log.d(TAG, "flushSync failed", e);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        return false;
    }

    // --- put

    public void putInt(String key, int value) {
        mData.put(mKeyer.encodeNewKey(key), Integer.valueOf(value));
        mDataChanged = true;
    }

    public void putBool(String key, boolean value) {
        mData.put(mKeyer.encodeNewKey(key), Boolean.valueOf(value));
        mDataChanged = true;
    }

    public void putString(String key, String value) {
        mData.put(mKeyer.encodeNewKey(key), value);
        mDataChanged = true;
    }

    // --- has

    public boolean hasKey(String key) {
        return mData.indexOfKey(mKeyer.encodeKey(key)) >= 0;
    }

    public boolean hasInt(String key) {
        Object o = mData.get(mKeyer.encodeKey(key));
        return o instanceof Integer;
    }

    public boolean hasBool(String key) {
        Object o = mData.get(mKeyer.encodeKey(key));
        return o instanceof Boolean;
    }

    public boolean hasString(String key) {
        Object o = mData.get(mKeyer.encodeKey(key));
        return o instanceof String;
    }

    // --- get

    public int getInt(String key, int defValue) {
        Object o = mData.get(mKeyer.encodeKey(key));
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else if (o != null) {
            throw new TypeMismatchException(key, "int", o);
        }
        return defValue;
    }

    public boolean getBool(String key, boolean defValue) {
        Object o = mData.get(mKeyer.encodeKey(key));
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        } else if (o != null) {
            throw new TypeMismatchException(key, "boolean", o);
        }
        return defValue;
    }

    public String getString(String key, String defValue) {
        Object o = mData.get(mKeyer.encodeKey(key));
        if (o instanceof String) {
            return (String) o;
        } else if (o != null) {
            throw new TypeMismatchException(key, "String", o);
        }
        return defValue;
    }

    // ----

    private boolean loadStream(InputStream is) {
        try {
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr, 4096);

            // get header
            String line = br.readLine();
            if (!HEADER.equals(line)) {
                Log.d(TAG, "Invalid file format, header missing.");
                return false;
            }

            line = br.readLine();
            SerialReader sr = new SerialReader(line);

            mData.clear();

            for (SerialReader.Entry entry : sr) {
                mData.append(entry.getKey(), entry.getValue());
            }

            line = br.readLine();
            if (!FOOTER.equals(line)) {
                Log.d(TAG, "Invalid file format, footer missing.");
                return false;
            }

            return true;

        } catch(Exception e) {
            Log.d(TAG, "Error reading file.", e);
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

            SerialWriter sw = new SerialWriter();

            for (int n = mData.size(), i = 0; i < n; i++) {
                int key = mData.keyAt(i);
                Object value = mData.valueAt(i);

                // no need to store null values.
                if (value == null) continue;

                if (value instanceof Integer) {
                    sw.addInt(key, ((Integer) value).intValue());
                } else if (value instanceof Boolean) {
                    sw.addBool(key, ((Boolean) value).booleanValue());
                } else if (value instanceof String) {
                    sw.addString(key, (String) value);
                } else {
                    throw new UnsupportedOperationException(
                            this.getClass().getSimpleName() +
                            " does not support type " +
                            value.getClass().getSimpleName());
                }
            }

            bw.write(sw.encodeAsString());
            bw.newLine();

            bw.write(FOOTER);
            bw.newLine();
            return true;

        } catch (Exception e) {
            Log.d(TAG, "Error writing file.", e);
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
