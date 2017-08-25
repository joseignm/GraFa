package cl.uchile.dcc.facet.core;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class PropertiesStatistics {

    public static void main(String[] args) throws IOException {

        if(args.length != 2) {
            System.err.println("USAGE: Properties_Indexes_Folder Output_File");
            System.exit(0);
        }
        String propDir = args[0];
        String statisticsFile = args[1];

        PrintWriter pw = new PrintWriter(new FileWriter(statisticsFile));
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(propDir)));
        int size = reader.maxDoc();

        pw.println("P,Q?,|s|,m',|o|");

        for(int i = 0; i<size; i++) {
            Document doc = reader.document(i);
            String pCode = doc.get(PropertiesFields.P.name());
            String occurrences = doc.get(PropertiesFields.NUMBER.name());
            String mPrime = doc.get(PropertiesFields.M_PRIME.name());
            String valuesCard = doc.get(PropertiesFields.VALUES_CARD.name());
            String hasQValues = mPrime == null ? "N" : "Y";
            pw.println(pCode+","+hasQValues+","+occurrences+","+mPrime+","+valuesCard);
        }

        pw.close();
        reader.close();
    }
}
