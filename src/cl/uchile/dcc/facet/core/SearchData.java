package cl.uchile.dcc.facet.core;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

public class SearchData {

    private static final HashMap<String,Float> BOOSTS = new HashMap<>();
    static {
        BOOSTS.put(DataFields.ALT_LABEL.name(), 2f);
        BOOSTS.put(DataFields.DESCRIPTION.name(), 1f);
        BOOSTS.put(DataFields.LABEL.name(), 5f);
    }

    private static final int DOCS_PER_PAGE  = 10;

    public static void main(String[] args) throws IOException {
        System.out.println("SearchData");
        System.out.println("Search data from the index previously created");
        System.out.println();

        if(args.length!=1) {
            System.err.println("USAGE: Lucene_Indexes_Folder");
            System.exit(0);
        }
        String in = args[0];

        // open a reader for the directory
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(in)));
        // open a searcher over the reader
        IndexSearcher searcher = new IndexSearcher(reader);
        // use the same analyser as the build
        Analyzer analyzer = new EnglishAnalyzer();

        // this accepts queries/searches and parses them into
        // searches over the index
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                new String[] {DataFields.LABEL.name(),
                        DataFields.DESCRIPTION.name(),
                        DataFields.ALT_LABEL.name()},
                analyzer, BOOSTS);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));

        while (true) {
            System.out.println(reader.numDocs() + " documents indexed.");
            System.out.println("Enter search code:");
            System.out.println("0:subject 1:keyword 2:property 3:property-object 4:exit");
            int opCode;
            try {
                opCode = Integer.parseInt(br.readLine());
            } catch (Exception e) {
                continue;
            }
            if(opCode == 4) break;
            if(opCode > 4 || opCode < 0) continue;

            System.out.println("Enter a keyword search phrase:");

            String line = br.readLine();
            if(line!=null){
                line = line.trim();
                if(!line.isEmpty()){
                    try{
                        // parse query
                        Query query;
                        Term term;
                        switch(opCode) {
                            case 0:
                                term = new Term(DataFields.SUBJECT.name(), line);
                                query = new TermQuery(term);
                                break;
                            case 1:
                                query = queryParser.parse(line);
                                break;
                            case 2:
                                term = new Term(DataFields.P.name(), line);
                                query = new TermQuery(term);
                                break;
                            default:
                                term = new Term(DataFields.PO.name(), line);
                                query = new WildcardQuery(term);
                        }

                        // get hits
                        TopDocs results = searcher.search(query, DOCS_PER_PAGE);
                        ScoreDoc[] hits = results.scoreDocs;

                        System.out.println("Running query: "+line);
                        System.out.println("Parsed query: "+query);
                        System.out.println("Matching documents: "+results.totalHits);
                        System.out.println("Showing top "+DOCS_PER_PAGE+" results");
                        System.out.println();
                        System.out.println();

                        for(ScoreDoc hit : hits) {
                            Document doc = searcher.doc(hit.doc);
                            String subject = doc.get(DataFields.SUBJECT.name());
                            String label = doc.get(DataFields.LABEL.name());
                            String desc = doc.get(DataFields.DESCRIPTION.name());
                            String alt = doc.get(DataFields.ALT_LABEL.name());
                            IndexableField[] instances = doc.getFields(DataFields.INSTANCE.name());
                            IndexableField[] pos = doc.getFields(DataFields.PO.name());
                            String boost = doc.get(DataFields.VALUE.name());
                            System.out.println(subject + " - " + label);
                            System.out.println(desc);
                            System.out.println(alt);
                            System.out.println("Instances:");
                            for(IndexableField instance : instances) {
                                System.out.println("\t" + instance.stringValue());
                            }
                            System.out.println("Property ## Value:");
                            for(IndexableField po : pos) {
                                System.out.println("\t" + po.stringValue());
                            }
                            System.out.println("Score: " + hit.score);
                            System.out.println("PageRank: " + boost);
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
