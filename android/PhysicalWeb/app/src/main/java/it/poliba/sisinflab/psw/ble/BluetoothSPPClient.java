package it.poliba.sisinflab.psw.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSPPClient extends AsyncTask<String, Void, String> {

    private OnTaskCompleted mListener;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private BluetoothAdapter mBluetoothAdapter;

    private String filename;
    private String url;

    private String TAG = BluetoothSPPClient.class.getName();

    public interface OnTaskCompleted {
        void onTaskCompleted(String receivedOWL, String filename, String url);
    }

    public BluetoothSPPClient(OnTaskCompleted listener) {
        mListener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        String mac = params[0];
        String cmd = params[1];
        this.filename = params[2];
        this.url = params[3];

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mmDevice = mBluetoothAdapter.getRemoteDevice(mac);
        mmDevice.fetchUuidsWithSdp();

        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }

        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return null;
        }

        return manageMyConnectedSocket(cmd);
    }

    // called after doInBackground finishes
    protected void onPostExecute(String result) {
        this.cancel();
        mListener.onTaskCompleted(result, filename, url);
    }

    private String manageMyConnectedSocket(String cmd) {

        OutputStream mmOutStream = null;
        InputStream mmInStream = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            mmInStream = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }

        try {
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
            return null;
        }

        try {
            cmd = cmd + "\n";
            mmOutStream.write(cmd.getBytes());
            mmOutStream.flush();
        } catch (IOException e) {
            Log.e(TAG,"Couldn't send data to the other device");
        }

        String owl = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(mmInStream));
        try {
            String line = br.readLine();
            while(line != null && !line.startsWith("$")) {
                owl = owl + line + "\n";
                //Log.d(TAG, line);
                line = br.readLine();
            }
        } catch (IOException e) {
            Log.d(TAG, "Input stream was disconnected", e);
            e.printStackTrace();
        }

        //Log.d(TAG, "OWL file received!");
        return owl;
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
