package it.poliba.sisinflab.psw.beacon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;
import java.util.UUID;

import it.poliba.sisinflab.psw.beacon.ble.PswUidBroadcastService;
import it.poliba.sisinflab.psw.beacon.owl.KBManager;

public class BeaconingActivity extends Activity {
    private static final String TAG = BeaconingActivity.class.getSimpleName();

    private static int DISPLAY_WAIT = 3000;

    private AlphanumericDisplay mSegmentDisplay;
    private Apa102 mLedstrip;

    private Gpio mLedGpioA;
    private Gpio mLedGpioB;
    private Gpio mLedGpioC;

    private Button mButtonA;
    private Button mButtonB;
    private Button mButtonC;

    int tmpLevel = 0;
    int hmdLevel = 0;
    int lumLevel = 0;

    private KBManager mng;
    private BluetoothManager mBluetoothManager;
    private String BT_MAC_ADDRESS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {

            // init KBManager
            mng = new KBManager(getResources().openRawResource(R.raw.mountadam_pinot_noir));
            //Log.i(TAG, mng.getOWL());

            // check BLE support and start beaconing
            checkBLE();

            // init LED display
            setupLedStrip();

            // init LEDs
            mLedGpioA = RainbowHat.openLedRed();
            mLedGpioB = RainbowHat.openLedGreen();
            mLedGpioC = RainbowHat.openLedBlue();

            // init Buttons
            mButtonA = RainbowHat.openButtonA();
            mButtonA.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    Log.d(TAG, "button A pressed:" + pressed);
                    if (pressed) {
                        tmpLevel = (tmpLevel + 1) % 3;
                        mng.setPropertyValue(KBManager.TEMPERATURE_PROPERTY, KBManager.TEMPERATURE_VALUES[tmpLevel]);
                        updateDisplayLevel(mLedGpioB, "TEMP", tmpLevel);

                        //Log.i(TAG, mng.getOWL());
                        restartBeaconing();
                    }
                }
            });

            mButtonB = RainbowHat.openButtonB();
            mButtonB.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    Log.d(TAG, "button B pressed:" + pressed);
                    if (pressed) {
                        hmdLevel = (hmdLevel + 1) % 3;
                        mng.setPropertyValue(KBManager.HUMIDITY_PROPERTY, KBManager.HUMIDITY_VALUES[hmdLevel]);
                        updateDisplayLevel(mLedGpioB, "HUM", hmdLevel);

                        //Log.i(TAG, mng.getOWL());
                        restartBeaconing();
                    }
                }
            });

            mButtonC = RainbowHat.openButtonC();
            mButtonC.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    Log.d(TAG, "button C pressed:" + pressed);
                    if (pressed) {
                        lumLevel = (lumLevel + 1) % 3;
                        mng.setPropertyValue(KBManager.LIGHT_PROPERTY, KBManager.LIGHT_VALUES[lumLevel]);
                        updateDisplayLevel(mLedGpioB, "LUM", lumLevel);

                        //Log.i(TAG, mng.getOWL());
                        restartBeaconing();
                    }
                }
            });

            // init Alphanumeric Display
            setupAlphanumericDisplay();

            // welcome message!
            setLongString("PSW DEMO AT WWW 2018");

        }catch (Exception e) {
            Log.e(TAG, "Error onCreate()", e);
        }
    }

    private void updateDisplayLevel(Gpio led, String txt, int level) {
        try {
            led.setValue(true);
            setLedLevel(level);
            setupAlphanumericDisplay(txt, true);
            led.setValue(false);
        } catch (Exception e) {
            Log.e(TAG, "Error during updateDisplay()", e);
        }
    }

    private void setLedLevel(int level) {
        if (level == 2) {
            setColorLedStrip(Color.HSVToColor(220, new float[]{0, 1.0f, 1.0f}), 7);
            setupAlphanumericDisplay("HIGH", 1500, false);
        } else if (level == 1) {
            setColorLedStrip(Color.HSVToColor(235, new float[]{50, 0.98f, 1.0f}), 4);
            setupAlphanumericDisplay("MED", 1500, false);
        } else if (level == 0) {
            setColorLedStrip(Color.HSVToColor(235, new float[]{100, 0.95f, 1.0f}), 2);
            setupAlphanumericDisplay("LOW", 1500, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mButtonA.close();
            mButtonB.close();
            mButtonC.close();

            mLedGpioA.close();
            mLedGpioB.close();
            mLedGpioC.close();
        } catch (Exception e) {
            Log.e(TAG, "Error onDestroy()", e);
        }

        destroyAlphanumericDisplay();
        destroyLedStrip();
    }

    private void checkBLE() {
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        String address = bluetoothAdapter.getAddress();
        BT_MAC_ADDRESS = address.replace(":","");

        // Register for system Bluetooth events
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
            startBeaconing();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startBeaconing();
        }
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    private void updateDisplay(Gpio led, String txt) {
        try {
            led.setValue(true);
            setLedLevel(txt);
            setupAlphanumericDisplay(txt, false);
            led.setValue(false);
            setupAlphanumericDisplay("OK", true);
        } catch (Exception e) {
            Log.e(TAG, "Error during updateDisplay()", e);
        }
    }

    private String getLabel(int value, String feature) {
        switch (value) {
            case 0:
                return "LO" + feature;
            case 1:
                return "MD" + feature;
            case 2:
                return "HI" + feature;
            default:
                return "ERR";
        }
    }

    class TurnOffDisplay extends Thread {
        long ms;
        boolean led;

        TurnOffDisplay(long ms, boolean led) {
            this.ms = ms;
            this.led = led;
        }

        public void run() {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            turnOffAlphanumericDisplay();
            if (led)
                turnOffLedStrip();
        }
    }

    private void setLongString(String text) {
        if (text.length()>4) {
            for(int i=0; i<text.length()-3; i++) {
                String display = text.substring(i, i+4);
                if (i==0)
                    setupAlphanumericDisplay(display, 1500, false);
                else if (i==text.length()-4)
                    setupAlphanumericDisplay(display, 2000, true);
                else
                    setupAlphanumericDisplay(display, 500, false);
            }
        } else
            setupAlphanumericDisplay(text, true);
    }

    private void setupAlphanumericDisplay() {
        try {
            mSegmentDisplay = RainbowHat.openDisplay();
            mSegmentDisplay.setBrightness(1.0f);
            mSegmentDisplay.setEnabled(true);
            mSegmentDisplay.clear();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring display", e);
        }
    }

    private void setupAlphanumericDisplay(String text, int ms, boolean led) {
        if (mSegmentDisplay != null) {
            try {
                mSegmentDisplay.clear();
                mSegmentDisplay.display(text);
                new TurnOffDisplay(ms, led).run();
            } catch (IOException e) {
                Log.e(TAG, "Error configuring display", e);
            }
        }
    }

    private void setupAlphanumericDisplay(String text, boolean led) {
        setupAlphanumericDisplay(text, DISPLAY_WAIT, led);
    }

    private void turnOffAlphanumericDisplay() {
        if (mSegmentDisplay != null) {
            try {
                mSegmentDisplay.clear();
            } catch (IOException e) {
                Log.e(TAG, "Error configuring display", e);
            }
        }
    }

    private void destroyAlphanumericDisplay() {
        if (mSegmentDisplay != null) {
            Log.i(TAG, "Closing display");
            try {
                turnOffAlphanumericDisplay();
                mSegmentDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing display", e);
            } finally {
                mSegmentDisplay = null;
            }
        }
    }

    private void setupLedStrip() {
        try {
            mLedstrip = RainbowHat.openLedStrip();
            mLedstrip.setBrightness(2);
            mLedstrip.setDirection(Apa102.Direction.REVERSED);
            int[] rainbow = new int[RainbowHat.LEDSTRIP_LENGTH];
            for (int i = 0; i < rainbow.length; i++) {
                rainbow[i] = Color.HSVToColor(240, new float[]{i * 360.f / rainbow.length, 1.0f, 1.0f});
            }
            mLedstrip.write(rainbow);
        } catch (Exception e) {
            Log.e(TAG, "Exception during setupLedStrip()", e);
        }
    }

    private void setLedLevel(String txt) {
        if (txt.startsWith("HI"))
            setColorLedStrip(Color.HSVToColor(220, new float[]{0, 1.0f, 1.0f}), 7);
        else if (txt.startsWith("MD"))
            setColorLedStrip(Color.HSVToColor(235, new float[]{50, 0.98f, 1.0f}), 4);
        else if (txt.startsWith("LO"))
            setColorLedStrip(Color.HSVToColor(235, new float[]{100, 0.95f, 1.0f}), 2);
    }

    private void setColorLedStrip(int color, int maxLed) {
        try {
            int[] rainbow = new int[RainbowHat.LEDSTRIP_LENGTH];
            for (int i = 0; i < rainbow.length; i++) {
                if (i < maxLed)
                    rainbow[i] = color;
                else
                    rainbow[i] = 0;
            }
            mLedstrip.write(rainbow);
        } catch (Exception e) {
            Log.e(TAG, "Exception during setColorLedStrip()", e);
        }
    }

    private void turnOffLedStrip() {
        if (mLedstrip != null) {
            try {
                int[] rainbow = new int[RainbowHat.LEDSTRIP_LENGTH];
                for (int i = 0; i < rainbow.length; i++) {
                    rainbow[i] = 0;
                }
                mLedstrip.write(rainbow);
            } catch (IOException e) {
                Log.e(TAG, "Exception closing LED strip", e);
            }
        }
    }

    private void destroyLedStrip() {
        if (mLedstrip != null) {
            try {
                turnOffLedStrip();
                mLedstrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception closing LED strip", e);
            } finally {
                mLedstrip = null;
            }
        }
    }

    private void startBeaconing() {
        Intent intent = new Intent(getBaseContext(), PswUidBroadcastService.class);
        intent.putExtra(PswUidBroadcastService.TITLE_KEY, mng.getResourceIRI().getFragment());
        intent.putExtra(PswUidBroadcastService.DATA_KEY, mng.getOWL().getBytes());

        intent.putExtra(PswUidBroadcastService.MAC_KEY, BT_MAC_ADDRESS);

        String ontology = mng.getOntologyIRI().toString();
        String onto_id = UUID.nameUUIDFromBytes(ontology.getBytes()).toString().substring(0, 4);
        intent.putExtra(PswUidBroadcastService.ONTO_KEY, onto_id);

        String instance = mng.getResourceIRI().getFragment();
        String instance_id = UUID.nameUUIDFromBytes(instance.getBytes()).toString().substring(0, 6);
        intent.putExtra(PswUidBroadcastService.INSTANCE_KEY, instance_id);

        getBaseContext().startService(intent);
    }

    private void stopBeaconing() {
        Intent intent = new Intent(getBaseContext(), PswUidBroadcastService.class);
        getBaseContext().stopService(intent);
    }

    private void restartBeaconing() {
        stopBeaconing();
        startBeaconing();
        Log.d(TAG, "Restart Beaconing!");
    }

}
