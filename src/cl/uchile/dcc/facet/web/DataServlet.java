package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

public abstract class DataServlet extends HttpServlet {

    IndexSearcher searcher;
    IndexReader reader;
    MultiFieldQueryParser queryParser;

    String getLabelFromSubject(String subject) throws IOException {
        TopDocs result = searcher.search(new TermQuery(new Term(DataFields.SUBJECT.name(), subject)), 1);
        if(result.totalHits < 1) {
            return subject;
        }
        Document doc = searcher.doc(result.scoreDocs[0].doc);
        return doc.get(DataFields.LABEL.name());
    }

    @Override
    public void init() throws ServletException {
        try {
            String LuceneDir = getServletContext().getInitParameter("LuceneDir");
            // open a reader for the directory
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(LuceneDir)));
            // open a searcher over the reader
            searcher = new IndexSearcher(reader);
            Analyzer analyzer = new EnglishAnalyzer();
            // Boosts for query parser
            HashMap<String,Float> boosts = new HashMap<>();
            boosts.put(DataFields.ALT_LABEL.name(), 2f);
            boosts.put(DataFields.DESCRIPTION.name(), 1f);
            boosts.put(DataFields.LABEL.name(), 10f);
            // this accepts queries/searches and parses them into
            // searches over the index
            queryParser = new MultiFieldQueryParser(
                    new String[]{DataFields.LABEL.name(),
                            DataFields.DESCRIPTION.name(),
                            DataFields.ALT_LABEL.name()},
                    analyzer, boosts);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open Lucene folder");
            throw new ServletException();
        }
    }
}
