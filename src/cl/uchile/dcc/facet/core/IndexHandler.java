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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

class IndexHandler extends AbstractRDFHandler {

    private IndexWriter writer;
    private Resource last;
    private List<String> ps;
    private Document d;
    private int read;
    private Properties properties;

    IndexHandler(IndexWriter iw) throws IOException {
        super();
        writer = iw;
        last = null;
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
        final String image = properties.getProperty("image");
        final String entityPrefix = properties.getProperty("entityPrefix");

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
            ps = new ArrayList<>();
            Field subj = new StringField(DataFields.SUBJECT.name(), name, Field.Store.YES);
            d.add(subj);
        }
        // NEW SUBJECT
        if(!last.toString().equals(subject.toString())) {
            last = subject;
            try {
                writer.addDocument(d);
            } catch (IOException e) {
                System.err.println("Error writing Lucene document.");
            }
            String name = last.toString();
            name = name.replace(entityIRI, "");
            d = new Document();
            ps = new ArrayList<>();
            Field subj = new StringField(DataFields.SUBJECT.name(), name, Field.Store.YES);
            d.add(subj);
        }
        // PROPERTIES
        String predicate = s.getPredicate().toString();
        if(predicate.startsWith(propertyIRI)) {
            String p = predicate.replace(propertyIRI, "");
            if(!ps.contains(p)) {
                ps.add(p);
                Field propertyField = new StringField(DataFields.PROPERTY.name(), p, Field.Store.YES);
                d.add(propertyField);
            }
            String object = s.getObject().toString();
            String value = object.replace(entityIRI, "");
            if(p.equals(instanceOf)) {
                Field typeField = new StringField(DataFields.TYPE.name(), value, Field.Store.YES);
                d.add(typeField);
            }
            if(p.equals(image)) {
                Field imgField = new StringField(DataFields.IMAGE.name(), value, Field.Store.YES);
                d.add(imgField);
            }
            if(value.startsWith(entityPrefix)) {
                String po = p + "##" + value;
                Field poField = new StringField(DataFields.PO.name(), po, Field.Store.YES);
                d.add(poField);
            }
        } else {
            // LITERAL VALUES
            if(!(s.getObject() instanceof Literal)) return;
            Literal value = (Literal) s.getObject();
            String language = value.getLanguage().orElse("??");
            // CURRENT LANGUAGES
            List<String> languages = Arrays.asList(properties.getProperty("languages").split(","));
            if(!languages.contains(language)) return;
            String object = value.getLabel();
            if(predicate.equals(labelIRI)) {
                Field label = new TextField(DataFields.LABEL.name()+"-"+language, object, Field.Store.YES);
                d.add(label);
            } else if(predicate.equals(descriptionIRI)) {
                Field description = new TextField(DataFields.DESCRIPTION.name()+"-"+language, object, Field.Store.YES);
                d.add(description);
            } else if(predicate.equals(alt_labelIRI)) {
                Field altLabel = new TextField(DataFields.ALT_LABEL.name()+"-"+language, object, Field.Store.YES);
                d.add(altLabel);
            }
        }
    }

    void finish() {
        try {
            if(d != null) {
                System.err.println(read + " lines read in total.");
                writer.addDocument(d);
            }
            writer.close();
            System.out.println("Complete!");
        } catch(IOException e) {
            System.err.println("Error. Cannot close Lucene writer");
        }
    }
}
