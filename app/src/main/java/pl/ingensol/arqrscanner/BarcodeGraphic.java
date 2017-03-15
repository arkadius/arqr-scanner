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
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.text.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private int mId;

    private Paint mRectPaint;
    private Paint mImagePaint;
    private Paint mTextPaint;
    private volatile Barcode mBarcode;

    private float[] mSrc = new float[8];
    private float[] mDst = new float[8];
    private Path mPath;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        mRectPaint = new Paint();
        mRectPaint.setColor(Color.WHITE);
        mRectPaint.setStyle(Paint.Style.FILL);

        mImagePaint = new Paint();

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(36.0f);

        mPath = new Path();
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

        Bitmap bitmap = null;
        URL url = null;
        if (barcode.url != null && !TextUtils.isEmpty(barcode.url.url)) {
            try {
                url = new URL(barcode.url.url);
            } catch (MalformedURLException e) {
                Log.e("barcode", "Invalid url in url barcode: " + barcode.url.url, e);
            }
        }
        try {
            url = new URL(barcode.rawValue);
        } catch (MalformedURLException e2) {
            Log.e("barcode", "Invalid url in raw barcode: " + barcode.rawValue, e2);
        }

        if (url != null) {
            try {
                bitmap = new DownloadImageTask().execute(url).get();
            } catch (InterruptedException e) {
                Log.e("barcode", "Image downloading interrupted", e);
            } catch (ExecutionException e) {
                Log.e("barcode", "Image downloading execution exception", e);
            }
        }

        if (bitmap != null) {
            drawImage(canvas, bitmap);
        } else {
            drawRawText(canvas, barcode);
        }
    }

    private void drawImage(Canvas canvas, Bitmap bitmap) {
        mSrc[0] = 0;
        mSrc[1] = 0;

        mSrc[2] = bitmap.getWidth();
        mSrc[3] = 0;

        mSrc[4] = bitmap.getWidth();
        mSrc[5] = bitmap.getHeight();

        mSrc[6] = 0;
        mSrc[7] = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(mSrc, 0, mDst, 0, 4);
        canvas.drawBitmap(bitmap, matrix, mImagePaint);
    }

    private void drawRawText(Canvas canvas, Barcode barcode) {
        mPath.reset();
        mPath.moveTo(mDst[0], mDst[1]);

        for (int i = 0; i < barcode.cornerPoints.length; i++) {
            mPath.lineTo(
                    mDst[(i + 1) % barcode.cornerPoints.length * 2],
                    mDst[(i + 1) % barcode.cornerPoints.length * 2 + 1]);
        }
        canvas.drawPath(mPath, mRectPaint);

        Rect rect = barcode.getBoundingBox();
        canvas.drawText(barcode.rawValue, translateX(rect.left), translateY(rect.top), mTextPaint);
    }

    private static class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(URL... urls) {
            try {
                return BitmapFactory.decodeStream(urls[0].openStream());
            } catch (IOException e) {
                Log.e("barcode", "Invalid image stream", e);
                return null;
            }
        }
    }

}
