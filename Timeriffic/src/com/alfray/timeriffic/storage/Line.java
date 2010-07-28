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

    public Line(String data) {
        decode(data);
        mIsDirty = false;
    }

    public void markDirty() {
        mIsDirty = true;
    }

    public String getData() {
        if (mIsDirty) {
            mData = encode();
            mIsDirty = false;
        }
        return mData;
    }

    protected abstract void decode(String data);
    protected abstract String encode();
}


