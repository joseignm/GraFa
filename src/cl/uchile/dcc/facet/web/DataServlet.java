package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.nio.file.Paths;

public abstract class DataServlet extends HttpServlet {

    IndexSearcher searcher;
    IndexReader reader;

    String getLabelFromSubject(String subject, String lang) throws IOException {
        TopDocs result = searcher.search(new TermQuery(new Term(DataFields.SUBJECT.name(), subject)), 1);
        if(result.totalHits < 1) {
            return subject;
        }
        Document doc = searcher.doc(result.scoreDocs[0].doc);
        String label = doc.get(DataFields.LABEL.name() +"-"+ lang);
        if(label == null) label = subject;
        return label;
    }

    @Override
    public void init() throws ServletException {
        try {
            String LuceneDir = getServletContext().getInitParameter("IndexDirectory");
            // open a reader for the directory
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(LuceneDir)));
            // open a searcher over the reader
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open Lucene folder");
            throw new ServletException();
        }
    }
}
