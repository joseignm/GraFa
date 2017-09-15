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

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SearchServlet extends DataServlet {

    private static final int MAX_DOCS = 50;
    private IndexSearcher instancesSearcher;
    private IndexReader instancesReader;
    private static final String subjectPrefix = "Q";

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String instancesDir = getServletContext().getInitParameter("InstancesDirectory");
            instancesReader = DirectoryReader.open(FSDirectory.open(Paths.get(instancesDir)));
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

        long startTime = System.currentTimeMillis();
        String serviceId = out.toString().split("@")[1];
        System.err.println("Service Id " + serviceId + " started");
        long threadStartTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId());

        try {
            // Construct query from all specified filters
            List<Query> queries = new ArrayList<>();
            String keyword = request.getParameter("keyword");
            String type = request.getParameter("instance");
            String[] propertiesForm = request.getParameterValues("properties");
            boolean hasProperties = false;
            if(propertiesForm != null) {
                for(String property : propertiesForm) {
                    if (!property.trim().isEmpty()) {
                        hasProperties = true;
                        break;
                    }
                }
            }
            if(hasProperties)
                Arrays.sort(propertiesForm);
            List<String> selectedProperties = new ArrayList<>();
            if(keyword != null && !keyword.trim().isEmpty()) {
                keyword = keyword.trim();
                queries.add(queryParser.parse(keyword));
            }
            if(type != null && !type.trim().isEmpty()) {
                String instanceOfCode = getServletContext().getInitParameter("InstancesCode");
                type = type.trim();
                queries.add(new TermQuery(new Term(DataFields.INSTANCE.name(), type)));
                request.setAttribute("type", getLabelFromSubject(type));
                selectedProperties.add(instanceOfCode);
            } else
                type = null;

            if(hasProperties) {
                List<String> checkedProperties = new ArrayList<>();
                List<CodeNameValue> labelProperties = new ArrayList<>();
                for(String property : propertiesForm) {
                    if(property.trim().isEmpty()) continue;
                    checkedProperties.add(property);
                    String[] split = property.split("##");
                    selectedProperties.add(split[0]);
                    labelProperties.add(new CodeNameValue(getLabelFromSubject(split[0]), getLabelFromSubject(split[1]), 0));
                    queries.add(new TermQuery(new Term(DataFields.PO.name(), property)));
                }
                request.setAttribute("checked", checkedProperties);
                request.setAttribute("labels", labelProperties);
            }

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            for(Query q : queries) {
                queryBuilder.add(q, BooleanClause.Occur.MUST);
            }
            Query baseQuery = queryBuilder.build();

            // Sort results by Lucene's score and PageRank score
            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(DataFields.BOOSTS.name());
            boostsSource = DoubleValuesSource.function(boostsSource, new ScoreBoostsOperator());
            boostsSource = DoubleValuesSource.scoringFunction(boostsSource, (src, score) -> src*score);
            Query query = new FunctionScoreQuery(baseQuery, boostsSource);

            List<Entry> entries = new ArrayList<>();
            Map<String, Integer> propertiesMap = new HashMap<>();

            boolean isCached = false;

            // Check if properties are cached
            if(type != null) {
                Query instancesQuery;
                boolean withProperties;
                if(hasProperties) {
                    instancesQuery = new WildcardQuery(new Term(InstancesFields.Q.name(), type+"||*"));
                    withProperties = true;
                } else {
                    instancesQuery = new TermQuery(new Term(InstancesFields.Q.name(), type));
                    withProperties = false;
                }
                TopDocs instancesResults = instancesSearcher.search(instancesQuery, instancesReader.numDocs());
                if(instancesResults.totalHits > 0) {
                    Document instanceDoc = null;
                    ScoreDoc[] hits = instancesResults.scoreDocs;
                    if(!withProperties) {
                        instanceDoc = instancesSearcher.doc(hits[0].doc);
                    } else {
                        System.err.println(Arrays.toString(propertiesForm));
                        for (ScoreDoc hit : hits) {
                            Document currentDoc = instancesSearcher.doc(hit.doc);
                            String docsCode = currentDoc.get(InstancesFields.Q.name());
                            String[] docProperties = docsCode.split("\\|\\|");
                            docProperties = Arrays.copyOfRange(docProperties, 1, docProperties.length);
                            Arrays.sort(docProperties);
                            if (Arrays.equals(propertiesForm, docProperties)) {
                                instanceDoc = currentDoc;
                                break;
                            }
                        }
                    }
                    if(instanceDoc != null) {
                        // Cache found!
                        IndexableField[] properties = instanceDoc.getFields(InstancesFields.PROPERTY.name());
                        if(properties.length == 0) {
                            isCached = false;
                        } else {
                            System.err.println("DEBUG: Cache found for " + instanceDoc.get(InstancesFields.Q.name()));
                            isCached = true;
                        }
                        for (IndexableField property : properties) {
                            String value = property.stringValue();
                            String[] split = value.split("##");
                            String pCode = split[0];
                            if (selectedProperties.contains(pCode)) continue;
                            int frequency = Integer.parseInt(split[1]);
                            propertiesMap.put(pCode, frequency);
                        }
                    }
                }
            }

            //System.err.println("Service "+serviceId+" read the cache if present at " + (System.currentTimeMillis()-startTime));
            // Getting the results
            TopDocs results;
            if(!isCached) {
                results = searcher.search(query, reader.maxDoc());
                System.err.println("DEBUG: Not cache found...");
            } else {
                results = searcher.search(query, MAX_DOCS);
            }
            ScoreDoc[] hits = results.scoreDocs;
            int counter = 0;

            for (ScoreDoc hit : hits) {
                counter++;
                Document doc = searcher.doc(hit.doc);
                // Store only top 50 results to display
                if(counter <= 50) {
                    String subject = doc.get(DataFields.SUBJECT.name());
                    String label = doc.get(DataFields.LABEL.name());
                    String description = doc.get(DataFields.DESCRIPTION.name());
                    String alt = doc.get(DataFields.ALT_LABEL.name());
                    String boost = Double.toString(hit.score);
                    Entry entry = new Entry(subject, label, description, alt, boost);
                    entries.add(entry);
                }
                // Compute properties if needed
                if(!isCached) {
                    IndexableField[] pos = doc.getFields(DataFields.PO.name());
                    for (IndexableField po : pos) {
                        String[] raw = po.stringValue().split("##");
                        if(!raw[1].startsWith(subjectPrefix)) continue;
                        String key = raw[0];
                        if (selectedProperties.contains(key)) continue;
                        if (propertiesMap.containsKey(key)) {
                            propertiesMap.replace(key, propertiesMap.get(key) + 1);
                        } else {
                            propertiesMap.put(key, 1);
                        }
                    }
                }
            }

            //System.err.println("Service "+serviceId+" get all results and facets if not cache was found at " + (System.currentTimeMillis()-startTime));
            // Get most frequent properties
            List<Map.Entry<String, Integer>> propertiesList = propertiesMap.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .collect(Collectors.toList());

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

            System.err.println("Service "+serviceId+" forwarding to JSP at " + (System.currentTimeMillis()-startTime));
            long threadTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId()) - threadStartTime;
            System.err.println("Service "+serviceId+" total active time " + threadTime);
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

}
