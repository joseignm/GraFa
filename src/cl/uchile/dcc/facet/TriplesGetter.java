package cl.uchile.dcc.facet;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.Set;

import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.Statement;

public class TriplesGetter {

    public static void main(String[] args) throws IOException {
        if(args.length!=2) {
            System.err.println("usage input baseIRI");
            System.exit(0);
        }

        InputStream in = new FileInputStream(args[0]);
        if(args[0].endsWith(".gz")){
            in = new GZIPInputStream(in);
        }

        RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES);
        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));

        try {
            parser.parse(in, args[1]);
        } catch (Exception e) {
            throw new IOException();
        } finally {
            in.close();
        }

        for(Statement s : model) {
            System.out.println(s.getSubject());
            System.out.println(s.getPredicate());
            System.out.println(s.getObject());
            break;
        }

    }
}
