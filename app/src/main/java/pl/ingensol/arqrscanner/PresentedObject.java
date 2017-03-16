package pl.ingensol.arqrscanner;

import com.google.android.gms.vision.barcode.Barcode;

import java.net.URL;

class PresentedObject {

    private PresentedObjectKey key;
    private Barcode barcode;
    private Object loadedValue;

    public PresentedObject(PresentedObjectKey key, Barcode barcode, Object loadedValue) {
        this.key = key;
        this.barcode = barcode;
        this.loadedValue = loadedValue;
    }

    public PresentedObjectKey getKey() {
        return key;
    }

    public Barcode getBarcode() {
        return barcode;
    }

    public Object getLoadedValue() {
        return loadedValue;
    }

}

interface PresentedObjectKey {
}

class PresentedImageKey implements PresentedObjectKey {

    private URL url;

    public PresentedImageKey(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresentedImageKey that = (PresentedImageKey) o;

        return url.equals(that.url);

    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

}

class PresentedTextKey implements PresentedObjectKey {

    private String text;

    public PresentedTextKey(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresentedTextKey that = (PresentedTextKey) o;

        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

}