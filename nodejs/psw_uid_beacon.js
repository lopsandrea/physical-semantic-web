var eddystoneBeacon = require('eddystone-beacon');
var bleno = require('bleno');
var webpageCharacteristic = require('./webpageCharacteristic');
var fs = require('fs');

var SERVICE_UUID = 'ae5946d4-e587-4ba8-b6a5-a97cca6affd3';

var MAX_URL_LENGTH = 18;
var ADVERTISING_HEADER_UUID = 'feaa';
var ATT_OP_MTU_RESP = 0x03;  // Refer to bleno/lib/hci-socket/gatt for doc
var MIN_MTU = 23;
var MAX_MTU = 505;

var Gatt = require('bleno/lib/hci-socket/gatt');
var Hci = require('bleno/lib/hci-socket/hci');
var exec = require('child_process').exec;

/**
 * This allows us to change the negotiate the MTU size from 0 - 505. We have
 * set REQUESTING_MTU to 505 for maximum transfer rate.
 */
Gatt.prototype.handleMtuRequest = function(request) {
  var mtu = request.readUInt16LE(1);
 
  if (mtu < MIN_MTU) {
    mtu = MIN_MTU;
  } else if (mtu > MAX_MTU) {
    mtu = MAX_MTU;
  }

  this._mtu = mtu;

  this.emit('mtuChange', this._mtu);

  var response = Buffer.alloc(3);

  response.writeUInt8(ATT_OP_MTU_RESP, 0);
  response.writeUInt16LE(mtu, 1);

  return response;
};

/**
 * Change the connection interval through commandline tool hcileup. Specs,
 * allow for a 7.5ms connection interval. Note, there is a 1.25 multiplier
 * on the min and max values.
 */
Hci.changeConnectionInterval = function(handle) {
  var min = 6;  // Will be multiplied by 1.25.
  var max = 7;  // Will be multiplied by 1.25.
  var latency = 0;
  var timeout = 500;
  
  fs.readFile("./owl-file.owl", function(err, data) {
	if (err) {
	  throw err;
	}
		
	console.log("PSW-UID data updated!");	
	characteristic.onWriteRequest(data, 0, null, null);
  });

  var cmd = `hcitool lecup --handle ${handle} --min ${min} --max ${max} ` +
            `--latency ${latency} --timeout ${timeout}`;

  console.log(cmd);

  exec(cmd, function(error, stdout, stderr) {
    if(stdout !== null) {
      console.log('stdout:\t' + stdout );
    }
    if(stderr !== null) {
      console.log('stderr:\t' + stderr);
    }
    if(error !== null) {
      console.log('ERROR:\t' + error);
    }
  });
};

/**
 * Does the standard processLeConnComplete library behavior, but calls our
 * custom changeConnectionInterval function to optimize CI.
 */
Hci.prototype.processLeConnComplete = function(status, data) {
  var handle = data.readUInt16LE(0);
  var role = data.readUInt8(2);
  var addressType = data.readUInt8(3) === 0x01 ? 'random': 'public';
  var address = data.slice(4, 10).toString('hex').match(/.{1,2}/g).reverse().join(':');
  var interval = data.readUInt16LE(10) * 1.25;
  var latency = data.readUInt16LE(12); // TODO: multiplier?
  var supervisionTimeout = data.readUInt16LE(14) * 10;
  var masterClockAccuracy = data.readUInt8(16); // TODO: multiplier?

  Hci.changeConnectionInterval(handle);

  this.emit('leConnComplete', status, handle, role, addressType, address,
             interval, latency, supervisionTimeout, masterClockAccuracy);
};

/*********************************************************/

var characteristic = new webpageCharacteristic();

fs.readFile("./owl-file.owl", function(err, data) {
  if (err) {
    throw err;
  }

  characteristic.onWriteRequest(data, 0, null, null);
});

var service = new bleno.PrimaryService({
  uuid: SERVICE_UUID,
  characteristics: [
    characteristic
  ]
});

bleno.once('advertisingStart', function(err) {
  
  if (err) {
    throw err;
  }

  console.log('on - advertisingStart');
  bleno.setServices([
    service
  ]);
});

/*********************************************************/

var options = {
  name: 'Beacon',    // set device name when advertising (Linux only)
  txPowerLevel: -22, // override TX Power Level, default value is -21,
  tlmCount: 2,       // 2 TLM frames
  tlmPeriod: 10      // every 10 advertisements
};

var namespaceId = '00000001000000000000'; // ONTO_ID (4B) + Resource_ID (6B) 
var instanceId = '001122334455'; // MAC Address (6B) [00:11:22:33:44:55]
eddystoneBeacon.advertisePswUid(namespaceId, instanceId, [options]);

console.log("Eddystone PSW Uid-Beacon published!");