/*
 * (c) ralfoide gmail com, 2008
 * Project: Timeriffic
 * License TBD
 */

package com.alfray.timeriffic.storage;

//-----------------------------------------------

public abstract class Line {

    private String mData;
    private boolean mIsDirty;

    /**
     * Create a new Line object.
     * Sets the line to non-dirty.
     *
     * @param data If non-null, will be passed to {@link #decode(String)}
     *   to deserialize the objet.
     */
    public Line(String data) {
        mData = data;
        if (data != null) decode(data);
        mIsDirty = false;
    }

    /**
     * Sets the line as dirty, meaning the next call to {@link #getData()}
     * will call {@link #encode()} to serialize the object.
     */
    public void markDirty() {
        mIsDirty = true;
    }

    /**
     * Retrieves the serialized data of the line.
     * The data is cached and only re-encoded using {@link #encode()} either
     * if there was no previous cached data or if the object has been
     * marked as dirty by {@link #markDirty()}.
     * <p/>
     * Resets the dirty flag and caches the new data.
     */
    public String getData() {
        if (mData == null || mIsDirty) {
            mData = encode();
            mIsDirty = false;
        }
        return mData;
    }

    /**
     * Invoked by {@link Line#Line(String)} to decode the data
     * and populate the fields.
     */
    protected abstract void decode(String data);

    /**
     * Invoked by {@link Line#getData()} to transform the fields into the
     * saved data string.
     */
    protected abstract String encode();
}


