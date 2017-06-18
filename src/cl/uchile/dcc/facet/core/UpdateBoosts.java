package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class UpdateBoosts {

    public static void main(String[] args) throws IOException {
        final int TICKS = 1000000;

        System.out.println("Update Boosts");

        if(args.length != 3) {
            System.out.println("USAGE: Rank_File Old_Lucene_Dir New_Lucene_Dir");
        }

        String ranksFile = args[0];
        String oldLuceneDir = args[1];
        String newLuceneDir = args[2];

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
            String label = oldDocument.get(DataFields.LABEL.name());
            String description = oldDocument.get(DataFields.DESCRIPTION.name());
            String alt = oldDocument.get(DataFields.ALT_LABEL.name());
            String[] instances = oldDocument.getValues(DataFields.INSTANCE.name());
            String[] ps = oldDocument.getValues(DataFields.P.name());
            String[] pos = oldDocument.getValues(DataFields.PO.name());

            Document newDocument = new Document();
            Field subjectField = new StringField(DataFields.SUBJECT.name(), subject, Field.Store.YES);
            newDocument.add(subjectField);
            if(label != null) {
                Field labelField = new TextField(DataFields.LABEL.name(), label, Field.Store.YES);
                newDocument.add(labelField);
            }
            if(description != null) {
                Field descriptionField = new TextField(DataFields.DESCRIPTION.name(), description, Field.Store.YES);
                newDocument.add(descriptionField);
            }
            if(alt != null) {
                Field altField = new TextField(DataFields.ALT_LABEL.name(), alt, Field.Store.YES);
                newDocument.add(altField);
            }
            for(String instance : instances) {
                Field instanceField = new StringField(DataFields.INSTANCE.name(), instance, Field.Store.YES);
                newDocument.add(instanceField);
            }
            for(String p : ps) {
                Field pField = new StringField(DataFields.P.name(), p, Field.Store.YES);
                newDocument.add(pField);
            }
            for(String po : pos) {
                Field poField = new StringField(DataFields.PO.name(), po, Field.Store.YES);
                newDocument.add(poField);
            }
            Field boostsField = new DoubleDocValuesField(DataFields.BOOSTS.name(), rank);
            newDocument.add(boostsField);
            Field storedField = new StoredField(DataFields.VALUE.name(), rank);
            newDocument.add(storedField);

            writer.addDocument(newDocument);
            read++;
        }

        writer.close();

        System.out.println("Min value: " + min);
        System.out.println("Max value: " + max);
        System.out.println("Complete! New directory created with boosts.");
    }

}
