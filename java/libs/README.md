# Physical (Semantic) Web Collection library for java

This java library contains data structures and convenience utilities for
storing metadata related to devices that broadcast URLs.  This library is
intended to help bootstrap new Physical Web clients written in java.

## Physical Semantic Web features

*Physical Web Collection* library was extended by means of an additional package **it.poliba.sisinflab.psw** mainly including: 


- *PswEddystoneBeacon*, extending the *EddystoneBeacon* class and modeling novel attributes (new frame type codes and message format) needed to identify and manage PSW devices; 
- *UidEddystoneBeacon*, to support [Eddystone-UID](https://github.com/google/eddystone/tree/master/eddystone-uid) frames not included in the basic implementation, only supporting [Eddystone-URL](https://github.com/google/eddystone/tree/master/eddystone-url) beacons.

## Usage

The Physical Web Collection library was developed as a *gradle* module in order to be easily integrated as dependency within a Java project following these steps:

- Clone or download the repository
- Edit the *settings.gradle* file to include the Physical Web Collection library into your gradle project

```
include ':libs'

project(':libs').projectDir = new File('<path>/java/libs')
```

- Edit the *build.gradle* file to set the library as project *dependency*

```
dependencies {
	
	// other project dependencies
	
    compile(project(':libs'))
}
```

## Basic examples

### PSW-URL Eddystone beacon

Get a PSW-URL Eddystone beacon from retrieved BLE service data

```java
/**
   * Parses the service data for URLs or URIs.
   * @param urlServiceData The ble advertised Eddystone URL Service UUID service data
   * @param uriServiceData The ble advertised URI Beacon Service UUID service data
   * @return EddystoneBeacon with flags, tx Power level and url parsed from the service data
*/

PswEddystoneBeacon b = EddystoneBeacon.parseFromServiceData(urlServiceData, uriServiceData);
```

### PSW-UID Eddystone beacon

Get a PSW-URL Eddystone beacon from retrieved BLE service data

```java
/**
   * Parses the service data.
   * @param serviceData The ble advertised Eddystone UID Service UUID service data
   * @return UidEddystoneBeacon with flags, tx Power level, ontology ID, instance ID and MAC address of the device exposing the resource parsed from the service data
*/

UidEddystoneBeacon b = PswEddystoneBeacon.parseUidFromServiceData(serviceData);

