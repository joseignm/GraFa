package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class IndexHandler extends AbstractRDFHandler {

    private IndexWriter writer;
    private Resource last;
    private List<String> alt_labels;
    private List<String> ps;
    private Document d;
    private int read;
    private Properties properties;

    IndexHandler(IndexWriter iw) throws IOException {
        super();
        writer = iw;
        last = null;
        alt_labels = new ArrayList<>();
        ps = new ArrayList<>();
        d = null;
        read = 0;
        properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);
    }

    @Override
    public void handleStatement(Statement s) {
        final int TICKS = 100000;

        final String entityIRI = properties.getProperty("entityIRI");
        final String propertyIRI = properties.getProperty("propertyIRI");
        final String labelIRI = properties.getProperty("labelIRI");
        final String descriptionIRI = properties.getProperty("descriptionIRI");
        final String alt_labelIRI = properties.getProperty("alt_labelIRI");
        final String instanceOf = properties.getProperty("instanceOf");

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
            for(String label: alt_labels) {
                Field alt_label = new TextField(DataFields.ALT_LABEL.name(), label, Field.Store.YES);
                d.add(alt_label);
            }
            for(String p : ps) {
                Field property = new StringField(DataFields.P.name(), p, Field.Store.YES);
                d.add(property);
            }
            alt_labels = new ArrayList<>();
            ps = new ArrayList<>();
            last = subject;
            // Write the document only if it has a proper label
            if(d.get(DataFields.LABEL.name()) != null) {
                try {
                    writer.addDocument(d);
                } catch (IOException e) {
                    System.err.println("Error writing Lucene document.");
                }
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
            if(!ps.contains(p)) ps.add(p);
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
            // LITERAL VALUES
            String selectedLang = properties.getProperty("language");
            if(!(s.getObject() instanceof Literal)) return;
            Literal value = (Literal) s.getObject();
            String language = value.getLanguage().orElse("??");
            if(!language.equals(selectedLang)) return;
            String object = value.getLabel();
            if(predicate.equals(labelIRI)) {
                Field label = new TextField(DataFields.LABEL.name(), object, Field.Store.YES);
                d.add(label);
            } else if(predicate.equals(descriptionIRI)) {
                Field description = new TextField(DataFields.DESCRIPTION.name(), object, Field.Store.YES);
                d.add(description);
            } else if(predicate.equals(alt_labelIRI)) {
                alt_labels.add(object);
            }
        }
    }

    void finish() {
        try {
            if(d != null) {
                System.err.println(read + " lines read in total.");
                for(String label: alt_labels) {
                    Field alt_label = new TextField(DataFields.ALT_LABEL.name(), label, Field.Store.YES);
                    d.add(alt_label);
                }
                for(String p : ps) {
                    Field property = new StringField(DataFields.P.name(), p, Field.Store.YES);
                    d.add(property);
                }
                writer.addDocument(d);
            }

            writer.close();
            System.out.println("Complete!");
        } catch(IOException e) {
            System.err.println("Error. Cannot close Lucene writer");
        }
    }
}
