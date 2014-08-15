package com.radiusnetworks.androidproximitykitreference;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.radiusnetworks.proximity.ProximityKitGeofenceNotifier;
import com.radiusnetworks.proximity.ProximityKitGeofenceRegion;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitMonitorNotifier;
import com.radiusnetworks.proximity.ProximityKitRangeNotifier;
import com.radiusnetworks.proximity.ProximityKitSyncNotifier;
import com.radiusnetworks.proximity.geofence.GooglePlayServicesException;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconData;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.client.DataProviderException;

import java.util.Date;

/**
 */
public class AndroidProximityKitReferenceApplication
        extends     Application
        implements  ProximityKitMonitorNotifier,
                    ProximityKitRangeNotifier,
                    ProximityKitSyncNotifier,
                    ProximityKitGeofenceNotifier {

    public static final String TAG = "AndroidProximityKitReferenceApplication";
    Date lastRefreshTime = new Date();
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MainActivity mainActivity = null;

    @Override
    /**
     * @see <a href="https://developer.android.com/google/play-services/setup.html">
     *          Setup Google Play services
     *      </a>
     */
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        ProximityKitManager pkManager = ProximityKitManager.getInstanceForApplication(this);

        /* ----- begin code only for testing ---- */
        pkManager.debugOn();
        /* ----- end code only for testing ------ */

        if (servicesConnected()) {
            try {
                pkManager.enableGeofences();
            } catch (GooglePlayServicesException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        pkManager.setProximityKitSyncNotifier(this);
        pkManager.setProximityKitMonitorNotifier(this);
        pkManager.setProximityKitRangeNotifier(this);
        pkManager.setProximityKitGeofenceNotifier(this);
        pkManager.start();
    }

    @Override
    public void didSync() {
        Log.d(TAG, "didSync() called");
    }

    @Override
    public void didFailSync(Exception e) {
        Log.d(TAG, "didFailSync() called with exception: " + e);
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
    public void didEnterRegion(Region region) {
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
    public void didExitRegion(Region region) {
        Log.d(TAG, "didExitRegion called with region: " + region);
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Log.d(TAG, "didDeterineStateForRegion called with region: " + region);
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

    @Override
    public void didEnterGeofence(ProximityKitGeofenceRegion region) {
        Log.d(TAG, "didEnterGeofenceRegion called with region: " + region);
    }

    @Override
    public void didExitGeofence(ProximityKitGeofenceRegion region) {
        Log.d(TAG, "didExitGeofenceRegion called with region: " + region);
    }

    @Override
    public void didDetermineStateForGeofence(int i, ProximityKitGeofenceRegion region) {
        Log.d(TAG, "didDeterineStateForGeofence called with region: " + region);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @see <a href="https://developer.android.com/google/play-services/setup.html">
     *          Setup Google Play services
     *      </a>
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS != resultCode) {
            Log.w(TAG, GooglePlayServicesUtil.getErrorString(resultCode));
            GooglePlayServicesUtil.showErrorNotification(resultCode, this);

            return false;
        }

        Log.d(TAG, "Google Play services available");
        return true;
    }
}
