package com.skydiver.bluetoothchatworkshop;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectedActivity extends Activity {

    BluetoothAdapter mBluetoothAdapter;
    EditText editData;
    Button sendButton;
    ListView transferredDataView;
    TextView connectedDevice;
    ArrayAdapter<String> arrayAdapter;
    BluetoothHandler myBluetoothHandler;
    private MediaPlayer mSoundHorn;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    String len, breadth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        init();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (myBluetoothHandler == null) {
            setupChat();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBluetoothHandler != null) {
            myBluetoothHandler.stop();
        }
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (myBluetoothHandler != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (myBluetoothHandler.getState() == BluetoothHandler.STATE_NONE) {
                // Start the Bluetooth chat services
                myBluetoothHandler.start();
                connectDevice();

            } else if (myBluetoothHandler.getState() == BluetoothHandler.STATE_CONNECTED) {

            }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {

        // Initialize the array adapter for the conversation thread
        arrayAdapter = new ArrayAdapter<>(this, R.layout.list_items_data_transferred);

        transferredDataView.setAdapter(arrayAdapter);

        // Initialize the compose field with a listener for the return key
        editData.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                sendButton.setEnabled(false);
//                if (null != editData) {
//                    String message = editData.getText().toString();
//                    sendMessage(message);
//                }
            }
        });
        // Initialize the BluetoothChatService to perform bluetooth connections
        myBluetoothHandler = new BluetoothHandler(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (myBluetoothHandler.getState() != BluetoothHandler.STATE_CONNECTED) {
            Toast.makeText(getBaseContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = (message + "\n").getBytes();
            myBluetoothHandler.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            editData.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("inReciever", "Disconnected from" + device.getName());
                Toast.makeText(getBaseContext(), "Disconnected from " + device.getName(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothHandler.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            arrayAdapter.clear();
                            break;
                        case BluetoothHandler.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothHandler.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            finish();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    arrayAdapter.add("Me:  " + writeMessage);
                    transferredDataView.smoothScrollToPosition(arrayAdapter.getCount() - 1);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.e("ConstructingString", "Message = " + readMessage);
                    arrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    transferredDataView.smoothScrollToPosition(arrayAdapter.getCount() - 1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getBaseContext()) {
                        Toast.makeText(getBaseContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getBaseContext()) {
                        Toast.makeText(getBaseContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
            }
        }
    };


    private void soundHorn() {
        Thread hornThread = new Thread() {
            @Override
            public void run() {
                mSoundHorn.start();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("in soundHorn", "exception occured");
                }
            }
        };
        hornThread.start();
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */

    private void setStatus(CharSequence subTitle) {
        ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    /**
     * Establish connection with other divice
     */
    private void connectDevice() {
        // Get the device MAC address
        String address = getIntent().getExtras()
                .getString(MainActivity.EXTRA_DEVICE_ADDRESS);
//        boolean connectionType = getIntent().getExtras().getBoolean(MainActivity.EXTRA_CONNECT_TYPE);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        myBluetoothHandler.connect(device);
    }

    private void init() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sendButton = findViewById(R.id.button_send);
        editData = findViewById(R.id.data_sent);
        transferredDataView = findViewById(R.id.data_transferred);

    }

}