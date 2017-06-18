package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import cl.uchile.dcc.facet.core.PropertiesFields;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesServlet extends DataServlet {

    private static final int DOCS_PER_PAGE = 50;
    private static final String propertyDir = "/home/jmoreno/bin/properties_v3.0";
    private IndexSearcher propertySearcher;

    @Override
    public void init() throws ServletException {
        super.init();
        try{
            IndexReader propertyReader = DirectoryReader.open(FSDirectory.open(Paths.get(propertyDir)));
            propertySearcher = new IndexSearcher(propertyReader);
        } catch(IOException ioe) {
            System.err.println("FATAL: Cannot open Properties Lucene folder");
            throw new ServletException();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String keyword = request.getParameter("keyword");
            String instance = request.getParameter("instance");
            String property = request.getParameter("property");
            String[] selected = request.getParameterValues("selected");
            List<Query> queries = new ArrayList<>();

            if(keyword != null && !keyword.trim().isEmpty())
                queries.add(queryParser.parse(keyword));
            if(instance != null && !instance.trim().isEmpty())
                queries.add(new TermQuery(new Term(DataFields.INSTANCE.name(), instance)));
            if(selected != null) {
                for (String filter : selected) {
                    queries.add(new TermQuery(new Term(DataFields.PO.name(), filter)));
                }
            }
            queries.add(new TermQuery(new Term(DataFields.P.name(), property)));

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            for(Query query : queries)
                queryBuilder.add(query, BooleanClause.Occur.MUST);
            Query query = queryBuilder.build();

            TopDocs results = searcher.search(query, DOCS_PER_PAGE);
            Map<String, String> values = new HashMap<>();

            if(results.totalHits < 200) {
                ScoreDoc[] hits = results.scoreDocs;
                for(ScoreDoc hit : hits) {
                    Document doc = searcher.doc(hit.doc);
                    IndexableField[] pos = doc.getFields(DataFields.PO.name());
                    for(IndexableField po : pos) {
                        String[] raw = po.stringValue().split("##");
                        if (!property.equals(raw[0])) continue;
                        String code = raw[1];
                        if (values.containsKey(code)) continue;
                        String name = getLabelFromSubject(code);
                        values.put(code, name);
                    }
                }
            } else {
                Query propertyQuery = new TermQuery(new Term(PropertiesFields.P.name(), property));
                TopDocs propertyResults = propertySearcher.search(propertyQuery, 1);
                ScoreDoc hit = propertyResults.scoreDocs[0];
                Document doc = propertySearcher.doc(hit.doc);
                IndexableField[] valuesResults = doc.getFields(PropertiesFields.VALUES.name());
                for(IndexableField value : valuesResults) {
                    String code = value.stringValue();
                    String name = getLabelFromSubject(code);
                    values.put(code, name);
                }
            }

            for (Map.Entry entry : values.entrySet()) {
                out.print("<input type='checkbox' name='properties' value='");
                out.print(property + "##" + entry.getKey() + "'>");
                out.print(entry.getValue());
                out.print("<br>");
            }
        } catch(Exception e) {
            out.println("Error");
        }
    }

}
