package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import cl.uchile.dcc.facet.core.InstancesFields;
import cl.uchile.dcc.facet.core.ScoreBoostsOperator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Paths;
import java.util.*;

public class SearchServlet extends DataServlet {

    private static final int DOCS_PER_PAGE = 200;
    private static final String instancesDir = "/home/jmoreno/bin/instances_v3.0";
    private IndexSearcher instancesSearcher;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            IndexReader instancesReader = DirectoryReader.open(FSDirectory.open(Paths.get(instancesDir)));
            instancesSearcher = new IndexSearcher(instancesReader);
        } catch(IOException ioe) {
            System.err.println("FATAL: Cannot open Lucene folder");
            throw new ServletException();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Construct query from all specified filters
            List<Query> queries = new ArrayList<>();
            String keyword = request.getParameter("keyword");
            String type = request.getParameter("instance");
            String[] propertiesForm = request.getParameterValues("properties");
            List<String> selectedProperties = new ArrayList<>();
            if(keyword != null && !keyword.trim().isEmpty()) {
                keyword = keyword.trim();
                queries.add(queryParser.parse(keyword));
            }
            if(type != null && !type.trim().isEmpty()) {
                type = type.trim();
                queries.add(new TermQuery(new Term(DataFields.INSTANCE.name(), type)));
                request.setAttribute("type", getLabelFromSubject(type));
                selectedProperties.add("P31");
            } else
                type = null;

            if(propertiesForm != null && propertiesForm.length > 0) {
                List<String> checkedProperties = new ArrayList<>();
                for(String property : propertiesForm) {
                    checkedProperties.add(property);
                    String pCode = property.split("##")[0];
                    selectedProperties.add(pCode);
                    queries.add(new TermQuery(new Term(DataFields.PO.name(), property)));
                }
                request.setAttribute("checked", checkedProperties);
            }

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            for(Query q : queries) {
                queryBuilder.add(q, BooleanClause.Occur.MUST);
            }
            Query baseQuery = queryBuilder.build();

            // Sort results by Lucene's score and PageRank score
            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(DataFields.BOOSTS.name());
            boostsSource = DoubleValuesSource.function(boostsSource, new ScoreBoostsOperator());
            boostsSource = DoubleValuesSource.scoringFunction(boostsSource, (Double src, Double score) -> src*score);
            Query query = new FunctionScoreQuery(baseQuery, boostsSource);

            // Getting the results
            TopDocs results = searcher.search(query, DOCS_PER_PAGE);
            ScoreDoc[] hits = results.scoreDocs;
            int counter = 0;

            List<Entry> entries = new ArrayList<>();
            Map<String, Integer> propertiesMap = new HashMap<>();
            for (ScoreDoc hit : hits) {
                counter++;
                Document doc = searcher.doc(hit.doc);
                // Store only top 50 results to display
                if(counter < 50) {
                    String subject = doc.get(DataFields.SUBJECT.name());
                    String label = doc.get(DataFields.LABEL.name());
                    String description = doc.get(DataFields.DESCRIPTION.name());
                    String alt = doc.get(DataFields.ALT_LABEL.name());
                    String boost = Double.toString(hit.score);
                    Entry entry = new Entry(subject, label, description, alt, boost);
                    entries.add(entry);
                }
                // Compute properties if few results or not type specified
                if(results.totalHits <= DOCS_PER_PAGE || type == null) {
                    IndexableField[] ps = doc.getFields(DataFields.P.name());
                    for (IndexableField p : ps) {
                        String key = p.stringValue();
                        if (selectedProperties.contains(key)) continue;
                        if (propertiesMap.containsKey(key)) {
                            propertiesMap.replace(key, propertiesMap.get(key) + 1);
                        } else {
                            propertiesMap.put(key, 1);
                        }
                    }
                }
            }
            // Too many results reading properties from separate index
            if(results.totalHits > DOCS_PER_PAGE && type != null) {
                Query instancesQuery = new TermQuery(new Term(InstancesFields.Q.name(), type));
                TopDocs instancesResults = instancesSearcher.search(instancesQuery, 1);
                if(instancesResults.totalHits > 0) {
                    Document instanceDoc = instancesSearcher.doc(instancesResults.scoreDocs[0].doc);
                    IndexableField[] properties = instanceDoc.getFields(InstancesFields.PROPERTY.name());
                    for(IndexableField property : properties) {
                        String value = property.stringValue();
                        String[] split = value.split("##");
                        String code = split[0];
                        if (selectedProperties.contains(code)) continue;
                        int frequency = Integer.parseInt(split[1]);
                        propertiesMap.put(code, frequency);
                    }
                }
            }
            // Get most frequent properties
            List<Map.Entry<String, Integer>> propertiesList = new ArrayList<>(propertiesMap.entrySet());
            Collections.sort(propertiesList,
                    (Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> o2.getValue().compareTo(o1.getValue()));
            int length = propertiesList.size() < 20 ? propertiesList.size() : 20;
            List<Map.Entry<String, Integer>> propertiesSub = propertiesList.subList(0, length);
            List<CodeNameValue> properties = new ArrayList<>();

            for(Map.Entry<String, Integer> property : propertiesSub) {
                String propName = getLabelFromSubject(property.getKey());
                properties.add(new CodeNameValue(property.getKey(), propName, property.getValue()));
            }
            // Send to JSP
            request.setAttribute("results", entries);
            request.setAttribute("properties", properties);
            request.setAttribute("total", results.totalHits);
            getServletConfig().getServletContext().getRequestDispatcher("/results.jsp").forward(request,response);
        } catch (Exception e) {
            out.println("Error while performing query!");
            e.printStackTrace(out);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    @Override
    public void destroy() {

    }

}
