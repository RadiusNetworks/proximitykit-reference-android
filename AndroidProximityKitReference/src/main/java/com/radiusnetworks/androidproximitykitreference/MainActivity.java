package com.radiusnetworks.androidproximitykitreference;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconData;
import com.radiusnetworks.ibeacon.client.DataProviderException;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ActionBarActivity {
    public static final String TAG = "MainActivity";
    Map<String,TableRow> rowMap = new HashMap<String,TableRow>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((AndroidProximityKitReferenceApplication)getApplication()).setMainActivity(this);
    }

    public void displayTableRow(final IBeacon iBeacon, final String displayString, final boolean updateIfExists) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout table = (TableLayout) findViewById(R.id.beacon_table);
                String key = iBeacon.getProximityUuid() + "-" + iBeacon.getMajor() + "-" + iBeacon.getMinor();
                TableRow tr = (TableRow) rowMap.get(key);
                if (tr == null) {
                    tr = new TableRow(MainActivity.this);
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    rowMap.put(key, tr);
                    table.addView(tr);
                }
                else {
                    if (updateIfExists == false) {
                        return;
                    }
                }
                tr.removeAllViews();
                TextView textView=new TextView(MainActivity.this);
                textView.setText(displayString);
                tr.addView(textView);

            }
        });

    }
}
