package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import cl.uchile.dcc.facet.core.InstancesFields;
import cl.uchile.dcc.facet.core.ScoreBoostsOperator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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
            String lang = request.getParameter("lang");
            if(lang==null) lang = "en";
            String keyword = request.getParameter("keyword");
            String type = request.getParameter("instance");
            String[] propertiesForm = request.getParameterValues("properties");
            boolean hasProperties = false;
            if(propertiesForm != null) {
                for(String property : propertiesForm) {
                    if(!property.trim().isEmpty()) {
                        hasProperties = true;
                        break;
                    }
                }
            }
            if(hasProperties)
                Arrays.sort(propertiesForm);
            List<String> selectedProperties = new ArrayList<>();

            Analyzer analyzer = new StandardAnalyzer();
            String labelFieldName = DataFields.LABEL.name() + "-" + lang;
            String altLabelFieldName = DataFields.ALT_LABEL.name() + "-" + lang;
            String descriptionFieldName = DataFields.DESCRIPTION.name() + "-" + lang;

            HashMap<String,Float> boostsMap = new HashMap<>();
            boostsMap.put(altLabelFieldName, 2f);
            boostsMap.put(descriptionFieldName, 1f);
            boostsMap.put(labelFieldName, 5f);

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    new String[] {labelFieldName, descriptionFieldName, altLabelFieldName},
                    analyzer, boostsMap);

            if(keyword != null && !keyword.trim().isEmpty()) {
                keyword = keyword.trim();
                queries.add(queryParser.parse(keyword));
            }
            if(type != null && !type.trim().isEmpty()) {
                String instanceOfCode = getServletContext().getInitParameter("InstancesCode");
                type = type.trim();
                queries.add(new TermQuery(new Term(DataFields.TYPE.name(), type)));
                request.setAttribute("type", getLabelFromSubject(type, lang));
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
                    labelProperties.add(new CodeNameValue(getLabelFromSubject(split[0], lang),
                            getLabelFromSubject(split[1], lang), 0));
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
            DoubleValuesSource boostsSource = DoubleValuesSource.fromDoubleField(DataFields.RANK.name());
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
                    instancesQuery = new WildcardQuery(new Term(InstancesFields.ID.name(), type+"||*"));
                    withProperties = true;
                } else {
                    instancesQuery = new TermQuery(new Term(InstancesFields.ID.name(), type));
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
                            String docsCode = currentDoc.get(InstancesFields.ID.name());
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
                            System.err.println("DEBUG: Cache found for " + instanceDoc.get(InstancesFields.ID.name()));
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

            System.err.println("Service "+serviceId+" read the cache if present at " + (System.currentTimeMillis()-startTime));
            // Getting the results
            TopDocs results;
            if(!isCached) {
                results = searcher.search(query, 50000);
                System.err.println("DEBUG: Not cache found...");
            } else {
                results = searcher.search(query, MAX_DOCS);
            }
            ScoreDoc[] hits = results.scoreDocs;

            System.err.println("Service "+serviceId+" finish searching... " + (System.currentTimeMillis()-startTime));
            int counter = 0;

            for (ScoreDoc hit : hits) {
                counter++;
                Document doc = searcher.doc(hit.doc);
                Set<String> docsProperties = new HashSet<>();
                // Store only top 50 results to display
                if(counter <= 50) {
                    String subject = doc.get(DataFields.SUBJECT.name());
                    String label = doc.get(labelFieldName);
                    if(label == null) label = subject;
                    String description = doc.get(descriptionFieldName);
                    if(description == null) description = "";
                    StringBuilder stringBuilder = new StringBuilder();
                    IndexableField[] altLabels = doc.getFields(altLabelFieldName);
                    for(IndexableField altLabel : altLabels) {
                        String text = altLabel.stringValue();
                        if(stringBuilder.length() > 0) stringBuilder.append(", ");
                        stringBuilder.append(text);
                    }
                    String image = doc.get(DataFields.IMAGE.name());
                    String boost = Double.toString(hit.score);
                    Entry entry = new Entry(subject, label, description, stringBuilder.toString(), boost, image);
                    entries.add(entry);
                }
                // Compute properties if needed
                if(!isCached) {
                    String subjectPrefix = getServletContext().getInitParameter("SubjectPrefix");
                    IndexableField[] pos = doc.getFields(DataFields.PO.name());
                    for (IndexableField po : pos) {
                        String[] raw = po.stringValue().split("##");
                        if(!raw[1].startsWith(subjectPrefix)) continue;
                        String key = raw[0];
                        if (selectedProperties.contains(key)) continue;
                        if (docsProperties.contains(key)) continue;
                        docsProperties.add(key);
                        if (propertiesMap.containsKey(key)) {
                            propertiesMap.replace(key, propertiesMap.get(key) + 1);
                        } else {
                            propertiesMap.put(key, 1);
                        }
                    }
                    if(counter%100==0)
                        System.err.println("Service "+serviceId+" is computing facets... " + (System.currentTimeMillis()-startTime));
                }
            }

            //System.err.println("Service "+serviceId+" get all results and facets if not cache was found at " + (System.currentTimeMillis()-startTime));
            // Get most frequent properties
            List<Map.Entry<String, Integer>> propertiesList = propertiesMap.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .collect(Collectors.toList());

            //int length = propertiesList.size() < 20 ? propertiesList.size() : 20;
            //List<Map.Entry<String, Integer>> propertiesSub = propertiesList.subList(0, length);
            List<CodeNameValue> properties = new ArrayList<>();

            for(Map.Entry<String, Integer> property : propertiesList) {
                String propName = getLabelFromSubject(property.getKey(), lang);
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
