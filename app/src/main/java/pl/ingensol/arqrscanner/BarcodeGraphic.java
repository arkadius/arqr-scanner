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
import android.graphics.PointF;
import android.graphics.Rect;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private final int POINTS_IN_RECT_COUNT = 4;
    private final int POINTS_IN_RECT_XY_COUNT = POINTS_IN_RECT_COUNT * 2;
    private final int FONT_HEIGHT = 36;
    private final float SCALE = 2.5f;
    private int mId;

    private Paint mRectPaint;
    private Paint mImagePaint;
    private Paint mTextPaint;

    private float[] mImagePoints;
    private float[] mTranslatedCornerPoints;
    private float[] mRotatedCornerPoints;
    private float[] mBarcodePoints;
    private float[] mScaledBarcodePoints;

    private Matrix mMatrix;
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

        mImagePoints = new float[POINTS_IN_RECT_XY_COUNT];
        mBarcodePoints = new float[POINTS_IN_RECT_XY_COUNT];
        mTranslatedCornerPoints = new float[POINTS_IN_RECT_XY_COUNT];
        mRotatedCornerPoints = new float[POINTS_IN_RECT_XY_COUNT];
        mScaledBarcodePoints = new float[POINTS_IN_RECT_XY_COUNT];

        mMatrix = new Matrix();
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
        translate(presentedObject.getBarcode().cornerPoints, mTranslatedCornerPoints);
        PointF center = findCenter(mTranslatedCornerPoints);
        computeBarcodePoints(mTranslatedCornerPoints, mBarcodePoints, center);
        if (presentedObject.getKey() instanceof PresentedImageKey) {
            drawImage(canvas, presentedObject, mBarcodePoints, center);
        } else {
            drawRawText(canvas, presentedObject, mBarcodePoints, center);
        }
    }

    private void translate(Point[] src, float[] dst) {
        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            Point point = src[i];
            dst[i * 2] = translateX(point.x);
            dst[i * 2 + 1] = translateY(point.y);
        }
    }

    private PointF findCenter(float[] points) {
        float xSum = 0;
        float ySum = 0;
        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            float x = points[i * 2];
            float y = points[i * 2 + 1];
            xSum += x;
            ySum += y;
        }
        return new PointF(xSum / POINTS_IN_RECT_COUNT, ySum / POINTS_IN_RECT_COUNT);
    }

    private void computeBarcodePoints(float[] src, float[] dst, PointF center) {
        int topLeftPointXyIndex = findTopLeftPointXyIndex(src, center);
        copyShifted(src, dst, topLeftPointXyIndex);
    }

    private int findTopLeftPointXyIndex(float[] points, PointF center) {
        mMatrix.reset();
        mMatrix.set(getRotationMatrix());
        transformRespectToPoint(mMatrix, center);
        mMatrix.mapPoints(mRotatedCornerPoints, points);

        float minXY = Float.MAX_VALUE;
        int topLeftPoint = -1;
        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            float x = mRotatedCornerPoints[i * 2];
            float y = mRotatedCornerPoints[i * 2 + 1];
            float pointXY = x * x + y * y;
            if (pointXY < minXY) {
                minXY = pointXY;
                topLeftPoint = i;
            }
        }

        return topLeftPoint * 2;
    }

    private void copyShifted(float[] src, float[] dst, int shift) {
        int pointsFromShiftToEnd = POINTS_IN_RECT_XY_COUNT - shift;
        System.arraycopy(src, shift, dst, 0,                    pointsFromShiftToEnd);
        System.arraycopy(src, 0,     dst, pointsFromShiftToEnd, shift);
    }

    private void drawImage(Canvas canvas, PresentedObject presentedImage, float[] barcodePoints, PointF center) {
        Bitmap bitmap = (Bitmap) presentedImage.getLoadedValue();

        mImagePoints[0] = 0;
        mImagePoints[1] = 0;

        mImagePoints[2] = bitmap.getWidth();
        mImagePoints[3] = 0;

        mImagePoints[4] = bitmap.getWidth();
        mImagePoints[5] = bitmap.getHeight();

        mImagePoints[6] = 0;
        mImagePoints[7] = bitmap.getHeight();

        scalePointsWithRespectToPointKeepingRatio(barcodePoints, mScaledBarcodePoints, center, bitmap.getWidth(), bitmap.getHeight(), SCALE);

        mMatrix.reset();
        mMatrix.setPolyToPoly(mImagePoints, 0, mScaledBarcodePoints, 0, POINTS_IN_RECT_COUNT);

        canvas.drawBitmap(bitmap, mMatrix, mImagePaint);
    }

    private void drawRawText(Canvas canvas, PresentedObject presentedText, float[] barcodePoints, PointF center) {
        scalePointsWithRespectToPointKeepingRatio(barcodePoints, mScaledBarcodePoints, center, 1, 1, SCALE);

        mPath.reset();
        mPath.moveTo(mScaledBarcodePoints[0], mScaledBarcodePoints[1]);

        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            mPath.lineTo(
                    mScaledBarcodePoints[(i + 1) % POINTS_IN_RECT_COUNT * 2],
                    mScaledBarcodePoints[(i + 1) % POINTS_IN_RECT_COUNT * 2 + 1]);
        }
        canvas.drawPath(mPath, mRectPaint);

        Rect rect = presentedText.getBarcode().getBoundingBox();
        PresentedTextKey key = (PresentedTextKey) presentedText.getKey();
        canvas.drawText(key.getText(), translateX(rect.left), translateY(rect.top + FONT_HEIGHT), mTextPaint);
    }

    private void scalePointsWithRespectToPointKeepingRatio(float[] src, float[] dst, PointF point, int width, int height, float scale) {
        float scaleBase = Math.min(width, height);
        float scaleX = width / scaleBase * scale;
        float scaleY = height / scaleBase * scale;

        mMatrix.reset();
        mMatrix.setScale(scaleX, scaleY);
        transformRespectToPoint(mMatrix, point);

        mMatrix.mapPoints(dst, src);
    }

    private void transformRespectToPoint(Matrix matrix, PointF point) {
        matrix.preTranslate(-point.x, -point.y);
        matrix.postTranslate(point.x, point.y);
    }

}