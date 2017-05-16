# OWLEditor for Android

This Android library contains data structures and convenience utilities for
visiting both entire OWL ontologies and simple OWL-based annotations and to compose OWL request on mobile devices.

Following the official guidelines for [Android-based UI](http://developer.android.com/guide/components/activities), the library includes:

- the **OWLEditorActivity**, acting as an high-level container;
- several **fragments**, each implementing a specific task (list classes or properties, compose a request).
 
All objects containing OWL-based information were implemented as [Parcelable](http://developer.android.com/guide/components/activities/parcelables-and-bundles.html) to ensure a correct and optimized exchange of data between Android processes (activities, fragments, services).

[OWL API](https://github.com/owlcs/owlapi) library (version 3.4.10) was exploited for creating, manipulating and serialising OWL Ontologies.

## Usage

The *OWLEditor* library was developed as an Android module in order to be easily integrated as dependency within a  project following these steps:

- Clone or download the repository
- Edit the *settings.gradle* file to include the OWLEditor library into your gradle project

```
include ':owleditor'

project(':owleditor').projectDir = new File('<path>/OwlEditor')
```

- Edit the *build.gradle* file to set the library as project *dependency*

```
dependencies {	
	// other project dependencies	
	compile(project(':owleditor'))
}
```

## Basic examples

### Show and visit an OWL annotation containing one or more OWL individuals

```java
/**
   * Open the OWLEditorActivity to view the content of an OWL annotation.
   * @param owlAnnotation The annotation as string
*/

Intent intent = new Intent(getActivity(), OWLEditorActivity.class);
intent.putExtra(getString(R.string.owl_string_key), owlAnnotation);
intent.putExtra(getString(R.string.owl_fragment_key), OWLIndividualFragment.class.getSimpleName());
startActivity(intent);
```

### Compose a new OWL annotation

The composed annotation will be saved on the external storage within the *owleditor* folder.

```java
/**
   * Open the OWLEditorActivity to compose a new OWL annotation.
   * @param ontology The reference ontology as string
*/

Intent intent = new Intent(this, OWLEditorActivity.class);
intent.putExtra(getString(R.string.owl_string_key), ontology);
intent.putExtra(getString(R.string.owl_fragment_key), OWLBuilderFragment.class.getSimpleName());
startActivity(intent);
```


