/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ingensol.arqrscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private int mId;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN
    };

    private static int mCurrentColorIndex = 0;

    private Paint mRectPaint;
    private Paint mImagePaint;
    private Paint mTextPaint;
    private volatile Barcode mBarcode;

    private Bitmap mBitmap;
    private float[] mSrc = new float[8];
    private float[] mDst = new float[8];

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mRectPaint = new Paint();
        mRectPaint.setColor(selectedColor);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(4.0f);

        mImagePaint = new Paint();

        mTextPaint = new Paint();
        mTextPaint.setColor(selectedColor);
        mTextPaint.setTextSize(36.0f);

        mBitmap = BitmapFactory.decodeResource(overlay.getResources(), R.drawable.beach);

        mSrc[0] = 0;
        mSrc[1] = 0;

        mSrc[2] = mBitmap.getWidth();
        mSrc[3] = 0;

        mSrc[4] = mBitmap.getWidth();
        mSrc[5] = mBitmap.getHeight();

        mSrc[6] = 0;
        mSrc[7] = mBitmap.getHeight();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Barcode getBarcode() {
        return mBarcode;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Barcode barcode) {
        mBarcode = barcode;
        postInvalidate();
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = mBarcode;
        if (barcode == null) {
            return;
        }

        for (int i = 0; i < barcode.cornerPoints.length; i++) {
            Point point = barcode.cornerPoints[i];
            mDst[i * 2] = translateX(point.x);
            mDst[i * 2 + 1] = translateY(point.y);
        }

        // Draws the bounding box around the barcode.
        for (int i = 0; i < barcode.cornerPoints.length; i++) {
            canvas.drawLine(
                    mDst[i * 2],
                    mDst[i * 2 + 1],
                    mDst[(i + 1) % barcode.cornerPoints.length * 2],
                    mDst[(i + 1) % barcode.cornerPoints.length * 2 + 1],
                    mRectPaint
            );
        }

        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(mSrc, 0, mDst, 0, 4);
        canvas.drawBitmap(mBitmap, matrix, mImagePaint);

        // Draws a label at the bottom of the barcode indicate the barcode value that was detected.
        Rect rect = barcode.getBoundingBox();
        canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint);
    }
}
