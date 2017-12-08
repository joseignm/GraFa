package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import javax.json.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class IndexValues extends Indexer {

    public static void main(String[] args) throws IOException {
        System.out.println("IndexValues");
        System.out.println("Creates a new index using two previous indexes");
        System.out.println("The new index contains all values for cached instances");
        System.out.println();
        if (args.length != 3) {
            System.out.println("USAGE: DataIndex InstancesIndex OutputIndex");
            System.exit(0);
        }

        // INIT: Make reader, searcher and writer
        System.out.println("Init...");
        final int TICKS = 100;

        String dataDirectory = args[0];
        IndexReader dataReader = DirectoryReader.open(FSDirectory.open(Paths.get(dataDirectory)));
        IndexSearcher dataSearcher = new IndexSearcher(dataReader);

        String propertyDirectory = args[1];
        IndexReader instancesReader = DirectoryReader.open(FSDirectory.open(Paths.get(propertyDirectory)));

        IndexWriter writer = makeWriter(args[2], new StandardAnalyzer());

        Properties configFile = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        configFile.load(input);
        String[] languages = configFile.getProperty("languages").split(",");
        String entityPrefix = configFile.getProperty("entityPrefix");

        long start = System.currentTimeMillis();
        for(int i=0; i<instancesReader.maxDoc(); i++) {
            if(i%TICKS == 0) {
                System.err.println(i+" instances processed...");
            }

            Document instancesDoc = instancesReader.document(i);
            IndexableField[] propertiesList = instancesDoc.getFields(InstancesFields.PROPERTY.name());
            if(propertiesList == null || propertiesList.length == 0) continue;

            // Cache all values for a property
            String instanceBase = instancesDoc.get(InstancesFields.ID.name());
            System.err.println(instanceBase+" need to cache all its properties");

            String[] queryParts = instanceBase.split("\\|\\|");
            if(queryParts.length == 0) {
                System.err.println("ERROR: No Q value but properties saved.");
                continue;
            }
            List<Query> queries = new ArrayList<>();
            queries.add(new TermQuery(new Term(DataFields.TYPE.name(), queryParts[0])));

            if(queryParts.length > 1) {
                for(int j=1; j<queryParts.length; j++) {
                    queries.add(new TermQuery(new Term(DataFields.PO.name(), queryParts[j])));
                }
            }

            for(IndexableField rawProperty : propertiesList) {
                String raw = rawProperty.stringValue();
                String property = raw.split("##")[0];

                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                for(Query query : queries) {
                    queryBuilder.add(query, BooleanClause.Occur.MUST);
                }
                queryBuilder.add(new TermQuery(new Term(DataFields.PROPERTY.name(), property)), BooleanClause.Occur.MUST);
                Query finalQuery = queryBuilder.build();

                Document valuesDocument = new Document();
                String baseCode = instanceBase + "||" + property;
                Field baseField = new StringField(ValuesFields.BASE.name(), baseCode, Field.Store.YES);
                valuesDocument.add(baseField);

                TopDocs results = dataSearcher.search(finalQuery, dataReader.numDocs());
                ScoreDoc[] hits = results.scoreDocs;
                List<String> possibleValues = new ArrayList<>();
                for(ScoreDoc hit : hits) {
                    Document searchDoc = dataSearcher.doc(hit.doc);
                    IndexableField[] pos = searchDoc.getFields(DataFields.PO.name());
                    for(IndexableField po : pos) {
                        String[] rawPO = po.stringValue().split("##");
                        if(!property.equals(rawPO[0])) continue;
                        String value = rawPO[1];
                        if(!value.startsWith(entityPrefix)) continue;
                        if(!possibleValues.contains(value)) possibleValues.add(value);
                    }
                }

                JsonBuilderFactory factory = Json.createBuilderFactory(null);
                JsonArrayBuilder main = factory.createArrayBuilder();
                for(String value : possibleValues) {
                    JsonObjectBuilder valueObject = factory.createObjectBuilder();
                    valueObject.add("id", property + "##" + value);
                    String pr = getField(dataSearcher, value, DataFields.RANK_STORED.name());
                    if(pr == null) continue;
                    valueObject.add("rank", Double.parseDouble(pr));
                    for(String language : languages) {
                        String label = getField(dataSearcher, value, DataFields.LABEL.name()+"-"+language);
                        if (label == null) label = value;
                        valueObject.add("name-"+language, label);
                    }
                    main.add(valueObject);
                }
                String jsonArray = main.build().toString();
                Field dataField = new StoredField(ValuesFields.VALUES.name(), jsonArray);
                valuesDocument.add(dataField);
                writer.addDocument(valuesDocument);
            }

            System.err.println(instanceBase + " finished!");
        }
        long totalTime = System.currentTimeMillis() - start;
        long minutes = totalTime / 1000 / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        System.err.println("Total time: " + hours + " h " + minutes + " min");
        writer.close();
        System.err.println("Complete!");
    }
}
