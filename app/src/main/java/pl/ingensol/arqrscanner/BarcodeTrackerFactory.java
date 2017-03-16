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
import android.os.AsyncTask;
import android.util.Log;

import pl.ingensol.arqrscanner.camera.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode>, LoadedValueMemo {

    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    private Map<PresentedObjectKey, Object> mLoadedValues;

    BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay) {
        mGraphicOverlay = barcodeGraphicOverlay;
        mLoadedValues = new HashMap<>();
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
        return new BarcodeGraphicTracker(mGraphicOverlay, graphic, this);
    }

    public synchronized Object getLoadedValue(PresentedObjectKey key) {
        Object value = mLoadedValues.get(key);
        if (value == null) {
            value = loadValue(key);
        }
        if (value != null) {
            mLoadedValues.put(key, value);
        }
        return value;
    }

    private Object loadValue(PresentedObjectKey key) {
        if (key instanceof PresentedImageKey) {
            try {
                PresentedImageKey presentedImage = (PresentedImageKey) key;
                return new DownloadImageTask().execute(presentedImage.getUrl()).get();
            } catch (Exception e) {
                Log.e("barcode", "Image downloading execution exception", e);
            }
        }
        return null;
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