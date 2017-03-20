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
import java.util.Timer;
import java.util.TimerTask;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
class BarcodeGraphicTracker extends Tracker<Barcode> {

    private static final int HIDE_AFTER_DONE_DELAY_MS = 300;

    private final GraphicOverlay<BarcodeGraphic> mOverlay;
    private final BarcodeGraphic mGraphic;
    private final LoadedValueMemo mLoadedValueMemo;
    private final TrackersCountListener mTrackersCountListener;
    private final Timer mTimer;

    BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> overlay, BarcodeGraphic graphic,
                          LoadedValueMemo loadedValueMemo, TrackersCountListener trackersCountListener, Timer timer) {
        mOverlay = overlay;
        mGraphic = graphic;
        mLoadedValueMemo = loadedValueMemo;
        mTrackersCountListener = trackersCountListener;
        mTimer = timer;
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
        mTrackersCountListener.onTrackerEnabled(this);
        mOverlay.add(mGraphic);

        if (barcode != null) {
            URL url = parseUrl(barcode);

            PresentedObject object = null;
            if (url != null) {
                PresentedObjectKey key = new PresentedImageKey(url);
                Object loadedValue = mLoadedValueMemo.getLoadedValue(key);
                if (loadedValue == null) {
                    key = new PresentedTextKey(barcode.rawValue);
                }
                object = new PresentedObject(key, barcode, loadedValue);
            } else {
                PresentedObjectKey key = new PresentedTextKey(barcode.rawValue);
                object = new PresentedObject(key, barcode, null);
            }
            mGraphic.updateItem(object);
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
     * This can happen for intermediate frames temporarily, for example if the object was momentarily
     * blocked from view.
     */
    @Override
    public void onMissing(Detector.Detections<Barcode> detectionResults) {
        // do not hide graphics to avoid flickering of image
    }

    /**
     * Called when the item is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        // hide graphics after a while to have a chance for barcode to be handled by other tracker to avoid flickering
        mTimer.schedule(new TimerTask() {
            synchronized public void run() {
                mTrackersCountListener.onTrackerDone(BarcodeGraphicTracker.this);
                mOverlay.remove(mGraphic);
            }
        }, HIDE_AFTER_DONE_DELAY_MS);
    }

}