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
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
class BarcodeGraphicTracker extends Tracker<Barcode> {
    private GraphicOverlay<BarcodeGraphic> mOverlay;
    private BarcodeGraphic mGraphic;
    private PresentedObject mLoadedObject;

    BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> overlay, BarcodeGraphic graphic) {
        mOverlay = overlay;
        mGraphic = graphic;
    }

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    @Override
    public void onNewItem(int id, Barcode item) {
        mGraphic.setId(id);
    }

    /**
     * Update the position/characteristics of the item within the overlay.
     */
    @Override
    public void onUpdate(Detector.Detections<Barcode> detectionResults, Barcode barcode) {
        mOverlay.add(mGraphic);

        PresentedObject presentedObject = null;
        if (barcode != null) {
            URL url = parseUrl(barcode);

            if (url != null) {
                presentedObject = new PresentedImage(url);
            } else {
                presentedObject = new PresentedText(barcode.rawValue);
            }

            synchronized (this) {
                if (!presentedObject.equals(mLoadedObject)) {
                    mLoadedObject = loadObject(presentedObject, barcode);
                }
                mLoadedObject.setBarcode(barcode);
                mGraphic.updateItem(mLoadedObject);
            }
        } else {
            mGraphic.updateItem(null);
        }
    }


    private URL parseUrl(Barcode barcode) {
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
        return url;
    }

    private PresentedObject loadObject(PresentedObject presentedObject, Barcode barcode) {
        if (presentedObject instanceof PresentedImage) {
            try {
                PresentedImage presentedImage = (PresentedImage) presentedObject;
                Bitmap bitmap = new DownloadImageTask().execute(presentedImage.getUrl()).get();
                presentedImage.setBitmap(bitmap);
            } catch (Exception e) {
                Log.e("barcode", "Image downloading execution exception", e);
                presentedObject = new PresentedText(barcode.rawValue);
            }
        }
        return presentedObject;
    }

    /**
     * Hide the graphic when the corresponding object was not detected.  This can happen for
     * intermediate frames temporarily, for example if the object was momentarily blocked from
     * view.
     */
    @Override
    public void onMissing(Detector.Detections<Barcode> detectionResults) {
        mOverlay.remove(mGraphic);
    }

    /**
     * Called when the item is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mGraphic);
    }


    private static class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(URL... urls) {
            URL url = urls[0];
            Log.i("barcode", "Loading image: " + url);
            try {
                return BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                Log.e("barcode", "Invalid image stream", e);
                return null;
            }
        }
    }

}

interface PresentedObject {

    Barcode getBarcode();

    void setBarcode(Barcode barcode);
}

class PresentedImage implements PresentedObject {
    private URL url;
    private Bitmap bitmap;
    private Barcode barcode;

    public PresentedImage(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public Barcode getBarcode() {
        return barcode;
    }

    @Override
    public void setBarcode(Barcode barcode) {
        this.barcode = barcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresentedImage that = (PresentedImage) o;

        return url.equals(that.url);

    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}

class PresentedText implements PresentedObject {
    private String text;
    private Barcode barcode;

    public PresentedText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public Barcode getBarcode() {
        return barcode;
    }

    @Override
    public void setBarcode(Barcode barcode) {
        this.barcode = barcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresentedText that = (PresentedText) o;

        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}
