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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.FontMetrics;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.alfray.timeriffic.R;

//-----------------------------------------------

/**
 */
public class GlobalStatus extends View {

    private Bitmap mAccentLeft;
    private Matrix mAccentRightMatrix;

    private Paint mPaintLast;
    private Paint mPaintNext;
    private Paint mPaintTimestamp;
    private Paint mPaintDesc;
    private float mYLast;
    private float mYNext;
    private float mYNextDesc;
    private float mYLastDesc;
    private int mXLastNext;
    private int mXTsDesc;
    private String mTextLast;
    private String mTextNext;
    private String mTextLastTs;
    private String mTextLastDesc;
    private String mTextNextTs;
    private String mTextNextDesc;
    private Paint mDummyPaint;
    private Runnable mVisibilityChangedCallback;

    public GlobalStatus(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAccentLeft = getResBitmap(R.drawable.globalstatus_accent_lines_left);

        // Fix to make GLE happy
        mDummyPaint = new Paint();

        int textFlags = Paint.ANTI_ALIAS_FLAG + Paint.SUBPIXEL_TEXT_FLAG;
        mPaintLast = new Paint(textFlags);
        mPaintLast.setColor(0xFF70D000);
        mPaintLast.setTextSize(convertDipToPixel(16));

        mPaintNext = new Paint(textFlags);
        mPaintNext.setColor(0xFF392394);
        mPaintNext.setTextSize(convertDipToPixel(16));

        mPaintTimestamp = new Paint(textFlags);
        mPaintTimestamp.setColor(0xFFCCCCCC);
        mPaintTimestamp.setTextSize(convertDipToPixel(12));

        mPaintDesc = new Paint(textFlags);
        mPaintDesc.setColor(0xFF181818);
        mPaintDesc.setTextSize(convertDipToPixel(12));

        FontMetrics fmLast = mPaintLast.getFontMetrics();
        FontMetrics fmTs = mPaintTimestamp.getFontMetrics();
        mYLast = -1 * fmLast.top;
        mYLastDesc = mYLast + fmTs.bottom - fmTs.top;
        mYNext = mYLast - fmLast.ascent + fmTs.descent;
        mYNextDesc = mYNext + fmTs.bottom - fmTs.top;

        mTextLast = context.getString(R.string.globalstatus_last);
        mTextNext = context.getString(R.string.globalstatus_next);

        // use witdh from globble toggle anim to align text
        Bitmap logoAnim = getResBitmap(R.drawable.globaltoggle_frame1);
        mXLastNext = logoAnim.getWidth();
        mXTsDesc = mXLastNext + 5 +
                        (int) (Math.max(mPaintLast.measureText(mTextLast),
                                        mPaintNext.measureText(mTextNext)));
    }

    /** Convert the dips to pixels */
    private float convertDipToPixel(float dip) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return dip * scale;
    }

    public void setTextLastTs(String textLastTs) {
        mTextLastTs = textLastTs;
    }

    public void setTextLastDesc(String lastDesc) {
        mTextLastDesc = lastDesc;
    }

    public void setTextNextTs(String textNextTs) {
        mTextNextTs = textNextTs;
    }

    public void setTextNextDesc(String nextDesc) {
        mTextNextDesc = nextDesc;
    }

    private Bitmap getResBitmap(int bmpResId) {
        Drawable d = getResources().getDrawable(bmpResId);
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w - 1, h - 1);
        d.draw(c);
        return bmp;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return mAccentLeft.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getSuggestedMinimumHeight(),
                                                        MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mAccentRightMatrix = new Matrix();
        mAccentRightMatrix.preTranslate(w, 0);
        mAccentRightMatrix.preScale(-1, 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            canvas.drawBitmap(mAccentLeft, 0, 0, mDummyPaint);
            canvas.drawBitmap(mAccentLeft, mAccentRightMatrix, mDummyPaint);

            canvas.drawText(mTextLast, mXLastNext, mYLast, mPaintLast);
            canvas.drawText(mTextNext, mXLastNext, mYNext, mPaintNext);

            if (mTextLastTs != null && mTextLastTs.length() > 0) {
                canvas.drawText(mTextLastTs, mXTsDesc, mYLast, mPaintTimestamp);
            }

            if (mTextLastDesc != null && mTextLastDesc.length() > 0) {
                canvas.drawText(mTextLastDesc, mXTsDesc, mYLastDesc, mPaintDesc);
            }

            if (mTextNextTs != null && mTextNextTs.length() > 0) {
                canvas.drawText(mTextNextTs, mXTsDesc, mYNext, mPaintTimestamp);
            }

            if (mTextNextDesc != null && mTextNextDesc.length() > 0) {
                canvas.drawText(mTextNextDesc, mXTsDesc, mYNextDesc, mPaintDesc);
            }
        } catch (UnsupportedOperationException e) {
            // Ignore, for GLE
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (mVisibilityChangedCallback != null && visibility == View.VISIBLE) {
            mVisibilityChangedCallback.run();
        }
        super.onWindowVisibilityChanged(visibility);
    }

    public void setWindowVisibilityChangedCallback(Runnable visibilityChangedCallback) {
        mVisibilityChangedCallback = visibilityChangedCallback;
    }
}


