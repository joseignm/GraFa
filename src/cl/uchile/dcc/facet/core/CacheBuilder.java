package cl.uchile.dcc.facet.core;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class CacheBuilder {

    private static final int TICKS = 100000;
    private static final int M_PRIME = 50000;

    private static List<String> makePOList(String directory) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(directory)));
        Fields fields = MultiFields.getFields(reader);
        Terms poTerms = fields.terms(DataFields.PO.name());
        TermsEnum poIterator = poTerms.iterator();
        BytesRef text;
        int read = 0;
        while((text = poIterator.next()) != null) {
            read++;
            if(read%TICKS == 0)
                System.out.println(read+" PO values processed...");
            String poCode = text.utf8ToString();
            String[] poSplit = poCode.split("##");
            String value = poSplit[1];
            if(!value.startsWith("Q")) continue;
            Term poTerm = new Term(DataFields.PO.name(), poCode);
            int frequency = reader.docFreq(poTerm);
            if(frequency > M_PRIME)
                map.put(poCode, frequency);
        }
        
        System.out.println(read+" PO values processed in total.");
        return map.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("CacheBuilder");
        System.out.println("Makes a list of all the instances that need a cache");
        System.out.println();

        if(args.length!=3) {
            System.out.println("USAGE: NT_file Index_Directory Output_List");
            System.exit(0);
        }

        String ntFilename = args[0];
        String indexDir = args[1];
        String outputFile = args[2];

        long startTime = System.currentTimeMillis();
        List<String> poList = makePOList(indexDir);
        System.err.println("PO List created!");
        System.err.println("INFO: Length of PO List = " + poList.size());

        InputStream in = new FileInputStream(ntFilename);
        if(ntFilename.endsWith(".gz")){
            System.err.println("Input file is gzipped.");
            in = new GZIPInputStream(in);
        }
        Reader reader = new InputStreamReader(in, "UTF-8");

        RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES);
        CacheHandler handler = new CacheHandler(poList);
        parser.setRDFHandler(handler);

        System.err.println("Reading NT file...");
        System.err.println("This may take a while...");
        try {
            parser.parse(reader, "");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException();
        } finally {
            in.close();
        }
        List<String> needsCachingList = handler.getResults();
        long totalTime = ((System.currentTimeMillis() - startTime) / 1000 / 60) + 1;
        System.err.println("List created!");

        System.err.println("Writing results to output file");
        PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
        needsCachingList.forEach(pw::println);
        pw.close();
        System.err.println("Complete!");
        System.err.println("Total time = " + totalTime + " min");
    }
}
