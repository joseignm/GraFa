package cl.uchile.dcc.facet.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class SearchProperties {

    public static void main(String[] args) throws IOException {
        System.out.println("SearchProperties");
        System.out.println("Search properties from the data to get its Subject");
        System.out.println();

        if(args.length!=1) {
            System.err.println("USAGE: Properties_Indexes_Folder");
            System.exit(0);
        }
        String propDir = args[0];
        final int DOCS_PER_PAGE = 10;

        // open a reader for the directory
        IndexReader propReader = DirectoryReader.open(FSDirectory.open(Paths.get(propDir)));
        // open a searcher over the reader
        IndexSearcher propSearcher = new IndexSearcher(propReader);

        Analyzer analyzer = new EnglishAnalyzer();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        SortField sortField = new SortedNumericSortField(PropertiesFields.OCCURRENCES.name(), SortField.Type.LONG, true);
        Sort sort = new Sort(sortField);

        while (true) {
            System.out.println("Enter search code:");
            System.out.println("0:subject 1:name 2:exit");
            int opCode;
            try {
                opCode = Integer.parseInt(br.readLine());
            } catch (Exception e) {
                continue;
            }
            if(opCode == 2) break;
            if(opCode > 2 || opCode < 0) continue;

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
                            query = new TermQuery(new Term(PropertiesFields.P.name(), line));
                        } else {
                            query = new QueryParser(PropertiesFields.LABEL.name(), analyzer).parse(line);
                        }

                        // get hits
                        results = propSearcher.search(query, DOCS_PER_PAGE, sort);
                        ScoreDoc[] hits = results.scoreDocs;

                        System.out.println("Running query: "+line);
                        System.out.println("Parsed query: "+query);
                        System.out.println("Matching documents: "+results.totalHits);
                        System.out.println("Showing top "+DOCS_PER_PAGE+" results");

                        for(ScoreDoc hit : hits) {
                            String subject;
                            String label;
                            String occurrences;
                            Document doc = propSearcher.doc(hit.doc);
                            subject = doc.get(PropertiesFields.P.name());
                            label = doc.get(PropertiesFields.LABEL.name());
                            occurrences = doc.get(PropertiesFields.NUMBER.name());
                            System.out.println(subject+"\t"+label+"\t"+occurrences);
                            IndexableField[] values = doc.getFields(PropertiesFields.VALUES.name());
                            System.out.print("\t");
                            for(IndexableField value : values) {
                                String o = value.stringValue();
                                System.out.print(o + " ");
                            }
                            System.out.println();
                            System.out.println("_______________________________________");
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
