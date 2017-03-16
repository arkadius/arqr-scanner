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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private final int POINTS_IN_RECT_COUNT = 4;
    private final int FONT_HEIGHT = 36;
    private int mId;

    private Paint mRectPaint;
    private Paint mImagePaint;
    private Paint mTextPaint;

    private float[] mSrc;
    private float[] mDst;

    private Path mPath;

    private volatile PresentedObject mPresentedObject;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        mRectPaint = new Paint();
        mRectPaint.setColor(Color.WHITE);
        mRectPaint.setStyle(Paint.Style.FILL);

        mImagePaint = new Paint();

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(FONT_HEIGHT);

        mSrc = new float[POINTS_IN_RECT_COUNT * 2];
        mDst = new float[POINTS_IN_RECT_COUNT * 2];

        mPath = new Path();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public PresentedObject getPresentedObject() {
        return mPresentedObject;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(PresentedObject presentedObject) {
        mPresentedObject = presentedObject;
        postInvalidate();
    }

    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        PresentedObject presentedObject = mPresentedObject;
        if (presentedObject == null) {
            return;
        }
        drawObject(canvas, presentedObject);
    }

    private void drawObject(Canvas canvas, PresentedObject presentedObject) {
        Point[] cornerPoints = presentedObject.getBarcode().cornerPoints;
        for (int i = 0; i < cornerPoints.length; i++) {
            Point point = cornerPoints[i];
            mDst[i * 2] = translateX(point.x);
            mDst[i * 2 + 1] = translateY(point.y);
        }
        if (presentedObject.getKey() instanceof PresentedImageKey) {
            drawImage(canvas, presentedObject);
        } else {
            drawRawText(canvas, presentedObject);
        }
    }

    private void drawImage(Canvas canvas, PresentedObject presentedImage) {
        Bitmap bitmap = (Bitmap) presentedImage.getLoadedValue();

        mSrc[0] = 0;
        mSrc[1] = 0;

        mSrc[2] = bitmap.getWidth();
        mSrc[3] = 0;

        mSrc[4] = bitmap.getWidth();
        mSrc[5] = bitmap.getHeight();

        mSrc[6] = 0;
        mSrc[7] = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(mSrc, 0, mDst, 0, POINTS_IN_RECT_COUNT);
        canvas.drawBitmap(bitmap, matrix, mImagePaint);
    }

    private void drawRawText(Canvas canvas, PresentedObject presentedText) {
        mPath.reset();
        mPath.moveTo(mDst[0], mDst[1]);

        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            mPath.lineTo(
                    mDst[(i + 1) % POINTS_IN_RECT_COUNT * 2],
                    mDst[(i + 1) % POINTS_IN_RECT_COUNT * 2 + 1]);
        }
        canvas.drawPath(mPath, mRectPaint);

        Rect rect = presentedText.getBarcode().getBoundingBox();
        PresentedTextKey key = (PresentedTextKey) presentedText.getKey();
        canvas.drawText(key.getText(), translateX(rect.left), translateY(rect.top + FONT_HEIGHT), mTextPaint);
    }

}