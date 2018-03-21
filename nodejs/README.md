PSW-UID beacon on Raspberry Pi
=========================

This is a nodejs implementation of a PSW-UID beacon developed on a Raspberry Pi 3.
It will advertise a connectable PSW-UID beacon over Bluetooth Low Energy (BLE). 
When the central (client) connects to the peripheral (PSW-UID/server), the
central will attempt to read the requested OWL annotation.

Instructions for Raspbian:
--------------------------
1. Clone repository onto Pi. If you're using a Pi2 or earlier, make sure you
have a bluetooth chip installed.

2. Make sure you have nodejs

   ```$ node -v```

If not, then run

    ```$ sudo apt install nodejs```

3. For bleno make sure bluetooth, bluez, libbluetooth-dev, and libudev-dev 
   are installed

   ```$ sudo apt-get install bluetooth bluez libbluetooth-dev libudev-dev```

4. Navigate to the repository directory and copy on the same folder the [psw-node-eddystone-beacon](https://github.com/sisinflab-swot/psw-node-eddystone-beacon) library.

5. Run

    ```$ sudo npm install psw-node-eddystone-beacon```

This should download all required nodejs libraries.
5. To run

    ```$ sudo node psw_uid_beacon.js```
