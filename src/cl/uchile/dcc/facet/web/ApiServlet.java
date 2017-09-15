package cl.uchile.dcc.facet.web;

import cl.uchile.dcc.facet.core.DataFields;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ApiServlet extends DataServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String queryText = request.getParameter("query");
            StandardQueryParser queryParser = new StandardQueryParser();
            Query query = queryParser.parse(queryText, DataFields.SUBJECT.name());

            TopDocs results = searcher.search(query, reader.numDocs());
            ScoreDoc[] hits = results.scoreDocs;
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder resultsArray = factory.createArrayBuilder();

            for(ScoreDoc hit : hits) {
                Document document = searcher.doc(hit.doc);
                JsonObjectBuilder hitObject = factory.createObjectBuilder();

                String id = document.get(DataFields.SUBJECT.name());
                hitObject.add("id", id);

                String label = document.get(DataFields.LABEL.name());
                label = label == null ? "" : label;
                hitObject.add("label", label);

                String altLabel = document.get(DataFields.ALT_LABEL.name());
                altLabel = altLabel == null ? "" : altLabel;
                hitObject.add("alt labels", altLabel);

                String description = document.get(DataFields.DESCRIPTION.name());
                description = description == null ? "" : description;
                hitObject.add("description", description);

                String rank = document.get(DataFields.VALUE.name());
                hitObject.add("rank", Double.parseDouble(rank));

                String type = document.get(DataFields.INSTANCE.name());
                type = type == null ? "" : type;
                hitObject.add("type", type);

                JsonArrayBuilder pArray = factory.createArrayBuilder();
                IndexableField[] ps = document.getFields(DataFields.P.name());
                for(IndexableField p : ps) {
                    pArray.add(p.stringValue());
                }
                hitObject.add("properties", pArray);

                JsonArrayBuilder poArray = factory.createArrayBuilder();
                IndexableField[] pos = document.getFields(DataFields.PO.name());
                for(IndexableField po : pos) {
                    poArray.add(po.stringValue());
                }
                hitObject.add("property value", poArray);

                resultsArray.add(hitObject);
            }

            out.print(resultsArray.build().toString());
        } catch(Exception e) {
            e.printStackTrace(out);
        }
    }
}
