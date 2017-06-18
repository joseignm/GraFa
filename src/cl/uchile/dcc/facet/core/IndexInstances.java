package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class IndexInstances extends Indexer {

    public static void main(String[] args) throws IOException {
        System.out.println("IndexProperties");
        System.out.println("Creates a new index using the previous one");
        System.out.println("The new index contains all instances with related properties");
        System.out.println();

        if (args.length != 2) {
            System.out.println("USAGE: Lucene_Index_Folder Index_Out_Dir");
            System.exit(0);
        }

        // INIT: Make reader and searcher
        System.out.println("Init...");
        final int TICKS = 1000;
        final int DOCS_LIMIT = 200;
        String in = args[0];
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(in)));
        Fields fields = MultiFields.getFields(reader);
        IndexSearcher searcher = new IndexSearcher(reader);

        // FIRST PART: Create index for all instances
        System.out.println("Creating the index...");
        Terms terms = fields.terms(DataFields.INSTANCE.name());
        TermsEnum termsEnum = terms.iterator();
        BytesRef text;
        int read = 0;

        String insDir = args[1];
        Analyzer analyzer = new EnglishAnalyzer();
        IndexWriter writer = makeWriter(insDir, analyzer);

        while ((text = termsEnum.next()) != null) {
            read++;
            if (read % TICKS == 0)
                System.out.println(read + " instances processed...");
            // Get the label of the instance
            String q = text.utf8ToString();
            String label = getField(searcher, q, DataFields.LABEL.name());
            String boostString = getField(searcher, q, DataFields.VALUE.name());
            double boost = boostString == null ? 0d : Double.parseDouble(boostString);
            Term term = new Term(DataFields.INSTANCE.name(), q);
            long occurrences = reader.docFreq(term);
            if (label == null) continue;
            if (occurrences < 1) System.err.println("WARN: No occurrences for " + q);
            // Create document with Q and Label
            Document d = new Document();
            Field qField = new StringField(InstancesFields.Q.name(), q, Field.Store.YES);
            Field labelField = new TextField(InstancesFields.LABEL.name(), label, Field.Store.YES);
            Field occurrencesField = new NumericDocValuesField(InstancesFields.OCCURRENCES.name(), occurrences);
            Field number = new StoredField(InstancesFields.NUMBER.name(), occurrences);
            Field boostField = new DoubleDocValuesField(InstancesFields.BOOST.name(), boost);
            d.add(qField);
            d.add(labelField);
            d.add(occurrencesField);
            d.add(number);
            d.add(boostField);
            // Get all possible properties for this particular instance
            if(occurrences > DOCS_LIMIT){
                // Map with property and frequency
                Map<String, Integer> propertiesMap = new HashMap<>();
                Query query = new TermQuery(new Term(DataFields.INSTANCE.name(), q));
                TopDocs results = searcher.search(query, reader.maxDoc());
                ScoreDoc[] hits = results.scoreDocs;
                for(ScoreDoc hit : hits) {
                    Document doc = searcher.doc(hit.doc);
                    IndexableField[] ps = doc.getFields(DataFields.P.name());
                    for(IndexableField p : ps) {
                        String key = p.stringValue();
                        if(propertiesMap.containsKey(key)) {
                            propertiesMap.replace(key, propertiesMap.get(key)+1);
                        } else {
                            propertiesMap.put(key, 1);
                        }
                    }
                }
                // Add the values to the document
                for(Map.Entry<String, Integer> entry : propertiesMap.entrySet()) {
                    String code = entry.getKey();
                    Integer frequency = entry.getValue();
                    String property = code + "##" + frequency.toString();
                    Field propertyField = new StringField(InstancesFields.PROPERTY.name(), property, Field.Store.YES);
                    d.add(propertyField);
                }
            }
            writer.addDocument(d);
        }
        System.out.println(read + " instances processed in total");
        writer.close();
        System.out.println("Completed!");
    }
}
