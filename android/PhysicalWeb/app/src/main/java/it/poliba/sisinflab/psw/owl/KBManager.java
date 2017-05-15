package it.poliba.sisinflab.psw.owl;

import android.content.Context;

import org.physical_web.collection.PwsResult;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import it.poliba.sisinflab.owl.owlapi.MicroReasoner;
import it.poliba.sisinflab.owl.owlapi.MicroReasonerFactory;
import it.poliba.sisinflab.owl.owlapi.ResourceNotFoundException;
import it.poliba.sisinflab.owl.sod.hlds.Contraction;
import it.poliba.sisinflab.owl.sod.hlds.Item;
import it.poliba.sisinflab.psw.PswDevice;
import it.poliba.sisinflab.psw.PswUtils;

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

    public KBManager(Context mContext) {
        this.mContext = mContext;
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();

        loadDefaultOntology();

        urlAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#url"));
        titleAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#title"));
        descAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#description"));
        imgAP = factory.getOWLAnnotationProperty(IRI.create("http://ogp.me/ns#image"));
    }

    private void loadDefaultOntology() {
        InputStream onto = mContext.getResources().openRawResource(R.raw.cultural_vienna);
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

    public PwsResult getPSWResult(File file, PwsResult pwsResult) {
        try {
            OWLOntology tmp = manager.loadOntologyFromOntologyDocument(file);
            if (tmp.getIndividualsInSignature().size()>0) {
                OWLNamedIndividual ind = tmp.getIndividualsInSignature().iterator().next();

                String title = getAnnotation(tmp, ind, titleAP, ind.getIRI().getFragment());
                String url = getAnnotation(tmp, ind, urlAP, "");
                String desc = getAnnotation(tmp, ind, descAP, "");

                // load (if needed) the beacon annotation into the KB
                if (!isLoaded(ind.getIRI())) {
                    reasoner.loadSupply(tmp);
                    Log.d(TAG, ind.getIRI() + " loaded!");
                }

                manager.removeOntology(tmp);

                PwsResult replacement = new Utils.PwsResultBuilder(pwsResult)
                    .setTitle(title)
                    .setDescription(url + "\n" + desc)
                    .addExtra(PswDevice.PSW_IRI_KEY, ind.getIRI().toString())
                    .addExtra(PswDevice.PSW_BEACON_URL_KEY, url)
                    .build();
                return replacement;
            }
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

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
            IRI request = IRI.create(resource.getNamespace(), "Request4");
            Item requestItem = reasoner.retrieveSupplyIndividual(request);
            Item resourceItem = reasoner.retrieveSupplyIndividual(resource);
            Item empty = new Item(IRI.create("#Empty"));

            double max = empty.description.abduce(requestItem.description).penalty;

            if (resourceItem.description.checkCompatibility(requestItem.description)) {
                double pen_a = resourceItem.description.abduce(requestItem.description).penalty;
                rank = pen_a / max;
            } else {
                Contraction cc = resourceItem.description.contract(requestItem.description);
                double pen_c = cc.K.abduce(requestItem.description).penalty;
                rank = pen_c / max;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rank;

    }

}
