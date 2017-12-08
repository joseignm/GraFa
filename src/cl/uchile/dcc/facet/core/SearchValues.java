package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class SearchValues {

    public static void main(String[] args) throws IOException {
        System.out.println("SearchValues");
        System.out.println("Search values for the properties from the data");
        System.out.println();

        if(args.length!=1) {
            System.err.println("USAGE: Values_Index");
            System.exit(0);
        }
        String valDir = args[0];
        final int DOCS_PER_PAGE = 10;

        // open a reader for the directory
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(valDir)));
        // open a searcher over the reader
        IndexSearcher searcher = new IndexSearcher(reader);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        System.out.println("Total instances: " + reader.numDocs());

        while (true) {
            System.out.println("Enter operation:");
            System.out.println("0:search 1:exit");
            int opCode;
            try {
                opCode = Integer.parseInt(br.readLine());
            } catch (Exception e) {
                continue;
            }
            if(opCode == 1) break;
            if(opCode != 0) continue;

            System.out.println("Enter a regex to search:");

            String line = br.readLine();
            if(line != null) {
                line = line.trim();
                if(!line.isEmpty()){
                    try{
                        // parse query
                        Query query = new WildcardQuery(new Term(ValuesFields.BASE.name(), line));
                        // get hits
                        TopDocs results = searcher.search(query, DOCS_PER_PAGE);
                        ScoreDoc[] hits = results.scoreDocs;

                        System.out.println("Running query: "+line);
                        System.out.println("Parsed query: "+query);
                        System.out.println("Matching documents: "+results.totalHits);
                        System.out.println("Showing top "+DOCS_PER_PAGE+" results");

                        for(int i=0; i<hits.length; i++) {
                            Document doc = searcher.doc(hits[i].doc);
                            String base = doc.get(ValuesFields.BASE.name());
                            String values = doc.get(ValuesFields.VALUES.name());
                            String example = values.substring(0, Math.min(100, values.length()));

                            System.out.println((i+1)+" "+base+"\t"+example);
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
