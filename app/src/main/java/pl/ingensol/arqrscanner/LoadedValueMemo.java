package pl.ingensol.arqrscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class LoadedValueMemo {

    private Map<PresentedObjectKey, Object> mLoadedValues = new HashMap<>();

    synchronized Object getLoadedValue(PresentedObjectKey key) {
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