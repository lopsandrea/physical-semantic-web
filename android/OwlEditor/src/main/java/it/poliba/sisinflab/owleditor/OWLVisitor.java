package it.poliba.sisinflab.owleditor;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectAllValuesFromImpl;

class OWLVisitor {

    private OWLOntologyManager mOWLManager;
    private OWLOntology mOntology;
    private OWLDataFactory mDataFactory;

    public OWLVisitor (String ontology) {
        mOWLManager = OWLManager.createOWLOntologyManager();
        mDataFactory = mOWLManager.getOWLDataFactory();
        loadOntology(ontology);
    }

    private void loadOntology(String ontology) {
        try {
            InputStream stream = new ByteArrayInputStream(ontology.getBytes(StandardCharsets.UTF_8));
            mOntology = mOWLManager.loadOntologyFromOntologyDocument(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeOntology() {
        if (mOntology != null)
            mOWLManager.removeOntology(mOntology);
    }

    public ArrayList<OWLItem> getOWLIndividuals() {
        ArrayList<OWLItem> items = new ArrayList<>();
        if(mOntology.getIndividualsInSignature().size() > 0) {
            for (OWLNamedIndividual ind : mOntology.getIndividualsInSignature()) {
                OWLItem item = new OWLItem(OWLItem.OWL_INDIVIDUAL, ind.getIRI());
                item.getFiller().addAll(visit(ind.getTypes(mOntology)));
                items.add(item);
            }
        }
        return items;
    }

    public ArrayList<OWLItem> getOWLClass() {
        ArrayList<OWLItem> items = new ArrayList<>();
        if(mOntology.getClassesInSignature().size() > 0) {
            for (OWLClass cls : mOntology.getClassesInSignature()) {
                items.add(new OWLItem(OWLItem.OWL_CLASS, cls.getIRI()));
            }
        }
        return items;
    }

    private ArrayList<OWLItem> visit(OWLClassExpression exp) {
        ArrayList<OWLItem> items = new ArrayList<>();
        if (exp instanceof OWLClass) {
            items.add(new OWLItem(OWLItem.OWL_CLASS, exp.asOWLClass().getIRI()));
        } else if (exp instanceof OWLObjectAllValuesFrom) {
            IRI objProp = ((OWLObjectAllValuesFromImpl) exp).getProperty().asOWLObjectProperty().getIRI();
            OWLItem item = new OWLItem(OWLItem.OWL_ALL_VALUES_FROM, objProp);
            item.getFiller().addAll(visit(((OWLObjectAllValuesFrom) exp).getFiller().asConjunctSet()));
            items.add(item);
        } else if (exp instanceof OWLObjectIntersectionOf) {
            items.addAll(visit(exp.asConjunctSet()));
        }
        return items;
    }

    private ArrayList<OWLItem> visit(Set<OWLClassExpression> expressionSet) {
        ArrayList<OWLItem> items = new ArrayList<>();
        for (OWLClassExpression exp : expressionSet) {
            items.addAll(visit(exp));
        }
        return items;
    }

    public ArrayList<OWLItem> getOWLSubClassOf() {
        return getOWLSubClassOf(mDataFactory.getOWLThing().getIRI());
    }

    public ArrayList<OWLItem> getOWLSubClassOf(IRI root) {
        ArrayList<OWLItem> items = new ArrayList<>();
        for(OWLSubClassOfAxiom sub : mOntology.getSubClassAxiomsForSuperClass(mDataFactory.getOWLClass(root))){
            for(OWLClass cls : sub.getSubClass().getClassesInSignature()) {
                items.add(new OWLItem(OWLItem.OWL_CLASS, cls.getIRI()));
            }
        }
        return items;
    }

    public ArrayList<OWLItem> getOWLObjectProperties() {
        ArrayList<OWLItem> items = new ArrayList<>();
        if(mOntology.getClassesInSignature().size() > 0) {
            for (OWLObjectProperty p : mOntology.getObjectPropertiesInSignature()) {
                items.add(new OWLItem(OWLItem.OWL_ALL_VALUES_FROM, p.getIRI()));
            }
        }
        return items;
    }

    public void saveOWLRequest(String reqName, OWLItem request, File out) {
        OWLOntology onto = null;
        try {
            String ns = mOntology.getOntologyID().getOntologyIRI().getNamespace().toString();
            onto = mOWLManager.createOntology(IRI.create(ns, "onto-" + reqName));

            OWLNamedIndividual ind = createOWLNamedIndividual(IRI.create(ns, reqName));
            OWLObjectIntersectionOf intersection = createOWLObjectIntersectionOf(request.getFiller());
            OWLClassAssertionAxiom ax = mDataFactory.getOWLClassAssertionAxiom(intersection, ind);
            mOWLManager.addAxiom(onto, ax);

            ManchesterOWLSyntaxOntologyFormat format = new ManchesterOWLSyntaxOntologyFormat();
            format.setDefaultPrefix(mOntology.getOntologyID().getDefaultDocumentIRI().toString() + "#");

            mOWLManager.saveOntology(onto, format, IRI.create(out.toURI()));
            mOWLManager.removeOntology(onto);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OWLObjectIntersectionOf createOWLObjectIntersectionOf(ArrayList<OWLItem> items) {
        Set<OWLClassExpression> exp = new HashSet<>();
        for(OWLItem item : items) {
            switch (item.getType()) {
                case OWLItem.OWL_CLASS:
                    exp.add(createOWLClass(item.getIRI()));
                    break;
                case OWLItem.OWL_COMPLEMENT_OF:
                    exp.add(createOWLObjectComplementOf(item.getIRI()));
                    break;
                case OWLItem.OWL_ALL_VALUES_FROM:
                    exp.add(createOWLObjectAllValuesFrom(item.getIRI(), item.getFiller()));
                    break;
                case OWLItem.OWL_MAX_CARDINALITY:
                    exp.add(createOWLObjectMaxCardinality(item.getIRI(), item.getCardinality()));
                    break;
                case OWLItem.OWL_MIN_CARDINALITY:
                    exp.add(createOWLObjectMinCardinality(item.getIRI(), item.getCardinality()));
                    break;
            }
        }
        return mDataFactory.getOWLObjectIntersectionOf(exp);
    }

    private OWLClass createOWLClass(IRI iri) {
        return mDataFactory.getOWLClass(iri);
    }

    private OWLObjectProperty createOWLObjectProperty(IRI iri) {
        return mDataFactory.getOWLObjectProperty(iri);
    }

    private OWLObjectComplementOf createOWLObjectComplementOf(IRI iri) {
        OWLClass cls = createOWLClass(iri);
        return mDataFactory.getOWLObjectComplementOf(cls);
    }

    private OWLObjectMinCardinality createOWLObjectMinCardinality(IRI iri, int value) {
        OWLObjectProperty p = createOWLObjectProperty(iri);
        return mDataFactory.getOWLObjectMinCardinality(value, p);
    }

    private OWLObjectMaxCardinality createOWLObjectMaxCardinality(IRI iri, int value) {
        OWLObjectProperty p = createOWLObjectProperty(iri);
        return mDataFactory.getOWLObjectMaxCardinality(value, p);
    }

    private OWLNamedIndividual createOWLNamedIndividual(IRI iri) {
        return mDataFactory.getOWLNamedIndividual(iri);
    }

    private OWLObjectAllValuesFrom createOWLObjectAllValuesFrom(IRI iri, ArrayList<OWLItem> items) {
        OWLObjectProperty p = createOWLObjectProperty(iri);
        OWLClassExpression exp;
        if (items.size()>0)
            exp = createOWLObjectIntersectionOf(items);
        else
            exp = mDataFactory.getOWLThing();

        return mDataFactory.getOWLObjectAllValuesFrom(p, exp);
    }
}
