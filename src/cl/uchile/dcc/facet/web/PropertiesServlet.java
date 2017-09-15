package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import cl.uchile.dcc.facet.core.ValuesField;
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
import javax.json.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

public class PropertiesServlet extends DataServlet {

    private static final int DOCS_PER_PAGE = 50000;
    private IndexReader valuesReader;
    private IndexSearcher valuesSearcher;

    @Override
    public void init() throws ServletException {
        super.init();
        try{
            String valuesDirectory = getServletContext().getInitParameter("ValuesDirectory");
            valuesReader = DirectoryReader.open(FSDirectory.open(Paths.get(valuesDirectory)));
            valuesSearcher = new IndexSearcher(valuesReader);
            // Magic is everything! - Reinhardt
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
            boolean hasProperties = false;
            Map<String, String> values = new HashMap<>();

            String keyword = request.getParameter("keyword");
            String instance = request.getParameter("instance");
            String property = request.getParameter("property");
            String[] selected = request.getParameterValues("selected");
            if(selected != null) {
                Arrays.sort(selected);
                hasProperties = true;
            }

            boolean hasCache = false;
            Document valuesDoc = null;
            if(instance != null && hasProperties) {
                Query valuesQuery = new WildcardQuery(new Term(ValuesField.BASE.name(), instance + "||*||" + property));
                TopDocs valuesResults = valuesSearcher.search(valuesQuery, valuesReader.numDocs());
                ScoreDoc[] hits = valuesResults.scoreDocs;
                for(ScoreDoc hit : hits) {
                    Document doc = valuesSearcher.doc(hit.doc);
                    String code = doc.get(ValuesField.BASE.name());
                    String[] split = code.split("\\|\\|");
                    String[] resultsProperties = Arrays.copyOfRange(split, 1, split.length -1);
                    Arrays.sort(resultsProperties);
                    if(Arrays.equals(selected, resultsProperties)) {
                        System.err.println("Loading values from cache for: " + code);
                        hasCache = true;
                        valuesDoc = doc;
                    }
                }
            } else if(instance != null) {
                Query valuesQuery = new TermQuery(new Term(ValuesField.BASE.name(), instance + "||" + property));
                TopDocs valuesResults = valuesSearcher.search(valuesQuery, 1);
                if(valuesResults.totalHits > 0) {
                    valuesDoc = valuesSearcher.doc(valuesResults.scoreDocs[0].doc);
                    hasCache = true;
                    System.err.println("Loading values from cache for: " + instance + "||" + property);
                }
            }

            if(hasCache) {
                String jsonFromDoc = valuesDoc.get(ValuesField.VALUES.name());
                out.print(jsonFromDoc);
                return;
            } else {
                System.err.println("Not cache found for values");
                List<Query> queries = new ArrayList<>();
                if(keyword != null && !keyword.trim().isEmpty())
                    queries.add(queryParser.parse(keyword));
                if(instance != null && !instance.trim().isEmpty())
                    queries.add(new TermQuery(new Term(DataFields.INSTANCE.name(), instance)));
                if(selected != null && selected.length > 0) {
                    for (String filter : selected) {
                        if(filter.isEmpty()) continue;
                        queries.add(new TermQuery(new Term(DataFields.PO.name(), filter)));
                    }
                }
                queries.add(new TermQuery(new Term(DataFields.P.name(), property)));

                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                for(Query query : queries)
                    queryBuilder.add(query, BooleanClause.Occur.MUST);
                Query query = queryBuilder.build();

                TopDocs results = searcher.search(query, DOCS_PER_PAGE);

                ScoreDoc[] hits = results.scoreDocs;
                for(ScoreDoc hit : hits) {
                    Document doc = searcher.doc(hit.doc);
                    IndexableField[] pos = doc.getFields(DataFields.PO.name());
                    for(IndexableField po : pos) {
                        String[] raw = po.stringValue().split("##");
                        if (!property.equals(raw[0])) continue;
                        String code = raw[1];
                        if(!code.startsWith("Q")) continue;
                        if (values.containsKey(code)) continue;
                        String name = getLabelFromSubject(code);
                        values.put(code, name);
                    }
                }
            }

            // Build JSON response
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder array = factory.createArrayBuilder();
            // Response
            for (Map.Entry<String,String> entry : values.entrySet()) {
                if(entry.getValue() == null) continue;
                array = array.add(factory.createObjectBuilder()
                    .add("id", property + "##" + entry.getKey())
                    .add("name", entry.getValue()));
            }
            out.print(array.build().toString());
        } catch(Exception e) {
            e.printStackTrace(out);
        }
    }

}
