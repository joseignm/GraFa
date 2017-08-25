package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class SearchInstances {

    public static void main(String[] args) throws IOException {
        System.out.println("SearchInstances");
        System.out.println("Search instances from the data to get its Subject");
        System.out.println();

        if(args.length!=1) {
            System.err.println("USAGE: Instances_Indexes_Folder");
            System.exit(0);
        }
        String insDir = args[0];
        final int DOCS_PER_PAGE = 10;

        // open a reader for the directory
        IndexReader insReader = DirectoryReader.open(FSDirectory.open(Paths.get(insDir)));
        // open a searcher over the reader
        IndexSearcher insSearcher = new IndexSearcher(insReader);

        Analyzer analyzer = new EnglishAnalyzer();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        SortField sortField = new SortedNumericSortField(InstancesFields.OCCURRENCES.name(), SortField.Type.LONG, true);
        Sort sort = new Sort(sortField);
        System.out.println("Total instances: " + insReader.numDocs());

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
                            query = new WildcardQuery(new Term(InstancesFields.Q.name(), line));
                        } else if(opCode == 1) {
                            query = new QueryParser(InstancesFields.LABEL.name(), analyzer).parse(line);
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
                            subject = doc.get(InstancesFields.Q.name());
                            label = doc.get(InstancesFields.LABEL.name());
                            occurrences = doc.get(InstancesFields.NUMBER.name());
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
