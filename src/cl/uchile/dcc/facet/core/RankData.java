package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RankData {

    private static double[] rankGraph(int[][] graph) {
        final int ITERATIONS = 25;
        final double D = 0.85d;
        int nodes = graph.length;

        double[] oldRanks = new double[nodes];

        double initial = 1d / nodes;

        for(int i=0; i<nodes; i++) {
            oldRanks[i] = initial;
        }

        double[] ranks = null;
        for(int i=0; i<ITERATIONS; i++) {
            double noLinkRank = 0d;
            ranks = new double[nodes];

            for(int j=0; j<nodes; j++) {
                if(graph[j] != null) {
                    int[] out = graph[j];
                    double share = oldRanks[j] * D / out.length;
                    for(int o : out) {
                        ranks[o] += share;
                    }
                } else {
                    noLinkRank += oldRanks[j];
                }
            }

            double shareNoLink = (noLinkRank*D) / nodes;

            double shareMinusD = (1d - D) / nodes;

            double weakRank = shareNoLink + shareMinusD;

            double sum = 0d;
            double e = 0d;

            for(int k=0; k<nodes; k++) {
                ranks[k] += weakRank;
                sum += ranks[k];
                e += Math.abs(oldRanks[k] - ranks[k]);
            }

            System.err.println("Iteration " + i + " finished! Sum "+ sum + " Epsilon "+ e);

            System.arraycopy(ranks, 0, oldRanks, 0, nodes);
        }

        return ranks;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("RankData");

        if(args.length != 3) {
            System.out.println("USAGE Lucene_Index_Dir Input_NT_File Output_GZip");
            System.exit(0);
        }

        long startTime = System.currentTimeMillis();
        String dataDirectory = args[0];
        String triplesFile = args[1];
        String output = args[2];

        // INIT INDEX READER
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(dataDirectory)));
        int graphLength = reader.maxDoc();
        Map<String, Integer> map = new HashMap<>();
        System.err.println("Graph size: " + graphLength);

        // CREATE MAP TO TRANSLATE SUBJECT TO ID
        System.err.println("Creating map...");
        for(int i=0; i<graphLength; i++) {
            Document doc = reader.document(i);
            String key = doc.get(DataFields.SUBJECT.name());
            map.put(key, i);
        }
        System.err.println("Map created successfully!");

        // CREATE GRAPH IN MEMORY
        System.err.println("Creating graph in memory...");
        System.err.println("This may take a while...");
        int[][] graph = new int[graphLength][];

        InputStream in = new FileInputStream(triplesFile);
        if(triplesFile.endsWith(".gz")){
            System.err.println("Input file is gzipped.");
            in = new GZIPInputStream(in);
        }
        Reader isr = new InputStreamReader(in, "UTF-8");

        RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES);
        RankHandler handler = new RankHandler(graph, map);
        parser.setRDFHandler(handler);

        System.err.println("Parsing file...");
        System.err.println("This may take a while...");
        try {
            parser.parse(isr, "");
        } catch (Exception e) {
            throw new IOException();
        } finally {
            in.close();
        }
        handler.finish();
        System.err.println("Graph loaded");

        // RANK GRAPH
        System.err.println("Ranking graph...");
        double[] ranks = rankGraph(graph);
        System.err.println("Ranking complete!");

        // WRITING TO OUTPUT
        OutputStream os = new FileOutputStream(output);
        os = new GZIPOutputStream(os);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
        System.err.println("Writing ranks to " + output);

        int written;
        for(written=0; written<ranks.length; written++){
            pw.println(written+"\t"+ranks[written]);
        }
        System.err.println("Finished writing ranks! Wrote "+written+" ranks.");

        pw.close();

        long totalTime = System.currentTimeMillis() - startTime;
        System.err.println("Total time: " + totalTime + " ms");
    }
}
