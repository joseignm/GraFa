package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.IOException;

class IndexHandler extends AbstractRDFHandler {

    private IndexWriter writer;
    private Resource last;
    private StringBuilder alt_labels;
    private Document d;
    private int read;

    IndexHandler(IndexWriter iw) {
        super();
        writer = iw;
        last = null;
        alt_labels = new StringBuilder();
        d = null;
        read = 0;
    }

    @Override
    public void handleStatement(Statement s) {
        final int TICKS = 100000;
        final String entityIRI = "http://www.wikidata.org/entity/";
        final String propertyIRI = "http://www.wikidata.org/prop/direct/";
        final String labelIRI = "http://www.w3.org/2000/01/rdf-schema#label";
        final String descriptionIRI = "http://schema.org/description";
        final String alt_labelIRI = "http://www.w3.org/2004/02/skos/core#altLabel";
        final String instanceOf = "P31";

        read++;
        if(read%TICKS==0)
            System.err.println(read+" lines read...");

        Resource subject = s.getSubject();
        // FIRST LINE
        if(last == null) {
            last = subject;
            String name = last.toString();
            name = name.replace(entityIRI, "");
            d = new Document();
            Field subj = new TextField(DataFields.SUBJECT.name(), name, Field.Store.YES);
            d.add(subj);
            // NEW! Dummy Numeric DocValues
            Field dummyField = new DoubleDocValuesField(DataFields.BOOSTS.name(), 0d);
            d.add(dummyField);
        }
        // NEW SUBJECT
        if(!last.toString().equals(subject.toString())) {
            Field alt_label = new TextField(DataFields.ALT_LABEL.name(), alt_labels.toString(), Field.Store.YES);
            alt_labels = new StringBuilder();
            d.add(alt_label);
            last = subject;
            try {
                writer.addDocument(d);
            } catch(IOException e) {
                System.err.println("Error writing Lucene document.");
            }
            String name = last.toString();
            name = name.replace(entityIRI, "");
            d = new Document();
            Field subj = new StringField(DataFields.SUBJECT.name(), name, Field.Store.YES);
            d.add(subj);
        }
        // PROPERTIES
        String predicate = s.getPredicate().toString();
        if(predicate.startsWith(propertyIRI)) {
            String p = predicate.replace(propertyIRI, "");
            Field property = new StringField(DataFields.P.name(), p, Field.Store.YES);
            d.add(property);
            String object = s.getObject().toString();
            String q = object.replace(entityIRI, "");
            String value = p + "##" + q;
            Field po = new StringField(DataFields.PO.name(), value, Field.Store.YES);
            d.add(po);
            if(p.equals(instanceOf)) {
                Field ins = new StringField(DataFields.INSTANCE.name(), q, Field.Store.YES);
                d.add(ins);
            }
        } else {
            // LITERAL VALUES, ONLY ENGLISH
            if(!(s.getObject() instanceof Literal)) return;
            Literal value = (Literal) s.getObject();
            String language = value.getLanguage().orElse("??");
            if(!language.equals("en")) return;
            String object = value.getLabel();
            switch (predicate){
                case labelIRI:
                    Field label = new TextField(DataFields.LABEL.name(), object, Field.Store.YES);
                    d.add(label);
                    break;
                case descriptionIRI:
                    Field description = new TextField(DataFields.DESCRIPTION.name(), object, Field.Store.YES);
                    d.add(description);
                    break;
                case alt_labelIRI:
                    alt_labels.append(object);
                    alt_labels.append(' ');
                    break;
            }
        }
    }

    void finish() {
        try {
            if(d != null) {
                System.err.println(read + " lines read in total.");
                Field alt_label = new TextField(DataFields.ALT_LABEL.name(), alt_labels.toString(), Field.Store.YES);
                d.add(alt_label);
                writer.addDocument(d);
            }

            writer.close();
            System.out.println("Complete!");
        } catch(IOException e) {
            System.err.println("Error. Cannot close Lucene writer");
        }
    }
}
