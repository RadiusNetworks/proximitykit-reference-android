package com.radiusnetworks.androidproximitykitreference;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitMonitorNotifier;
import com.radiusnetworks.proximity.ProximityKitRangeNotifier;
import com.radiusnetworks.proximity.ProximityKitRegion;
import com.radiusnetworks.proximity.ProximityKitSyncNotifier;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconData;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.client.DataProviderException;

import java.util.Date;

/**
 * Created by dyoung on 4/2/14.
 *
 * Note:  the kit associated with this
 */
public class AndroidProximityKitReferenceApplication extends Application implements ProximityKitMonitorNotifier, ProximityKitRangeNotifier, ProximityKitSyncNotifier {

    public static final String TAG = "AndroidProximityKitReferenceApplication";
    Date lastRefreshTime = new Date();
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MainActivity mainActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        ProximityKitManager pkManager = ProximityKitManager.getInstanceForApplication(this);

        /* ----- begin code only for testing ---- */
        pkManager.getBeaconManager().setDebug(true);
        /* ----- end code only for testing ------ */

        pkManager.setProximityKitSyncNotifier(this);
        pkManager.setProximityKitMonitorNotifier(this);
        pkManager.setProximityKitRangeNotifier(this);
        pkManager.start();
    }

    @Override
    public void didSync() {
        Log.d(TAG, "didSync() called");
    }

    @Override
    public void didFailSync(Exception e) {
        Log.d(TAG, "didFailSync() called with exception: "+e);
    }

    @Override
    public void beaconDataUpdate(Beacon beacon, BeaconData beaconData, DataProviderException e) {
        if (e != null) {
            Log.d(TAG, "data fetch error:" + e);
        }
        if (beaconData != null) {
            Log.d(
                    TAG,
                    "I have an beacon with data: " + beacon + " welcomeMessage=" +
                            beaconData.get("welcomeMessage")
            );
            // You could instead call beacon.toString() which wraps up the identifiers
            String displayString = beacon.getId1() + " " +
                    beacon.getId2().toInt() + " " + beacon.getId3().toInt() +
                    "\nWelcome message: " + beaconData.get("welcomeMessage");
            if (mainActivity != null) {
                mainActivity.displayTableRow(beacon, displayString, true);
            }
            if (new Date().getTime() - lastRefreshTime.getTime() > 24 * 60 * 60000l) { /* one day */
                Log.d(TAG, "It is time to force a refresh of beacon data");
                beaconData.sync(this);
                lastRefreshTime = new Date();
            }
        }
    }

    @Override
    public void didEnterRegion(ProximityKitRegion region) {
        // In this example, this class sends a notification to the user whenever an beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region: " + region);
        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;
        } else {
            // If we have already seen beacons and launched the MainActivity before, we simply
            // send a notification to the user on subsequent detections.
            Log.d(TAG, "Sending notification.");
            sendNotification();
        }
    }

    @Override
    public void didExitRegion(ProximityKitRegion region) {
        Log.d(TAG, "didExitRegion called with region: "+region);
    }

    @Override
    public void didDetermineStateForRegion(int i, ProximityKitRegion region) {
        Log.d(TAG, "didDeterineStateForRegion called with region: "+region);
    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Proximity Kit Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }
}
