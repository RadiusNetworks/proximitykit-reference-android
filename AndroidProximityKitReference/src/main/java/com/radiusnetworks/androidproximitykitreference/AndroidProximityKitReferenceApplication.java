package com.radiusnetworks.androidproximitykitreference;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.proximity.ProximityKitManager;
import com.radiusnetworks.proximity.ProximityKitNotifier;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.client.DataProviderException;

import java.util.Date;

/**
 * Created by dyoung on 4/2/14.
 */
public class AndroidProximityKitReferenceApplication extends Application implements ProximityKitNotifier {
    public static final String TAG = "ProximityKitReferenceApplication";
    private boolean haveDetectedIBeaconsSinceBoot = false;
    private MainActivity mainActivity = null;
    Date lastRefreshTime = new Date();

    @Override
    public void onCreate() {
        super.onCreate();
        ProximityKitManager pkManager = ProximityKitManager.getInstanceForApplication(this);
        pkManager.setNotifier(this);
        pkManager.start();

    }

    @Override
    public void didSync() {

    }

    @Override
    public void didFailSync(Exception e) {

    }

    @Override
    public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData iBeaconData, DataProviderException e) {
        if (e != null) {
            Log.d(TAG, "data fetch error:" + e);
        }
        if (iBeaconData != null) {
            Log.d(TAG, "I have an iBeacon with data: uuid="+iBeacon.getProximityUuid()+" major="+iBeacon.getMajor()+" minor="+iBeacon.getMinor()+" welcomeMessage="+iBeaconData.get("welcomeMessage"));
            String displayString = iBeacon.getProximityUuid()+" "+iBeacon.getMajor()+" "+iBeacon.getMinor()+"\n"+"Welcome message:"+iBeaconData.get("welcomeMessage");
            if (mainActivity != null) {
                mainActivity.displayTableRow(iBeacon, displayString, true);
            }
            if (new Date().getTime() - lastRefreshTime.getTime() > 24*60*60000l) { /* one day */
                Log.d(TAG, "It is time to force a refresh of iBeacon data");
                iBeaconData.sync(this);
                lastRefreshTime = new Date();
            }
        }
    }


    @Override
    public void didEnterRegion(Region region) {
        // In this example, this class sends a notification to the user whenever an iBeacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region.");
        if (!haveDetectedIBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an iBeacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedIBeaconsSinceBoot = true;
        } else {
            // If we have already seen iBeacons and launched the MainActivity before, we simply
            // send a notification to the user on subsequent detections.
            Log.d(TAG, "Sending notification.");
            sendNotification();
        }
    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Proximity Kit Reference Application")
                        .setContentText("An iBeacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


}
