# The Physical Semantic Web

<img align="left" src="http://sisinflab.poliba.it/swottools/physicalweb/img/spw.png" hspace="15" width="70px" style="float: left">

Semantic Web technologies have been acknowledged as tools to promote interoperability and intelligent information processing in ubiquitous computing. The **Semantic Web** and the **Internet of Things** paradigms are even more converging toward the so-called **Semantic Web of Things** (SWoT) [[Ruta *et al.*, ICSC 2012]](http://sisinflab.poliba.it/publications/2012/RSD12/) enabling semantic-enhanced pervasive computing by embedding intelligence into ordinary objects and environments through a large number of heterogeneous micro-devices, each conveying a small amount of information.	

The proposed project aims to demonstrate how *encapsulate semantic annotations* in beacons, to enhance features and functionalities offered by objects in a Physical Web. Reference scenarios could be so extended to enable both *human-to-machine* and *machine-to-machine* interactions: objects become resources exposing a semantic annotation used to characterize themselves without depending on a centralized infrastructure.

## What is the *Physical Web*?

The [Physical Web](https://google.github.io/physical-web/) is an approach to unleash the core superpower of the web: interaction on demand. People should be able to walk up to any smart device - a vending machine, a poster, a toy, a bus stop, a rental car - and not have to download an app first. Everything should be just a tap away. 

### How does it work? 
A small utility on the phone scans for URLs that are nearby. [Eddystone-URL](https://github.com/google/eddystone) Bluetooth beacon format is used to find nearby URLs without requiring any centralized archive. Finding URLs through Wi-Fi using mDNS and uPnP is also supported.	

### How does this change things? 
The Physical Web isn't about replacing native apps: it's about enabling interaction when native apps just aren't practical. Once any smart device can have a web address, the overhead of a dedicated app is no longer required for simple interactions.

## What is the *Physical Semantic Web*?

User agents running on mobile personal devices should be able to *dynamically discover* the best available resources according to user's profile and preferences. Not simply a resource in the surroundings of the user, but the one better supporting her current tasks through *unobtrusive and context-dependent* suggestion.

### Semantic-enhanced Resource Discovery
Our proposal aims to extend the Physical Web project exploiting **Semantic Web** technologies, enabling advanced resource discovery features. Particularly, the proposed approach (maintaining the reference URI-based mechanism) allows detecting all Eddystone-URL beacons in a given environment. Each URL could target:

- a classic web page (basic scenario). Users can access the document via a web browser;
- an annotated web page. Users may either view the web page or exploit the features based on semantics in the metadata;
- a semantic annotation. User agents can get and use it.

### How can I rank retrieved resources? 
Retrieved annotations can be used to perform a **semantic-based matchmaking** [[Scioscia *et al.*, IJSWIS 2014]](http://sisinflab.poliba.it/publications/2014/SRLGIPD14/) between a request (e.g., *user profile*) and multiple resources (i.e., *objects descriptions*). 

In the proposed solution, the user profile consists of a concept expression including information like interests and hobbies. The profile can be either manually composed through a GUI, or through speech-based interaction, or automatically generated by a profiler crawler running on the user's device.

Several resource domains (cultural heritage, shopping, accommodation, transportation, points of interest) can be explored by simply selecting the proper ontology. In the same way, semantic annotations referred to object features will include concept expression referred to common domain ontologies. 

### Ranking and results Refinement
For each pair <user profile, resource>, a score value is calculated to assess the similarity between the user profile and the beacon.	Exploiting non-standard inference, a full explanation about the score is given to the user evidencing which compatible and/or missing features of the resource determine the result. Analogously, in case of incompatibility profile/resource, semantic-based services are used to detect conflicting properties and elements of the beacons annotations.

Finally, detected beacons and related rank are displayed in an ordered list. Each beacon could also specify a link to a webpage associated to an element of the list. Specific user preferences can be also taken into account by implementing the following features:

- History: to store URLs visited by the user;
- Favorites: to create a bookmark for user favorite URLs;
- Spam: to mark a URL as spam.

## Contents
* [Physical Web webpage](https://google.github.io/physical-web/) - Official webpage with info and useful links about the Physical Web project
* [Physical Semantic Web webpage](http://sisinflab.poliba.it/swottools/physicalweb/) - Official webpage for the Physical Semantic Web project
* [Eddystone Protocol](https://github.com/google/eddystone) - Repo with the specification for the beacons themselves
* Physical Semantic Web - Developed tools
	* [Physical Semantic Web Collection library](java/libs) - A Java SE library containing basic data structures and convenience utilities for storing metadata related to BLE devices;
    * [PSW Android client](android/PhysicalWeb) - A mobile client for Android devices    
    * [OWLEditor](android/OWLEditor) - An Android libray for for showing, composing and manipulating OWL annotations
    * [psw-node-eddystone-beacon](https://github.com/sisinflab-swot/psw-node-eddystone-beacon) - A simple library to advertise PSW Eddystone Beacons using Node.js
    * [psw-node-eddystone-beacon-scanner](https://github.com/sisinflab-swot/psw-node-eddystone-beacon-scanner) - A scan module for Node.js to discover PSW Eddystone beacons


## Awards

The Physical Semantic Web project proposed by [SisInf Lab](http://sisinflab.poliba.it/) people was selected for the *Google Internet of Things (IoT) Technology Research Award* in March 2016. Google will provide IoT technology prototypes and support for developing the project.

## References

If you want to refer to the Physical Semantic Web vision in a publication, please cite the following paper:

```
@InProceedings{psw-sac17,
  author       = {Michele Ruta and Floriano Scioscia and Saverio Ieva and Giuseppe Loseto and Filippo Gramegna and Agnese Pinto and Eugenio {Di Sciascio}},
  title        = {Knowledge discovery and sharing in the IoT: the Physical Semantic Web vision},
  booktitle    = {32nd ACM SIGAPP Symposium On Applied Computing},
  pages        = {492-498},
  month        = {apr},
  year         = {2017},
  organization = {Association for Computing Macinery, Inc. (ACM)}
}
```

## License

All projects composing the Physical Semantic Web toolkit are distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Contribute
The main purpose of this repository is to share and continue to improve the Physical Semantic Web framework, making it easier to use. If you're interested in helping us any feedback you have about using Physical Semantic Web would be greatly appreciated. There are only a few guidelines that we need contributors to follow reported in the CONTRIBUTING.md file.