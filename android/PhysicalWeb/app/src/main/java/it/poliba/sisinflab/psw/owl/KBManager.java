package it.poliba.sisinflab.psw.owl;

import android.content.Context;
import android.os.Environment;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.physical_web.collection.PwsResult;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import it.poliba.sisinflab.owl.owlapi.MicroReasoner;
import it.poliba.sisinflab.owl.owlapi.MicroReasonerFactory;
import it.poliba.sisinflab.owl.owlapi.ResourceNotFoundException;
import it.poliba.sisinflab.owl.sod.hlds.Abduction;
import it.poliba.sisinflab.owl.sod.hlds.AtomicConcept;
import it.poliba.sisinflab.owl.sod.hlds.Contraction;
import it.poliba.sisinflab.owl.sod.hlds.GreaterThanRole;
import it.poliba.sisinflab.owl.sod.hlds.Item;
import it.poliba.sisinflab.owl.sod.hlds.LessThanRole;
import it.poliba.sisinflab.owl.sod.hlds.SemanticDescription;
import it.poliba.sisinflab.owl.sod.hlds.UniversalRole;
import it.poliba.sisinflab.psw.PswDevice;

public class KBManager {

    final String TAG = KBManager.class.getSimpleName();

    Context mContext = null;
    OWLOntologyManager manager = null;
    OWLDataFactory factory = null;
    MicroReasoner reasoner = null;

    OWLAnnotationProperty urlAP = null;
    OWLAnnotationProperty titleAP = null;
    OWLAnnotationProperty descAP = null;
    OWLAnnotationProperty imgAP = null;

    IRI mRequest = null;

    final int DEFAULT_REQUEST = R.raw.mountadam_pinot_noir;

    public KBManager(Context mContext, int ontology) {
        this.mContext = mContext;
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();

        urlAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#url"));
        titleAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#title"));
        descAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#description"));
        imgAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#image"));

        loadOntology(ontology);
    }

    public KBManager(Context mContext) {
        new KBManager(mContext, R.raw.wine_ontology_www18);
    }

    private void loadOntology(int onto_resource) {
        InputStream onto = mContext.getResources().openRawResource(onto_resource);
        try {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(onto);

            MicroReasonerFactory reasonerFactory = new MicroReasonerFactory();
            reasoner = reasonerFactory.createMicroReasoner(ontology);

            Log.d(TAG, "Ontology Loaded! " + ontology.getOntologyID());
            Log.d(TAG, reasoner.getReasonerName() + " running...");

            manager.removeOntology(ontology);
            onto.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void loadCustomRequest() {
        try {
            File owl = new File(Environment.getExternalStorageDirectory().toString() + "/owleditor", "UserRequest.owl");

            OWLOntology tmp;
            if (owl.exists())
                tmp = manager.loadOntologyFromOntologyDocument(owl);
            else
                tmp = manager.loadOntologyFromOntologyDocument(mContext.getResources().openRawResource(DEFAULT_REQUEST));

            mRequest = reasoner.loadDemand(tmp).iterator().next();
            manager.removeOntology(tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getDefaultRequest() {
        return DEFAULT_REQUEST;
    }

    public static IRI loadIndividual(InputStream in) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        IRI iri = null;
        try {
            OWLOntology tmp = manager.loadOntologyFromOntologyDocument(in);
            if (tmp.getIndividualsInSignature().size()>0) {
                OWLNamedIndividual ind = tmp.getIndividualsInSignature().iterator().next();
                iri = ind.getIRI();
                manager.removeOntology(tmp);
            }
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return iri;
    }

    private boolean isLoaded(IRI iri) {
        try {
            if (reasoner.retrieveSupplyIndividual(iri) == null)
                return false;
            else
                return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public PwsResult getPSWResult(File file, PwsResult pwsResult, boolean load) {
        try {
            OWLOntology tmp = manager.loadOntologyFromOntologyDocument(file);
            if (tmp.getIndividualsInSignature().size()>0) {
                OWLNamedIndividual ind = tmp.getIndividualsInSignature().iterator().next();

                String title = getAnnotation(tmp, ind, titleAP, ind.getIRI().getFragment());
                String url = getAnnotation(tmp, ind, urlAP, "");
                String desc = getAnnotation(tmp, ind, descAP, "");
                String image = getAnnotation(tmp, ind, imgAP, "");

                // load (if needed) the beacon annotation into the KB
                if(!isLoaded(ind.getIRI()) || load) {
                    reasoner.loadSupply(tmp);
                    Log.d(TAG, ind.getIRI() + " loaded!");
                }

                manager.removeOntology(tmp);

                PwsResult replacement = new Utils.PwsResultBuilder(pwsResult)
                    .setTitle(title)
                    .setDescription(desc)
                    //.setDescription(url + "\n" + desc)
                    .setIconUrl(image)
                    .addExtra(PswDevice.PSW_IRI_KEY, ind.getIRI().toString())
                    .addExtra(PswDevice.PSW_BEACON_URL_KEY, url)
                    .addExtra(PswDevice.SITEURL_KEY, url)
                    .build();
                return replacement;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // reset manager
        for(OWLOntology onto : manager.getOntologies())
            manager.removeOntology(onto);

        return pwsResult;
    }

    private String getAnnotation(OWLOntology onto, OWLNamedIndividual ind, OWLAnnotationProperty ap, String defaultValue) {
        Set<OWLAnnotation> annotationSet = ind.getAnnotations(onto, ap);
        if (annotationSet.size()>0){
            OWLAnnotation annotation = annotationSet.iterator().next();
            return ((OWLLiteral)annotation.getValue()).getLiteral();
        }
        return defaultValue;
    }

    public double getRank(String individual) {
        double rank = 1;
        try {
            IRI resource = IRI.create(individual);
            //IRI request = IRI.create(resource.getNamespace(), "Request4");
            Item requestItem = reasoner.retrieveDemandIndividual(mRequest);

            Item resourceItem = null;
            if(reasoner.getSupplyIndividuals().contains(resource))
                resourceItem = reasoner.retrieveSupplyIndividual(resource);
            else
                return rank;

            Item empty = new Item(IRI.create("#Empty"));
            double max = empty.description.abduce(requestItem.description).penalty;

            if (resourceItem.description.checkCompatibility(requestItem.description)) {
                double pen_a = resourceItem.description.abduce(requestItem.description).penalty;
                rank = pen_a / max;
            } else {
                Contraction cc = resourceItem.description.contract(requestItem.description);
                Abduction ca = resourceItem.description.abduce(cc.K);
                double ca_max = empty.description.abduce(cc.K).penalty;
                rank = cc.penalty/max + ca.penalty/ca_max;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rank;

    }

    public String generateOWL(IRI baseUri, SemanticDescription h, SemanticDescription g) {
        System.out.println("Generating OWL " + baseUri.toString());

        OWLOntologyManager tmp = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = tmp.getOWLDataFactory();

        try {
            OWLOntology tmpNew = tmp.createOntology(baseUri);

            OWLObjectIntersectionOf desc = semDescToOWL(df, h);
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(baseUri.toString() + "MissingFeatures"));

            OWLClassAssertionAxiom ax = df.getOWLClassAssertionAxiom(desc, ind);
            AddAxiom addAx = new AddAxiom(tmpNew, ax);
            tmp.applyChange(addAx);

            if (g != null) {
                OWLObjectIntersectionOf desc2 = semDescToOWL(df, g);
                OWLNamedIndividual ind2 = df.getOWLNamedIndividual(IRI.create(baseUri.toString() + "IncompatibleFeatures"));

                OWLClassAssertionAxiom ax2 = df.getOWLClassAssertionAxiom(desc2, ind2);
                AddAxiom addAx2 = new AddAxiom(tmpNew, ax2);
                tmp.applyChange(addAx2);
            }

            ManchesterOWLSyntaxOntologyFormat ms = new ManchesterOWLSyntaxOntologyFormat();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            tmp.saveOntology(tmpNew, ms, os);
            String resp = os.toString();
            os.close();

            tmp.removeOntology(tmpNew);

            return resp.replaceAll("\n+", "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private OWLObjectIntersectionOf semDescToOWL(OWLDataFactory df, SemanticDescription sd) {
        Set<OWLClassExpression> desc = new HashSet<OWLClassExpression>();

        for (AtomicConcept a : sd.atomicConcepts) {
            OWLClass cls = df.getOWLClass(a.name);
            if (!a.denied)
                desc.add(cls);
            else
                desc.add(df.getOWLObjectComplementOf(cls));
        }

        for (LessThanRole ltr : sd.lessThanRoles) {
            desc.add(df.getOWLObjectMaxCardinality(ltr.cardinality, df.getOWLObjectProperty(ltr.name)));
        }

        for (GreaterThanRole gtr : sd.greaterThanRoles) {
            desc.add(df.getOWLObjectMinCardinality(gtr.cardinality, df.getOWLObjectProperty(gtr.name)));
        }

        for (UniversalRole ur : sd.universalRoles){
            OWLObjectProperty p = df.getOWLObjectProperty(ur.name);
            OWLObjectIntersectionOf filler = this.semDescToOWL(df, ur.filler);
            desc.add(df.getOWLObjectAllValuesFrom(p, filler));
        }

        return df.getOWLObjectIntersectionOf(desc);
    }

    public String getExplaination(String individual) {
        IRI resource = IRI.create(individual);
        Item requestItem = reasoner.retrieveDemandIndividual(mRequest);

        Item resourceItem = null;
        if(reasoner.getSupplyIndividuals().contains(resource))
            resourceItem = reasoner.retrieveSupplyIndividual(resource);
        else
            return null;

        String owl = "";
        if (resourceItem.description.checkCompatibility(requestItem.description)) {
            SemanticDescription h = resourceItem.description.abduce(requestItem.description).H;
            owl = generateOWL(IRI.create(resource.getNamespace()), h, null);
        } else {
            Contraction cc = resourceItem.description.contract(requestItem.description);
            Abduction ca = resourceItem.description.abduce(cc.K);
            owl = generateOWL(IRI.create(resource.getNamespace()), ca.H, cc.G);
        }

        return owl;
    }

}
