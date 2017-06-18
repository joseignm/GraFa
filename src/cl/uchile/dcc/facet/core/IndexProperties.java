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
import java.util.*;

public class IndexProperties extends Indexer {

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
        final int TICKS = 100;
        String in = args[0];
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(in)));
        Fields fields = MultiFields.getFields(reader);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new EnglishAnalyzer();
        BytesRef text;

        // SECOND PART: Create index for all properties
        Terms terms = fields.terms(DataFields.P.name());
        TermsEnum termsEnum = terms.iterator();
        String propDir = args[1];
        IndexWriter writer = makeWriter(propDir, analyzer);
        int read = 0;

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
            d.add(pField);
            d.add(labelField);
            d.add(occurrencesField);
            d.add(number);
            // Cache the values except instanceOf
            if(!p.equals("P31")) {
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
