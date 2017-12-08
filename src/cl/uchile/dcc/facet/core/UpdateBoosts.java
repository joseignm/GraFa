package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class UpdateBoosts {

    public static void main(String[] args) throws IOException {
        final int TICKS = 1000000;

        System.out.println("Update Boosts");

        if(args.length != 3) {
            System.out.println("USAGE: Rank_File Old_Lucene_Dir New_Lucene_Dir");
        }

        long startTime = System.currentTimeMillis();
        String ranksFile = args[0];
        String oldLuceneDir = args[1];
        String newLuceneDir = args[2];

        Properties properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);

        String[] languages = properties.getProperty("languages").split(",");

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(oldLuceneDir)));
        IndexWriterConfig iwc = new IndexWriterConfig(new EnglishAnalyzer());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(newLuceneDir)), iwc);

        System.err.println(reader.maxDoc() + " documents stored");

        InputStream is = new FileInputStream(ranksFile);
        if(ranksFile.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int read = 0;
        String line;
        double min = 1d;
        double max = 0d;

        while((line = br.readLine()) != null) {
            if(read%TICKS == 0) {
                System.err.println(read + " lines read...");
            }
            String[] tabs = line.split("\t");
            if(tabs.length < 2) {
                System.err.println("Corrupted line in " + read);
            }
            int doc = Integer.parseInt(tabs[0]);
            double rank = Double.parseDouble(tabs[1]);
            if(rank < min) {
                min = rank;
            }
            if(rank > max) {
                max = rank;
            }

            Document oldDocument = reader.document(doc);
            String subject = oldDocument.get(DataFields.SUBJECT.name());
            String image = oldDocument.get(DataFields.IMAGE.name());
            String[] instances = oldDocument.getValues(DataFields.TYPE.name());
            String[] ps = oldDocument.getValues(DataFields.PROPERTY.name());
            String[] pos = oldDocument.getValues(DataFields.PO.name());

            Document newDocument = new Document();
            Field subjectField = new StringField(DataFields.SUBJECT.name(), subject, Field.Store.YES);
            newDocument.add(subjectField);
            if(image != null) {
                Field imageField = new StringField(DataFields.IMAGE.name(), image, Field.Store.YES);
                newDocument.add(imageField);
            }
            for(String instance : instances) {
                Field instanceField = new StringField(DataFields.TYPE.name(), instance, Field.Store.YES);
                newDocument.add(instanceField);
            }
            for(String p : ps) {
                Field pField = new StringField(DataFields.PROPERTY.name(), p, Field.Store.YES);
                newDocument.add(pField);
            }
            for(String po : pos) {
                Field poField = new StringField(DataFields.PO.name(), po, Field.Store.YES);
                newDocument.add(poField);
            }
            Field boostsField = new DoubleDocValuesField(DataFields.RANK.name(), rank);
            newDocument.add(boostsField);
            Field storedField = new StoredField(DataFields.RANK_STORED.name(), rank);
            newDocument.add(storedField);

            // LABELS
            for(String lang : languages) {
                String labelFieldName = DataFields.LABEL.name() + "-" + lang;
                String altLabelFieldName = DataFields.ALT_LABEL.name() + "-" + lang;
                String descriptionFieldName = DataFields.DESCRIPTION.name() + "-" + lang;

                String[] labels = oldDocument.getValues(labelFieldName);
                String[] altLabels = oldDocument.getValues(altLabelFieldName);
                String[] descriptions = oldDocument.getValues(descriptionFieldName);

                for(String label: labels) {
                    Field labelField = new TextField(labelFieldName, label, Field.Store.YES);
                    newDocument.add(labelField);
                }
                for(String altLabel: altLabels) {
                    Field altLabelField = new TextField(altLabelFieldName, altLabel, Field.Store.YES);
                    newDocument.add(altLabelField);
                }
                for(String description : descriptions) {
                    Field descriptionField = new TextField(descriptionFieldName, description, Field.Store.YES);
                    newDocument.add(descriptionField);
                }
            }

            writer.addDocument(newDocument);
            read++;
        }

        writer.close();

        long totalTime = System.currentTimeMillis() - startTime;
        System.err.println("Total time: " + totalTime + " ms");

        System.out.println("Min value: " + min);
        System.out.println("Max value: " + max);
        System.out.println("Complete! New directory created with boosts.");
    }

}
