package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        final int TICKS = 1000000;

        System.out.println("RankData");

        if(args.length != 2) {
            System.out.println("USAGE Lucene_Index_Dir Output_GZip");
            System.exit(0);
        }

        String dataDirectory = args[0];
        String output = args[1];

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
        for(int i=0; i<graphLength; i++) {
            if(i%TICKS == 0) {
                System.err.println(i + "/" + graphLength + " nodes processed");
            }
            Document doc = reader.document(i);
            IndexableField[] pos = doc.getFields(DataFields.PO.name());
            List<Integer> outLinksList = new ArrayList<>();
            for(IndexableField po : pos) {
                String reference = po.stringValue().split("##")[1];
                if(map.containsKey(reference)) {
                    outLinksList.add(map.get(reference));
                }
            }
            if(!outLinksList.isEmpty()) {
                graph[i] = outLinksList.stream().mapToInt(a -> a).toArray();
            }
        }
        reader.close();
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
    }
}
