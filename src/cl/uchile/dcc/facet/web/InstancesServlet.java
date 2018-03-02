package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.InstancesFields;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;

public class InstancesServlet extends HttpServlet {

    private static final int DOCS_PER_PAGE = 15;
    private IndexSearcher searcher;

    @Override
    public void init() throws ServletException {
        try {
            String instancesDir = getServletContext().getInitParameter("InstancesDirectory");
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(instancesDir)));
            // open a searcher over the reader
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            System.err.println("FATAL: Cannot open Lucene folder");
            throw new ServletException();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        String lang = request.getParameter("lang");
        if(lang == null) lang = "en";
        String labelFieldName = InstancesFields.LABEL.name() + "-" + lang;
        String altLabelFieldName = InstancesFields.ALT_LABEL.name() + "-" + lang;

        try {
            String keyword = request.getParameter("keyword");
            if(keyword == null) {
                out.println("No keyword provided");
                return;
            }

            Analyzer analyzer = new StandardAnalyzer();
            HashMap<String,Float> boostsMap = new HashMap<>();
            boostsMap.put(altLabelFieldName, 1f);
            boostsMap.put(labelFieldName, 1f);

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    new String[] {labelFieldName, altLabelFieldName},
                    analyzer, boostsMap);

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            Query baseAutoCompleteQuery = null;
            if(keyword.matches("[A-Za-z ]*")) {
                keyword = keyword.trim();
                baseAutoCompleteQuery = queryParser.parse(keyword + "*");
            }
            Query baseLiteralQuery = queryParser.parse(keyword);
/*
            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(InstancesFields.RANK.name());
            boostsSource = DoubleValuesSource.function(boostsSource, new ScoreBoostsOperator());
            boostsSource = DoubleValuesSource.scoringFunction(boostsSource, (Double src, Double score) -> src*score);
*/
            if(baseAutoCompleteQuery != null)
                queryBuilder.add(baseAutoCompleteQuery, BooleanClause.Occur.SHOULD);
            queryBuilder.add(baseLiteralQuery, BooleanClause.Occur.SHOULD);
            Query query = queryBuilder.build();

            Sort sorting = new Sort(new SortField(InstancesFields.RANK.name(), SortField.Type.DOUBLE, true));

            TopDocs results = searcher.search(query, DOCS_PER_PAGE, sorting);
            ScoreDoc[] hits = results.scoreDocs;
            if(hits.length == 0) throw new Exception();

            for(ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                String label = doc.get(labelFieldName);
                String q = doc.get(InstancesFields.ID.name());
                if(label==null) label = q;
                String occurrences = doc.get(InstancesFields.FREQ_STORED.name());
                if(label.contains(keyword)) {
                    out.print("<option code='" + q + "'>" + label + " (" + occurrences);
                    if (lang.equals("es")) {
                        out.print(" resultados");
                    } else {
                        out.print(" results");
                    }
                    out.println(")</option>");
                    continue;
                }
                IndexableField[] altLabels = doc.getFields(altLabelFieldName);
                for(IndexableField altLabel : altLabels) {
                    String name = altLabel.stringValue();
                    if(name.contains(keyword)) {
                        out.print("<option code='" + q + "'>" + name + " (" + occurrences);
                        if (lang.equals("es")) {
                            out.print(" resultados");
                        } else {
                            out.print(" results");
                        }
                        out.println(")</option>");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace(out);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

}
