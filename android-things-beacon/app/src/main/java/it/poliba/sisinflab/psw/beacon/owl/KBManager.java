package it.poliba.sisinflab.psw.beacon.owl;

import android.util.Log;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class KBManager {

    private String TAG = KBManager.class.getSimpleName();

    public static String TEMPERATURE_PROPERTY = "hasTemperature";
    public static String HUMIDITY_PROPERTY = "hasHumidity";
    public static String LIGHT_PROPERTY = "hasLight";

    public static String[] TEMPERATURE_VALUES = new String[]{"VeryLowTemperature", "MediumTemperature", "VeryHighTemperature"};
    public static String[] HUMIDITY_VALUES = new String[]{"VeryLowHumidity", "MediumHumidity", "VeryHighHumidity"};
    public static String[] LIGHT_VALUES = new String[]{"LowLight", "MediumLight", "HighLight"};

    OWLOntologyManager manager = null;
    OWLDataFactory factory = null;
    OWLOntology ontology = null;

    IRI resourceIRI;

    public KBManager(InputStream onto) {
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();

        try {
            ontology = manager.loadOntologyFromOntologyDocument(onto);
            resourceIRI = ontology.getIndividualsInSignature().iterator().next().getIRI();

            Log.d(TAG, "Ontology Loaded! " + ontology.getOntologyID());
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    public IRI getOntologyIRI() {
        return IRI.create(resourceIRI.getNamespace());
    }

    public IRI getResourceIRI() {
        return resourceIRI;
    }

    public String getOWL() {
        String owl = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OWLOntologyFormat format = manager.getOntologyFormat(ontology);
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        if (format.isPrefixOWLOntologyFormat()) {
            manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
        }
        try {
            manager.saveOntology(ontology, manSyntaxFormat, out);
            owl = new String(out.toByteArray(), Charset.defaultCharset());
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        return owl;
    }

    public void setPropertyValue(String property, String value) {
        if (ontology.getIndividualsInSignature().size()>0) {
            OWLNamedIndividual ind = ontology.getIndividualsInSignature().iterator().next();

            Set<OWLClassExpression> updated = new HashSet<OWLClassExpression>();

            OWLIndividualAxiom ax = ontology.getAxioms(ind).iterator().next();
            for(OWLClassExpression exp : ((OWLObjectIntersectionOf)((OWLClassAssertionAxiom)ax).getClassExpression()).getOperands()) {
                if(exp.getClassExpressionType() == ClassExpressionType.OBJECT_ALL_VALUES_FROM) {
                    OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom)exp;
                    if (all.getProperty().getNamedProperty().getIRI().getFragment().equalsIgnoreCase(property)) {
                        OWLClass updatedValue = factory.getOWLClass(IRI.create(resourceIRI.getNamespace() + value));
                        OWLObjectAllValuesFrom updatedObj = factory.getOWLObjectAllValuesFrom(all.getProperty(), updatedValue);
                        updated.add(updatedObj);
                    } else
                        updated.add(exp);
                } else
                    updated.add(exp);
            }

            // remove old axiom
            manager.removeAxiom(ontology, ax);

            // add updated axiom
            OWLObjectIntersectionOf updatedInter = factory.getOWLObjectIntersectionOf(updated);
            OWLClassAssertionAxiom updatedAx = factory.getOWLClassAssertionAxiom(updatedInter, ind);
            AddAxiom addAxiomChange = new AddAxiom(ontology, updatedAx);
            manager.applyChange(addAxiomChange);
        }
    }

}
