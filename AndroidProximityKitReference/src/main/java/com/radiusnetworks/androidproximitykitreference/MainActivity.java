package com.radiusnetworks.androidproximitykitreference;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.radiusnetworks.proximity.ProximityKitBeacon;
import com.radiusnetworks.proximity.ProximityKitGeofenceRegion;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ActionBarActivity {
    public static final String TAG = "MainActivity";
    Map<String, TableRow> rowMap = new HashMap<String, TableRow>();
    public static boolean isRunning = false;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((AndroidProximityKitReferenceApplication) getApplication()).setMainActivity(this);
        if (isRunning) {
            startManager();
        } else {
            stopManager();
        }
        //for Version 19 comment out from ==================000 HERE 000==================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage(
                        "Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @TargetApi(23)
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                requestPermissions(
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSION_REQUEST_FINE_LOCATION
                                );
                            }
                        }
                );
                builder.show();
            }
        }
        // to ==================000 HERE 000==================
    }

    //For Version 19 comment out from ==================000 HERE 000==================
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }
    //to ==================000 HERE 000==================



    public void displayTableRow(final ProximityKitBeacon beacon, final String displayString, final boolean updateIfExists) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout table = (TableLayout) findViewById(R.id.beacon_table);
                // You could instead call beacon.toString() which includes the identifiers
                String key = beacon.getId1().toString() + "-" +
                        beacon.getId2().toInt() + "-" + beacon.getId3().toInt();
                TableRow tr = (TableRow) rowMap.get(key);
                if (tr == null) {
                    tr = new TableRow(MainActivity.this);
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    rowMap.put(key, tr);
                    table.addView(tr);
                } else {
                    if (updateIfExists == false) {
                        return;
                    }
                }
                tr.removeAllViews();
                TextView textView = new TextView(MainActivity.this);
                textView.setText(displayString);
                tr.addView(textView);
            }
        });
    }
    public void displayTableRow(final ProximityKitGeofenceRegion geofence, final String displayString, final boolean updateIfExists) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout table = (TableLayout) findViewById(R.id.beacon_table);

                String key = geofence.getRequestId();
                TableRow tr = (TableRow) rowMap.get(key);
                if (tr == null) {
                    tr = new TableRow(MainActivity.this);
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    rowMap.put(key, tr);
                    table.addView(tr);
                } else {
                    if (updateIfExists == false) {
                        return;
                    }
                }
                tr.removeAllViews();
                TextView textView = new TextView(MainActivity.this);
                textView.setText(displayString);
                tr.addView(textView);
            }
        });
    }

    /**
     * Button action which turns the Proximity Kit manager service on and off.
     *
     * @param view  button object which was pressed
     */
    public void toggleManager(View view) {
        if (view.getId() != R.id.manager_toggle) { return; }

        if (isRunning) {
            stopManager();
            isRunning = false;
        } else {
            startManager();
            isRunning = true;
        }
    }

    /**
     * Turn the Proximity Kit manager on and update the UI accordingly.
     */
    private void startManager() {
        AndroidProximityKitReferenceApplication app = (AndroidProximityKitReferenceApplication) getApplication();
        Button btn = (Button) findViewById(R.id.manager_toggle);

        app.startManager();
        btn.setText(R.string.manager_toggle_stop);
    }

    /**
     * Turn the Proximity Kit manager off and update the UI accordingly.
     */
    private void stopManager() {
        AndroidProximityKitReferenceApplication app = (AndroidProximityKitReferenceApplication) getApplication();
        TableLayout table = (TableLayout) findViewById(R.id.beacon_table);
        Button btn = (Button) findViewById(R.id.manager_toggle);

        app.stopManager();
        table.removeAllViews();
        rowMap.clear();
        btn.setText(R.string.manager_toggle_start);
    }
}
