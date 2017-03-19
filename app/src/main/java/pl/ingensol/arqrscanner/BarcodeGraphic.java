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
    private int mId;

    private Paint mRectPaint;
    private Paint mImagePaint;
    private Paint mTextPaint;

    private float[] mImagePoints;
    private float[] mDstPoints;
    private float[] mTranslatedPoints;
    private float[] mRotatedPoints;

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
        mDstPoints = new float[POINTS_IN_RECT_XY_COUNT];
        mTranslatedPoints = new float[POINTS_IN_RECT_XY_COUNT];
        mRotatedPoints = new float[POINTS_IN_RECT_XY_COUNT];

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
        computeDestinationPoints(presentedObject, mDstPoints);
        if (presentedObject.getKey() instanceof PresentedImageKey) {
            drawImage(canvas, presentedObject);
        } else {
            drawRawText(canvas, presentedObject);
        }
    }

    private void computeDestinationPoints(PresentedObject presentedObject, float[] dst) {
        translate(presentedObject.getBarcode().cornerPoints, mTranslatedPoints);
        int topLeftPointXyIndex = findTopLeftPointXyIndex(mTranslatedPoints);
        copyShifted(mTranslatedPoints, dst, topLeftPointXyIndex);
    }

    private void translate(Point[] src, float[] dst) {
        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            Point point = src[i];
            dst[i * 2] = translateX(point.x);
            dst[i * 2 + 1] = translateY(point.y);
        }
    }

    private int findTopLeftPointXyIndex(float[] points) {
        PointF center = findCenter(points);
        Matrix rotationWithRespectToCenterMatrix = computeRotationWithRespectToPointMatrix(getRotationMatrix(), center);
        rotationWithRespectToCenterMatrix.mapPoints(mRotatedPoints, points);

        float minXY = Float.MAX_VALUE;
        int topLeftPoint = -1;
        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            float x = mRotatedPoints[i * 2];
            float y = mRotatedPoints[i * 2 + 1];
            float pointXY = x * x + y * y;
            if (pointXY < minXY) {
                minXY = pointXY;
                topLeftPoint = i;
            }
        }

        return topLeftPoint * 2;
    }

    private Matrix computeRotationWithRespectToPointMatrix(Matrix rotationMatrix, PointF center) {
        Matrix rotationWithRespectToCenterMatrix = new Matrix(rotationMatrix);
        rotationWithRespectToCenterMatrix.preTranslate(-center.x, -center.y);
        rotationWithRespectToCenterMatrix.postTranslate(center.x, center.y);
        return rotationWithRespectToCenterMatrix;
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

    private void copyShifted(float[] src, float[] dst, int shift) {
        int pointsFromShiftToEnd = POINTS_IN_RECT_XY_COUNT - shift;
        System.arraycopy(src, shift, dst, 0,                    pointsFromShiftToEnd);
        System.arraycopy(src, 0,     dst, pointsFromShiftToEnd, shift);
    }

    private void drawImage(Canvas canvas, PresentedObject presentedImage) {
        Bitmap bitmap = (Bitmap) presentedImage.getLoadedValue();

        mImagePoints[0] = 0;
        mImagePoints[1] = 0;

        mImagePoints[2] = bitmap.getWidth();
        mImagePoints[3] = 0;

        mImagePoints[4] = bitmap.getWidth();
        mImagePoints[5] = bitmap.getHeight();

        mImagePoints[6] = 0;
        mImagePoints[7] = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(mImagePoints, 0, mDstPoints, 0, POINTS_IN_RECT_COUNT);
        canvas.drawBitmap(bitmap, matrix, mImagePaint);
    }

    private void drawRawText(Canvas canvas, PresentedObject presentedText) {
        mPath.reset();
        mPath.moveTo(mDstPoints[0], mDstPoints[1]);

        for (int i = 0; i < POINTS_IN_RECT_COUNT; i++) {
            mPath.lineTo(
                    mDstPoints[(i + 1) % POINTS_IN_RECT_XY_COUNT],
                    mDstPoints[(i + 1) % POINTS_IN_RECT_XY_COUNT + 1]);
        }
        canvas.drawPath(mPath, mRectPaint);

        Rect rect = presentedText.getBarcode().getBoundingBox();
        PresentedTextKey key = (PresentedTextKey) presentedText.getKey();
        canvas.drawText(key.getText(), translateX(rect.left), translateY(rect.top + FONT_HEIGHT), mTextPaint);
    }

}