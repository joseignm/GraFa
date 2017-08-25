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

    //private static final String instancesDir = "/home/jmoreno/bin/instances_v3.1";
    private static final int DOCS_PER_PAGE = 20;
    private IndexSearcher searcher;
    private QueryParser queryParser;

    @Override
    public void init() throws ServletException {
        try {
            String instancesDir = getServletContext().getInitParameter("InstancesDirectory");
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

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            Query baseAutoCompleteQuery = null;
            if(keyword.matches("[A-Za-z ]*"))
                baseAutoCompleteQuery = queryParser.parse(keyword+"*");
            Query baseLiteralQuery = queryParser.parse(keyword);

            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(InstancesFields.BOOST.name());
            boostsSource = DoubleValuesSource.function(boostsSource, new ScoreBoostsOperator());
            boostsSource = DoubleValuesSource.scoringFunction(boostsSource, (Double src, Double score) -> src*score);

            if(baseAutoCompleteQuery != null)
                queryBuilder.add(new FunctionScoreQuery(baseAutoCompleteQuery, boostsSource), BooleanClause.Occur.SHOULD);
            queryBuilder.add(new FunctionScoreQuery(baseLiteralQuery, boostsSource), BooleanClause.Occur.SHOULD);
            Query query = queryBuilder.build();

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
