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

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import java.net.MalformedURLException;
import java.net.URL;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
class BarcodeGraphicTracker extends Tracker<Barcode> {
    private GraphicOverlay<BarcodeGraphic> mOverlay;
    private BarcodeGraphic mGraphic;
    private LoadedValueMemo mLoadedValueMemo;

    BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> overlay, BarcodeGraphic graphic, LoadedValueMemo loadedValueMemo) {
        mOverlay = overlay;
        mGraphic = graphic;
        mLoadedValueMemo = loadedValueMemo;
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

        if (barcode != null) {
            URL url = parseUrl(barcode);

            PresentedObjectKey key = null;
            if (url != null) {
                key = new PresentedImageKey(url);
            } else {
                key = new PresentedTextKey(barcode.rawValue);
            }

            Object loadedValue = mLoadedValueMemo.getLoadedValue(key);

            mGraphic.updateItem(new PresentedObject(key, barcode, loadedValue));
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

}