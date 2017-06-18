package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.InstancesFields;
import cl.uchile.dcc.facet.core.ScoreBoostsOperator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class InstancesServlet extends HttpServlet {

    private static final String instancesDir = "/home/jmoreno/bin/instances_v2.8";
    private static final int DOCS_PER_PAGE = 20;
    private IndexSearcher searcher;
    private QueryParser queryParser;

    @Override
    public void init() throws ServletException {
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(instancesDir)));
            // open a searcher over the reader
            searcher = new IndexSearcher(reader);
            Analyzer analyzer = new EnglishAnalyzer();
            queryParser = new QueryParser(InstancesFields.LABEL.name(), analyzer);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open Lucene folder");
            throw new ServletException();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String keyword = request.getParameter("keyword");
            if(keyword == null) {
                out.println("No keyword provided");
                return;
            }
            if(queryParser == null) {
                out.println("Fatal: Query Parser is null. Is init failing?");
            }
            Query baseQuery = queryParser.parse(keyword);
            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(InstancesFields.BOOST.name());
            boostsSource = DoubleValuesSource.function(boostsSource, new ScoreBoostsOperator());
            boostsSource = DoubleValuesSource.scoringFunction(boostsSource, (Double src, Double score) -> src*score);

            Query query = new FunctionScoreQuery(baseQuery, boostsSource);
            TopDocs results = searcher.search(query, DOCS_PER_PAGE);
            ScoreDoc[] hits = results.scoreDocs;
            for(ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                String label = doc.get(InstancesFields.LABEL.name());
                String q = doc.get(InstancesFields.Q.name());
                String occurrences = doc.get(InstancesFields.NUMBER.name());
                out.println("<option code='"+q+"'>"+label+" ("+occurrences+")</option>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

}
