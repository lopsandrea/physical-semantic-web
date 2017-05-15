# Physical Semantic Web Android client

The Physical Semantic Web (PSW) mobile application extends the basic Android client (developed by Google and available on the [Google Play store](https://play.google.com/store/apps/details?id=physical_web.org.physicalweb)) implementing  the enhancements proposed in the PSW vision.

The Physical Semantic Web client supports the following tasks:

- enable PSW features through the *Settings* panel. If PSW is not active, the client only detects basic Physical Web (PW) beacons;
- compose an OWL-based request for beacon discovery exploiting the **OWLEditor** library. 
- detect both PW and PSW beacons. Nearby beacons are showed as *remote* (URL, PSW-URL) or *local* (FatBeacon, PSW-UID) devices. In the last case, also the type of local connection is reported (Wi-Fi or BLE Wireless Network);
- semantic-based ranking. As described in [[Ruta *et al.*, SAC 2017]](http://sisinflab.poliba.it/publications/2017/RSILGPD17/), beacons are ranked with respect to the user request by means of an utility function. A coloured progress bar indicates the obtained rank value in an easily understandable way;
- view data shared by beacons. Selecting a beacon, the user can open in the browser the related web page or (only for PSW devices) show the exposed OWL annotation.
- share PSW beacons. A new option was introduced into the *Demo* panel to advertise PSW-UID beacons and expose an OWL annotation selected by the user.