package cl.uchile.dcc.facet.core;

import java.io.*;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFFormat;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;

public class IndexData extends Indexer {

    public static void main(String[] args) throws IOException {
        System.out.println("IndexData");
        System.out.println("Reads a WikiData dump (NT format) and creates a index with the most relevant data");
        System.out.println();

        if(args.length!=2) {
            System.out.println("USAGE: Input_NT_file Output_Directory");
            System.exit(0);
        }

        long startTime = System.currentTimeMillis();

        String filename = args[0];
        String outputDir = args[1];

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = makeWriter(outputDir, analyzer);

        InputStream in = new FileInputStream(filename);
        if(filename.endsWith(".gz")){
            System.err.println("Input file is gzipped.");
            in = new GZIPInputStream(in);
        }
        Reader reader = new InputStreamReader(in, "UTF-8");

        RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES);
        IndexHandler handler = new IndexHandler(writer);
        parser.setRDFHandler(handler);

        System.err.println("Parsing file...");
        System.err.println("This may take a while...");
        try {
            parser.parse(reader, "");
        } catch (Exception e) {
            throw new IOException();
        } finally {
            in.close();
        }
        handler.finish();

        long totalTime = System.currentTimeMillis() - startTime;
        System.err.println("Total time: " + totalTime + " ms");
        System.err.println("Parsing completed!");
    }
}
