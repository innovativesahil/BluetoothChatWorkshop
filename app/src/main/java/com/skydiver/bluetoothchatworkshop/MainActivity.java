package com.skydiver.bluetoothchatworkshop;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    //Tag for debugging info in Logcat
    private static final String TAG = "Main Activity";

    //Extra for intent
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    //Request codes for intents
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_DISCOVERY = 2;
    private static final int REQUEST_CONNECT_DEVICE = 3;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the Bluetooth connection
     */
    private BluetoothHandler mBluetoothHandler = null;

    private Button toggleBTButton;
    private ListView pairedDevicesList;

    ArrayList<String> arrayList = new ArrayList<>();
    ArrayAdapter<String> mNewDevicesArrayAdapter;
    Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Giving references to views
        pairedDevicesList = (ListView) findViewById(R.id.list_paired_devices);
        toggleBTButton = (Button) findViewById(R.id.button_toggle);

        toggleBTButton.setOnClickListener(mToggleClickListener);
        pairedDevicesList.setOnItemClickListener(mDeviceClickListener);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    View.OnClickListener mScanClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mBluetoothAdapter == null) {
                refreshButtonState();
                Toast.makeText(getApplicationContext(), "Bluetooth Device not available!", Toast.LENGTH_SHORT).show();
            } else if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
            loadPairedDevices();
            ensureDiscoverable();

        }
    };

    View.OnClickListener mToggleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toggleBluetooth();
        }
    };

    /**
     * The on-item-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            mBluetoothAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(intent);
        }
    };

    /**
     * Load the list of already paired devices
     */
    public void loadPairedDevices() {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);

        pairedDevicesList.setAdapter(pairedDevicesArrayAdapter);
        pairedDevicesArrayAdapter.clear();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (mBluetoothAdapter.getBondedDevices().size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.e(TAG, "Paired Device : " + device.getName() + " Address : " + device.getAddress());
                arrayList.add(device.getName() + "\n" + device.getAddress());
            }

        } else {
            pairedDevicesArrayAdapter.add("No Paired Devices");
            pairedDevicesArrayAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Toggle the state of bluetooth adapter on button click
     */
    private void toggleBluetooth() {
        if (mBluetoothAdapter == null) {
            refreshButtonState();
            Toast.makeText(getApplicationContext(), "Bluetooth Device not available!", Toast.LENGTH_SHORT).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            refreshButtonState();
            Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Refresh the button state if bluetooth state is changed
     */
    private void refreshButtonState() {
        Log.e(TAG, "Button State Refreshed");
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                toggleBTButton.setText("Turn Bluetooth Off");
            } else if (!mBluetoothAdapter.isEnabled()) {
                toggleBTButton.setText("Turn Bluetooth On");
            }
        } else {
            toggleBTButton.setText("Bluetooth adapter unavailable");
            toggleBTButton.setEnabled(false);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    refreshButtonState();
                    Toast.makeText(getApplicationContext(), "Bluetooth enabled Successfully", Toast.LENGTH_SHORT).show();
                    loadPairedDevices();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Action cancelled by User!", Toast.LENGTH_SHORT).show();
                    refreshButtonState();
                } else {
                    refreshButtonState();
                    Toast.makeText(getApplicationContext(), "Bluetooth couldn't be enabled!", Toast.LENGTH_SHORT).show();
                }
            case REQUEST_ENABLE_DISCOVERY:
                if (resultCode == RESULT_CANCELED)
                    Toast.makeText(getApplicationContext(), "Discovery Started", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_accept_connection:

                return true;
            case R.id.menu_bluetooth_settings:
                return true;
            case R.id.menu_about:
                return true;
            case R.id.menu_help:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
