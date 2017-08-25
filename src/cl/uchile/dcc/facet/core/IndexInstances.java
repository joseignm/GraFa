package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexInstances extends Indexer {

    private static final int DOCS_LIMIT = 50000;

    private static IndexSearcher dataSearcher;

    private static Map<String,Integer> getAllProperties(TopDocs results) throws IOException {
        Map<String, Integer> propertiesMap = new HashMap<>();
        ScoreDoc[] hits = results.scoreDocs;
        for(ScoreDoc hit : hits) {
            Document doc = dataSearcher.doc(hit.doc);
            IndexableField[] pos = doc.getFields(DataFields.PO.name());
            for(IndexableField po : pos) {
                String raw = po.stringValue();
                String[] split = raw.split("##");
                if(!split[1].startsWith("Q")) continue;
                String key = split[0];
                if(key.equals("P31")) continue;
                if(propertiesMap.containsKey(key)) {
                    propertiesMap.replace(key, propertiesMap.get(key)+1);
                } else {
                    propertiesMap.put(key, 1);
                }
            }
        }
        return propertiesMap;
    }

    private static void addPropertiesToDoc(Map<String, Integer> properties, Document d) {
        for(Map.Entry<String, Integer> entry : properties.entrySet()) {
            String code = entry.getKey();
            Integer frequency = entry.getValue();
            String property = code + "##" + frequency.toString();
            Field propertyField = new StringField(InstancesFields.PROPERTY.name(), property, Field.Store.YES);
            d.add(propertyField);
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("IndexProperties");
        System.out.println("Creates a new index using the previous one");
        System.out.println("The new index contains all instances with related properties");
        System.out.println();
        if (args.length != 3) {
            System.out.println("USAGE: DataIndex CachingFile OutputIndex");
            System.exit(0);
        }

        // INIT: Make reader and searcher
        System.out.println("Init...");
        final int TICKS = 100;

        String dataDirectory = args[0];
        IndexReader dataReader = DirectoryReader.open(FSDirectory.open(Paths.get(dataDirectory)));
        dataSearcher = new IndexSearcher(dataReader);

        // FIRST PART: Create index for all instances
        System.out.println("Creating the index...");
        Fields fields = MultiFields.getFields(dataReader);
        Terms terms = fields.terms(DataFields.INSTANCE.name());
        TermsEnum termsEnum = terms.iterator();
        BytesRef text;
        int read = 0;

        String outputDirectory = args[2];
        Analyzer analyzer = new EnglishAnalyzer();
        IndexWriter writer = makeWriter(outputDirectory, analyzer);

        while((text = termsEnum.next()) != null) {
            read++;
            if (read % TICKS == 0)
                System.out.println(read + " instances processed...");
            // Get the label of the instance
            String q = text.utf8ToString();

            String label = getField(dataSearcher, q, DataFields.LABEL.name());
            String boostString = getField(dataSearcher, q, DataFields.VALUE.name());
            double boost = boostString == null ? 0d : Double.parseDouble(boostString);
            Term term = new Term(DataFields.INSTANCE.name(), q);
            long occurrences = dataReader.docFreq(term);
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
                Query query = new TermQuery(new Term(DataFields.INSTANCE.name(), q));
                TopDocs results = dataSearcher.search(query, dataReader.maxDoc());
                Map<String, Integer> propertiesMap = getAllProperties(results);
                // Add the values to the document
                addPropertiesToDoc(propertiesMap, d);

            }
            writer.addDocument(d);
        }
        System.out.println(read + " instances processed in total");

        System.out.println("Creating caching entries...");
        // SECOND PART: Do the caching for all entries in file
        List<String> needsCachingList;
        String needCachingFile = args[1];
        // Read the file
        try(Stream<String> stream = Files.lines(Paths.get(needCachingFile))) {
            needsCachingList = stream
                    .filter(l -> l.split("\\|\\|").length > 1)
                    .collect(Collectors.toList());
        }
        // Do the cache
        read = 0;
        int totalEntries = needsCachingList.size();
        for(String entry : needsCachingList) {
            read++;
            if(read%10 == 0) {
                System.out.println(read + " / " + totalEntries + " entries processed in total");
            }
            String[] queryParts = entry.split("\\|\\|");
            String type = queryParts[0].split("##")[1];
            String id = type;
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            queryBuilder.add(new TermQuery(new Term(DataFields.INSTANCE.name(), type)), BooleanClause.Occur.MUST);
            for(int i = 1; i < queryParts.length; i++) {
                String po = queryParts[i];
                id = id + "||" + po;
                queryBuilder.add(new TermQuery(new Term(DataFields.PO.name(), po)), BooleanClause.Occur.MUST);
            }
            System.err.println("Creating cache for " + id);
            Query query = queryBuilder.build();
            Document doc = new Document();
            Field idField = new StringField(InstancesFields.Q.name(), id, Field.Store.YES);
            doc.add(idField);
            TopDocs results = dataSearcher.search(query, dataReader.numDocs());
            Map<String, Integer> propertiesMap = getAllProperties(results);
            addPropertiesToDoc(propertiesMap, doc);
            writer.addDocument(doc);
        }

        System.out.println("Caching complete");
        writer.close();
        System.out.println("Finish!");
    }
}
