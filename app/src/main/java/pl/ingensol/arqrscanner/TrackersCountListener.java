package pl.ingensol.arqrscanner;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

abstract class TrackersCountListener {

    private static final int HANDLE_TRACKER_DONE_DELAY_MS = 2000;

    private Set<Tracker<Barcode>> mEnabledTrackers = new HashSet<>();
    private Timer mTimer;


    TrackersCountListener(Timer timer) {
        mTimer = timer;
    }

    synchronized void onTrackerEnabled(Tracker<Barcode> tracker) {
        mEnabledTrackers.add(tracker);
        onTrackersCountChanged(mEnabledTrackers.size());
    }

    void onTrackerDone(final Tracker<Barcode> tracker) {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (TrackersCountListener.this) {
                    mEnabledTrackers.remove(tracker);
                    onTrackersCountChanged(mEnabledTrackers.size());
                }
            }
        }, HANDLE_TRACKER_DONE_DELAY_MS);
    }

    protected abstract void onTrackersCountChanged(int size);

}
