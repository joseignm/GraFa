package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;

public class SearchInstances {

    public static void main(String[] args) throws IOException {
        System.out.println("SearchInstances");
        System.out.println("Search instances from the data to get its Subject");
        System.out.println();

        if(args.length!=2) {
            System.err.println("USAGE: Instances_Indexes_Folder Language");
            System.exit(0);
        }
        String insDir = args[0];
        String lang = args[1];
        final int DOCS_PER_PAGE = 10;

        String labelFieldName = InstancesFields.LABEL.name() + "-" + lang;
        String altLabelFieldName = InstancesFields.ALT_LABEL.name() + "-" + lang;

        // open a reader for the directory
        IndexReader insReader = DirectoryReader.open(FSDirectory.open(Paths.get(insDir)));
        // open a searcher over the reader
        IndexSearcher insSearcher = new IndexSearcher(insReader);

        Analyzer analyzer = new StandardAnalyzer();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        SortField sortField = new SortedNumericSortField(InstancesFields.FREQUENCY.name(), SortField.Type.LONG, true);
        Sort sort = new Sort(sortField);
        System.out.println("Total instances: " + insReader.numDocs());

        HashMap<String,Float> boostsMap = new HashMap<>();
        boostsMap.put(altLabelFieldName, 2f);
        boostsMap.put(labelFieldName, 5f);

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                new String[] {labelFieldName, altLabelFieldName},
                analyzer, boostsMap);

        while (true) {
            System.out.println("Enter search code:");
            System.out.println("0:subject 1:name 2:all 3:exit");
            int opCode;
            try {
                opCode = Integer.parseInt(br.readLine());
            } catch (Exception e) {
                continue;
            }
            if(opCode == 3) break;
            if(opCode > 3 || opCode < 0) continue;

            System.out.println("Enter a keyword search phrase:");

            String line = br.readLine();
            if(line != null) {
                line = line.trim();
                if(!line.isEmpty()){
                    try{
                        // parse query
                        Query query;
                        TopDocs results;
                        if(opCode == 0) {
                            query = new WildcardQuery(new Term(InstancesFields.ID.name(), line));
                        } else if(opCode == 1) {
                            query = queryParser.parse(line);
                        } else {
                            query = new MatchAllDocsQuery();
                        }

                        // get hits
                        results = insSearcher.search(query, DOCS_PER_PAGE, sort);
                        ScoreDoc[] hits = results.scoreDocs;

                        System.out.println("Running query: "+line);
                        System.out.println("Parsed query: "+query);
                        System.out.println("Matching documents: "+results.totalHits);
                        System.out.println("Showing top "+DOCS_PER_PAGE+" results");

                        for(int i=0; i<hits.length; i++) {
                            String subject;
                            String label;
                            String occurrences;
                            Document doc = insSearcher.doc(hits[i].doc);
                            subject = doc.get(InstancesFields.ID.name());
                            label = doc.get(labelFieldName);
                            occurrences = doc.get(InstancesFields.FREQ_STORED.name());
                            int cache = doc.getFields(InstancesFields.PROPERTY.name()).length;

                            System.out.println((i+1)+" "+subject+"\t"+label+"\t"+occurrences + "\t" + cache);
                            System.out.println();
                        }
                    } catch(Exception e) {
                        System.err.println("Error with query '"+line+"'");
                        e.printStackTrace();
                    }
                }
            }

        }
    }

}
