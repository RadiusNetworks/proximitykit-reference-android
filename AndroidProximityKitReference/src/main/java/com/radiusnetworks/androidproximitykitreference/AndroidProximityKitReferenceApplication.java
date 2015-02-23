package com.radiusnetworks.androidproximitykitreference;

import org.altbeacon.beacon.BeaconParser;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.radiusnetworks.proximity.ProximityKitBeacon;
import com.radiusnetworks.proximity.ProximityKitBeaconRegion;
import com.radiusnetworks.proximity.ProximityKitGeofenceNotifier;
import com.radiusnetworks.proximity.ProximityKitGeofenceRegion;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitMonitorNotifier;
import com.radiusnetworks.proximity.ProximityKitRangeNotifier;
import com.radiusnetworks.proximity.ProximityKitSyncNotifier;
import com.radiusnetworks.proximity.beacon.BeaconManager;
import com.radiusnetworks.proximity.geofence.GooglePlayServicesException;
import com.radiusnetworks.proximity.model.KitBeacon;
import com.radiusnetworks.proximity.model.KitOverlay;

import java.util.Collection;

/**
 */
public class AndroidProximityKitReferenceApplication
        extends Application
        implements ProximityKitMonitorNotifier,
        ProximityKitRangeNotifier,
        ProximityKitSyncNotifier,
        ProximityKitGeofenceNotifier {

    private static final String MAIN_OFFICE_LOCATION = "main-office";
    private static final long   ONE_DAY_IN_MS        = 24 * 60 * 60 * 1000l;
    private static final int ROLLUP_THRESHOLD        = 100;
    public static final  String TAG                  = "AndroidProximityKitReferenceApplication";

    private boolean             haveDetectedBeaconsSinceBoot = false;
    private MainActivity        mainActivity                 = null;
    private ProximityKitManager pkManager                    = null;

    @Override
    /**
     * It is the job of the application to ensure that Google Play services is available before
     * enabling geofences in Proximity Kit.
     *
     * A good place to do this is when we set the Proximity Kit manager instance. However there are
     * issues with this decision. See the notes in <code>servicesConnected()</code> for details.
     *
     * This is also where we are setting the notifier callbacks. Be aware that, currently, only one
     * notifier can be set per notifier type. This means if the app (this demo app does not) sets
     * another notifier somewhere else, it will overwrite this notifier.
     *
     * @see #servicesConnected()
     * @see <a href="https://developer.android.com/google/play-services/setup.html">
     *          Setup Google Play services
     *      </a>
     */
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        // Hold a reference to the Proximity Kit manager to access the kit later
        pkManager = ProximityKitManager.getInstanceForApplication(this);

        /* ----- begin code only for debugging ---- */

        pkManager.debugOn();

        /* ----- end code only for debugging ------ */

        /*
         * All current versions of ProximityKit Android use the AltBeacon/android-beacon-library
         * under the hood.
         *
         * By default only the AltBeacon format is picked up on Android devices. However, we are
         * free to configure your own custom format by registering a parser with your
         * ProximityKitManager's BeaconManager.
         */
        BeaconManager beaconManager = pkManager.getBeaconManager();
        beaconManager.getBeaconParsers().add(
                new BeaconParser().setBeaconLayout(
                        "m:2-5=c0decafe,i:6-13,i:14-17,p:18-18,d:19-22,d:23-26"
                                                  )
                                            );

        /*
         * Sets the maximum number of regions that may be individually configured in ProximityKit
         * before the regions will start being automatically combined.  By default,
         * each beacon or region in the ProximityKit server results in one monitoring and ranging
         * region.  In cases of large numbers of individual beacons configured on the server,
         * these beacons will be combined into a smaller number of matching regions for
         * monitoring and ranging purposes.
         *
         * By default, this value is -1, which means that no rollup will occur.
         */
        pkManager.setMaxRegionsBeforeRollup(ROLLUP_THRESHOLD);

        /*
         * It is our job (the app) to ensure that Google Play services is available. If it is not
         * then attempting to enable geofences in Proximity Kit will fail, throwing a
         * GooglePlayServicesException. This will happen in the following conditions:
         *
         * - We forget to include Google Play services as a dependency of our applicaiton
         * - The device the app is running on does not have Google Play services
         * - The device the app is running on has an outdated version of Google Play services
         *
         * It is our responsibility to handle this, as we (the app), are the only one in a position
         * to decide how to behave if this service is not available.
         *
         * In this example, we've decided to check to make sure the service is available. In the
         * event we think the service is available, but enabling geofences still fails, we log the
         * error and continue without geofences.
         *
         * See servicesConnected for how we handle the cases where the device doesn't have Google
         * Play services, or the version is out of date.
         */
        if (servicesConnected()) {
            // As a safety mechanism, `enableGeofences()` throws a checked exception in case the
            // app does not properly handle Google Play support.
            try {
                pkManager.enableGeofences();

                // No point setting the geofence notifier if we aren't using geofences
                pkManager.setProximityKitGeofenceNotifier(this);
            } catch (GooglePlayServicesException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        /*
         * Set desired callbacks before calling `start()`.
         *
         * We can set these notifications after calling `start()`. However, this means we will miss
         * any notifications posted in the time between those actions.
         *
         * You are free to set only the notifiers you want callbacks for. We are setting all of them
         * to demonstrate how each set works.
         */
        pkManager.setProximityKitSyncNotifier(this);
        pkManager.setProximityKitMonitorNotifier(this);
        pkManager.setProximityKitRangeNotifier(this);

        /*
         * Now that we potentially have geofences setup and our notifiers are registered, we are
         * ready to start Proximity Kit.
         *
         * We could start it right now with:
         *
         *      pkManager.start();
         *
         * Instead we are letting the user decide when to start or stop in the UI.
         */
    }

    /**
     * Start the Proximity Kit Manager.
     * <p/>
     * Allows the app to control when the Proximity Kit manager is running. This can similarly used
     * by libraries to hook into when Proximity Kit manager should run.
     */
    public void startManager() {
        pkManager.start();
    }

    /**
     * Stop the Proximity Kit Manager.
     * <p/>
     * Allows the app to control when the Proximity Kit manager is running. This can similarly used
     * by libraries to hook into when Proximity Kit manager should run.
     */
    public void stopManager() {
        pkManager.stop();
    }

    /**
     * Verify that Google Play services is available.
     * <p/>
     * If the service is not available it could be due to several reasons. We take the easy way out
     * in this demo and simply log the error. We then use the utility class provided to pop a
     * notification to the end user with the message.
     * <p/>
     * Google Play services controls the text and content of this notification. We could roll our
     * own notification, display a dialog (which would require an Activity context), or do
     * something
     * else. This is why it is our (the app) responsibility to make this decision and not left up
     * to Proximity Kit.
     *
     * @return <code>true</code> if Google Play services is available, otherwise <code>false</code>
     * @see <a href="https://developer.android.com/google/play-services/setup.html">
     * Setup Google Play services
     * </a>
     */
    public boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS != resultCode) {
            // Taking the easy way out log it, then let Google Play pop the appropriate notification
            Log.w(TAG, GooglePlayServicesUtil.getErrorString(resultCode));
            GooglePlayServicesUtil.showErrorNotification(resultCode, this);

            return false;
        }

        Log.d(TAG, "Google Play services available");
        return true;
    }

    /**
     * Set main activity for app display related callbacks.
     *
     * @param mainActivity
     *         <code>Activity</code> to send app display related callbacks
     */
    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /***********************************************************************************************
     * START ProximityKitSyncNotifier
     **********************************************************************************************/

    @Override
    /**
     * Called when data has been sync'd with the Proximity Kit server.
     */
    public void didSync() {
        Log.d(TAG, "didSync(): Sycn'd with server");

        // Access every beacon configured in the kit, printing out the value of an attribute
        // named "myKey"
        for (KitBeacon beacon : pkManager.getKit().getBeacons()) {
            Log.d(
                    TAG,
                    "For beacon: " + beacon.getProximityUuid() + " " + beacon.getMajor() + " " +
                            beacon.getMinor() + ", the value of welcomeMessage is " +
                            beacon.getAttributes().get("welcomeMessage")
                 );
        }

        // Access every geofence configured in the kit, printing out the value of an attribute
        // named "myKey"
        for (KitOverlay overlay : pkManager.getKit().getOverlays()) {
            Log.d(
                    TAG,
                    "For geofence: (" + overlay.getLatitude() + ", " + overlay.getLongitude() +
                            ") with radius " + overlay.getRadius() + ", the value of myKey is " +
                            overlay.getAttributes().get("myKey")
                 );
        }
    }

    @Override
    /**
     * Called when syncing with the Proximity Kit server failed.
     *
     * @param e     The exception encountered while syncing
     */
    public void didFailSync(Exception e) {
        Log.d(TAG, "didFailSync() called with exception: " + e);
    }

    /***********************************************************************************************
     * END ProximityKitSyncNotifier
     **********************************************************************************************/

    /***********************************************************************************************
     * START ProximityKitRangeNotifier
     * ********************************************************************************************/

    @Override
    /**
     * Called whenever the Proximity Kit manager sees registered beacons.
     *
     * @param beacons   a collection of <code>ProximityKitBeacon</code> instances seen in the most
     *                  recent ranging cycle.
     * @param region    The <code>ProximityKitBeaconRegion</code> instance that was used to start
     *                  ranging for these beacons.
     */
    public void didRangeBeaconsInRegion(Collection<ProximityKitBeacon> beacons, ProximityKitBeaconRegion region) {
        if (beacons.size() == 0) {
            return;
        }

        Log.d(TAG, "didRangeBeaconsInRegion: size=" + beacons.size() + " region=" + region);

        for (ProximityKitBeacon beacon : beacons) {
            Log.d(
                    TAG,
                    "I have a beacon with data: " + beacon + " attributes=" +
                            beacon.getAttributes()
                 );

            // We've wrapped up further behavior in some internal helper methods
            // Check their docs for details on additional things which you can do we beacon data
            displayBeacon(beacon);
        }
    }

    /***********************************************************************************************
     * END ProximityKitRangeNotifier
     **********************************************************************************************/

    /***********************************************************************************************
     * START ProximityKitMonitorNotifier
     **********************************************************************************************/

    @Override
    /**
     * Called when at least one beacon in a <code>ProximityKitBeaconRegion</code> is visible.
     *
     * @param region    an <code>ProximityKitBeaconRegion</code> which defines the criteria of
     *                  beacons being monitored
     */
    public void didEnterRegion(ProximityKitBeaconRegion region) {
        // In this example, this class sends a notification to the user whenever an beacon
        // matching a Region (defined above) are first seen.
        Log.d(
                TAG,
                "ENTER beacon region: " + region + " " +
                        region.getAttributes().get("welcomeMessage")
             );

        // Attempt to open the app now that we've entered a region if we started in the background
        tryAutoLaunch();

        // Notify the user that we've seen a beacon
        sendNotification(region);
    }

    @Override
    /**
     * Called when no more beacons in a <code>ProximityKitBeaconRegion</code> are visible.
     *
     * @param region    an <code>ProximityKitBeaconRegion</code> that defines the criteria of
     *                  beacons being monitored
     */
    public void didExitRegion(ProximityKitBeaconRegion region) {
        Log.d(TAG, "didExitRegion called with region: " + region);
    }

    @Override
    /**
     * Called when a the state of a <code>Region</code> changes.
     *
     * @param state     set to <code>ProximityKitMonitorNotifier.INSIDE</code> when at least one
     *                  beacon in a <code>ProximityKitBeaconRegion</code> is now visible; set to
     *                  <code>ProximityKitMonitorNotifier.OUTSIDE</code> when no more beacons in the
     *                  <code>ProximityKitBeaconRegion</code> are visible
     * @param region    an <code>ProximityKitBeaconRegion</code> that defines the criteria of
     *                  beacons being monitored
     */
    public void didDetermineStateForRegion(int state, ProximityKitBeaconRegion region) {
        Log.d(TAG, "didDeterineStateForRegion called with state: " + state + "\tregion: " + region);

        switch (state) {
            case ProximityKitMonitorNotifier.INSIDE:
                String welcomeMessage = region.getAttributes().get("welcomeMessage");
                if (welcomeMessage != null) {
                    Log.d(TAG, "Beacon " + region + " says: " + welcomeMessage);
                }
                break;
            case ProximityKitMonitorNotifier.OUTSIDE:
                String goodbyeMessage = region.getAttributes().get("goodbyeMessage");
                if (goodbyeMessage != null) {
                    Log.d(TAG, "Beacon " + region + " says: " + goodbyeMessage);
                }
                break;
            default:
                Log.d(TAG, "Received unknown state: " + state);
                break;
        }
    }

    /***********************************************************************************************
     * END ProximityKitMonitorNotifier
     **********************************************************************************************/

    /***********************************************************************************************
     * START ProximityKitGeofenceNotifier
     **********************************************************************************************/

    @Override
    /**
     * Called when a <code>Geofence</code> is visible.
     *
     * @param geofence  a <code>ProximityKitGeofenceRegion</code> that defines the criteria of
     *                  Geofence to look for
     */
    public void didEnterGeofence(ProximityKitGeofenceRegion region) {
        // In this example, this class sends a notification to the user whenever an beacon
        // matching a Region (defined above) are first seen.
        Log.d(
                TAG,
                "didEnterGeofenceRegion called with region: " + region + " " +
                        region.getAttributes().get("welcomeMessage")
             );

        // Attempt to open the app now that we've entered a region if we started in the background
        tryAutoLaunch();

        // Notify the user that we've seen a geofence
        sendNotification(region);

        // Force a sync if we enter the main office so we ensure we have the latest data.
        // We wouldn't want the boss to think we weren't working ┌( ಠ_ಠ)┘
        if (region.getAttributes().get("location") == MAIN_OFFICE_LOCATION) {
            forceSync();
        }
    }

    @Override
    /**
     * Called when a previously visible <code>Geofence</code> disappears.
     *
     * @param geofence  a <code>ProximityKitGeofenceRegion</code> that defines the criteria of
     *                  Geofence to look for
     */
    public void didExitGeofence(ProximityKitGeofenceRegion region) {
        Log.d(TAG, "didExitGeofenceRegion called with region: " + region);
    }

    @Override
    /**
     * Called when the device is cross a <code>Geofence</code> boundary.
     *
     * Called with a state value of <code>ProximityKitGeofenceNotifier.INSIDE</code> when the device
     * is completely within a <code>Geofence</code>.
     *
     * Called with a state value of <code>ProximityKitGeofenceNotifier.OUTSIDE</code> when the
     * device is no longer in a <code>Geofence</code>.
     *
     * @param state     either <code>ProximityKitGeofenceNotifier.INSIDE</code> or
     *                  <code>ProximityKitGeofenceNotifier.OUTSIDE</code>
     * @param geofence  the <code>ProximityKitGeofenceRegion</code> region this is event is
     *                  associated
     */
    public void didDetermineStateForGeofence(int state, ProximityKitGeofenceRegion region) {
        Log.d(
                TAG,
                "didDeterineStateForGeofence called with state: " + state + "\tregion: " + region
             );

        switch (state) {
            case ProximityKitGeofenceNotifier.INSIDE:
                String welcomeMessage = region.getAttributes().get("welcomeMessage");
                if (welcomeMessage != null) {
                    Log.d(TAG, "Geofence " + region + " says: " + welcomeMessage);
                }
                break;
            case ProximityKitGeofenceNotifier.OUTSIDE:
                String goodbyeMessage = region.getAttributes().get("goodbyeMessage");
                if (goodbyeMessage != null) {
                    Log.d(TAG, "Geofence " + region + " says: " + goodbyeMessage);
                }
                break;
            default:
                Log.d(TAG, "Received unknown state: " + state);
                break;
        }
    }

    /***********************************************************************************************
     * END ProximityKitGeofenceNotifier
     **********************************************************************************************/

    /***********************************************************************************************
     * START App Helpers
     **********************************************************************************************/

    /**
     * App helper method to notify an activity when we see a beacon.
     *
     * @param beacon
     *         <code>org.altbeacon.beacon.Beacon</code> instance of the
     *         beacon seen
     */
    private void displayBeacon(ProximityKitBeacon beacon) {
        if (mainActivity == null || beacon == null) {
            return;
        }

        // We could instead call beacon.toString() which wraps up the identifiers
        String displayString = beacon.getId1() + " " +
                beacon.getId2().toInt() + " " + beacon.getId3().toInt() +
                "\nWelcome message: " + beacon.getAttributes().get("welcomeMessage");

        // We've elected to notify our only view of the beacon and a message to display
        mainActivity.displayTableRow(beacon, displayString, true);
    }

    /**
     * App helper method to force Proximity Kit to sync.
     * <p/>
     * The Proximity Kit manager should automatically sync every hour, however, we can force an
     * ad-hoc sync anytime we want. This demonstrates how to do that.
     */
    private void forceSync() {
        Log.d(TAG, "Forcing a sync with the Proximity Kit server");
        pkManager.sync();
    }

    /**
     * Generate a consistent ID string given three identifier tokens.
     *
     * @param id1
     *         Identifier token 1
     * @param id2
     *         Identifier token 2
     * @param id3
     *         Identifier token 3
     * @return An ID string representing the three tokens.
     */
    private String generateId(Object id1, Object id2, Object id3) {
        return id1.toString() + "-" + id2 + "-" + id3;
    }

    /**
     * Send a notification stating a beacon is nearby.
     *
     * @param region
     *         The beacon region that was seen.
     */
    private void sendNotification(ProximityKitBeaconRegion region) {
        Log.d(TAG, "Sending notification.");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Proximity Kit Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Send a notification stating a geofence was entered.
     *
     * @param region
     *         Geofence which was entered.
     */
    private void sendNotification(ProximityKitGeofenceRegion region) {
        Log.d(TAG, "Sending notification.");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Proximity Kit Reference Application")
                        .setContentText("A geofence is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Attempt to launch the main activity if we were started in the background.
     */
    private void tryAutoLaunch() {
        if (haveDetectedBeaconsSinceBoot) {
            return;
        }

        // If we were started in the background for some reason
        Log.d(TAG, "auto launching MainActivity");

        // The very first time since boot that we detect an beacon, we launch the
        // MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // **IMPORTANT**: Make sure to add android:launchMode="singleInstance" in the manifest
        // to keep multiple copies of this activity from getting created if the user has
        // already manually launched the app.
        startActivity(intent);
        haveDetectedBeaconsSinceBoot = true;
    }

    /***********************************************************************************************
     * END App Helpers
     **********************************************************************************************/
}
