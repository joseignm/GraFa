package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.*;

public class IndexProperties extends Indexer {

    private static class Property {
        String p;
        String o;
        long n;

        private Property(String p, String o, long n) {
            this.p = p; this.o = o; this.n = n;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("IndexProperties");
        System.out.println("Creates a new index using the previous one");
        System.out.println("The new index contains all properties");
        System.out.println();

        if(args.length != 2) {
            System.out.println("USAGE: Lucene_Index_Folder Index_Out_Dir");
            System.exit(0);
        }

        // INIT: Make reader and searcher
        System.out.println("Init...");
        final int TICKS = 1000;
        String in = args[0];
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(in)));
        Fields fields = MultiFields.getFields(reader);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new EnglishAnalyzer();
        BytesRef text;
        int read = 0;

        // READ Properties file
        Properties properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);

        // FIRST PART: Compute m'
        System.out.println("Count frequency for all PO values...");
        long startTime = System.currentTimeMillis();
        List<Property> poList = new ArrayList<>();
        Terms poTerms = fields.terms(DataFields.PO.name());
        TermsEnum poIterator = poTerms.iterator();
        while((text = poIterator.next()) != null) {
            read++;
            if(read%TICKS == 0)
                System.out.println(read+" PO values processed...");
            String poCode = text.utf8ToString();
            String[] poSplit = poCode.split("##");
            String value = poSplit[1];
            if(!value.startsWith("Q")) continue;
            Term poTerm = new Term(DataFields.PO.name(), poCode);
            poList.add(new Property(poSplit[0], value, reader.docFreq(poTerm)));
        }

        System.out.println("Grouping by property...");
        Map<String, List<Property>> poMap = poList.stream().collect(Collectors.groupingBy(p -> p.p));

        Map<String, Property> mValues = new HashMap<>();
        Map<String, Integer> valuesCardinality = new HashMap<>();

        System.out.println("Obtaining max value...");
        read = 0;
        for(Map.Entry<String, List<Property>> entry : poMap.entrySet()) {
            read++;
            if(read%TICKS == 0)
                System.out.println(read+" properties processed...");
            valuesCardinality.put(entry.getKey(), entry.getValue().size());
            long maxFrequency = 0L;
            Property maxProperty = null;
            List<Property> poSublist = entry.getValue();
            for(Property poEntry : poSublist) {
                if(poEntry.n > maxFrequency) {
                    maxFrequency = poEntry.n;
                    maxProperty = poEntry;
                }
            }
            mValues.put(entry.getKey(), maxProperty);
        }
        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime) / 1000 ;
        System.out.println("Computing m' - Total time: " + totalTime + " s");

        // SECOND PART: Create index for all properties
        Terms terms = fields.terms(DataFields.P.name());
        TermsEnum termsEnum = terms.iterator();
        String propDir = args[1];
        IndexWriter writer = makeWriter(propDir, analyzer);
        read = 0;

        System.out.println("Creating the index");
        while((text = termsEnum.next()) != null) {
            read++;
            if(read%TICKS == 0)
                System.out.println(read+" properties processed...");
            // Get the label of the property
            String p = text.utf8ToString();
            String label = getField(searcher, p, DataFields.LABEL.name());
            Term term = new Term(DataFields.P.name(), p);
            long occurrences = reader.docFreq(term);
            if(label == null) continue;
            // Create document with P and Label
            Document d = new Document();
            Field pField = new StringField(PropertiesFields.P.name(), p, Field.Store.YES);
            Field labelField = new TextField(PropertiesFields.LABEL.name(), label, Field.Store.YES);
            Field occurrencesField = new NumericDocValuesField(PropertiesFields.OCCURRENCES.name(), occurrences);
            Field number = new StoredField(PropertiesFields.NUMBER.name(), occurrences);

            Property thisProperty = mValues.get(p);
            if (thisProperty != null) {
                Field mPrime = new StoredField(PropertiesFields.M_PRIME.name(), thisProperty.n);
                Field topValue = new StringField(PropertiesFields.TOP_VALUE.name(), thisProperty.o, Field.Store.YES);
                d.add(mPrime);
                d.add(topValue);
            }

            Integer cardinality = valuesCardinality.get(p);
            int valuesCard = cardinality == null ? 0 : cardinality;
            Field cardField = new StoredField(PropertiesFields.VALUES_CARD.name(), valuesCard);
            d.add(cardField);
            d.add(pField);
            d.add(labelField);
            d.add(occurrencesField);
            d.add(number);
            // Cache the values except instanceOf
            String instanceOfCode = properties.getProperty("instanceOf");
            if(!p.equals(instanceOfCode)) {
                Map<String, Integer> valuesMap = new HashMap<>();
                Query query = new TermQuery(new Term(DataFields.P.name(), p));
                TopDocs results = searcher.search(query, reader.maxDoc());
                ScoreDoc[] hits = results.scoreDocs;
                for(ScoreDoc hit : hits) {
                    Document doc = searcher.doc(hit.doc);
                    IndexableField[] pos = doc.getFields(DataFields.PO.name());
                    for(IndexableField po : pos) {
                        String raw = po.stringValue();
                        if(!raw.startsWith(p)) continue;
                        String[] rawSplit = raw.split("##");
                        String value = rawSplit[1];
                        if(!value.startsWith("Q")) continue;
                        if(valuesMap.containsKey(value)) {
                            valuesMap.replace(value, valuesMap.get(value)+1);
                        } else {
                            valuesMap.put(value, 1);
                        }
                    }
                }
                // Add the values to the document
                List<Map.Entry<String, Integer>> valuesList = new ArrayList<>(valuesMap.entrySet());
                Collections.sort(valuesList,
                        (Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> o2.getValue().compareTo(o1.getValue()));
                for(Map.Entry<String, Integer> entry : valuesList) {
                    if(entry.getValue() < 5) break;
                    Field valueField = new StringField(PropertiesFields.VALUES.name(), entry.getKey(), Field.Store.YES);
                    d.add(valueField);
                }
            }
            writer.addDocument(d);
        }
        System.out.println(read+" properties processed in total");
        writer.close();
        System.out.println("Complete!");
    }
}
