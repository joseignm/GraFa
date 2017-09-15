package cl.uchile.dcc.facet.testing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class SearchThread extends Thread {

    private static final String baseURL = "http://facet.dcc.uchile.cl/testfacet/";
    private static final Object mutex = new Object();
    private SearchStats stats;

    SearchThread(SearchStats ts) {
        stats = ts;
    }

    @Override
    public void run() {
        List<String> currentProperties = new ArrayList<>();
        String instance = "Q5";
        Random random = ThreadLocalRandom.current();

        int results;
        int depth = 0;
        try {
            do {
                // Construct url
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(baseURL);
                urlBuilder.append("search?instance=");
                urlBuilder.append(instance);
                for (String property : currentProperties) {
                    urlBuilder.append("&properties=");
                    urlBuilder.append(URLEncoder.encode(property, "UTF-8"));
                }
                String url = urlBuilder.toString();

                Document resultsPage;
                int size;
                long start, end;

                //synchronized(mutex) {
                    start = System.currentTimeMillis();
                    Connection connection = Jsoup.connect(url).timeout(60 * 1000);
                    connection.get();
                    Connection.Response response = connection.response();
                    size = response.bodyAsBytes().length;
                    resultsPage = response.parse();
                    end = System.currentTimeMillis();
                //}
                long time = end - start;

                Element resultsInfo = resultsPage.select("div.col-md-9").first().child(0);
                results = Integer.parseInt(resultsInfo.text().replaceAll("[^0-9]", ""));

                if (results < 2) {
                    DataEntry currentStep = new DataEntry(url, time, results, depth, 0, size);
                    stats.addDataSearch(currentStep);
                    break;
                }
                Elements possibleProperties = resultsPage.select("button[type=button][value^=P]");

                RandomWeightList<String> availableProperties = new RandomWeightList<>();
                for (int j = 0; j < possibleProperties.size(); j++) {
                    Element element = possibleProperties.get(j);
                    String code = element.attr("value");
                    if (code.contains("##")) continue;
                    double weight = possibleProperties.size() - j;
                    availableProperties.add(code, weight);
                }
                String selectedProperty = availableProperties.nextElement();

                DataEntry currentStep = new DataEntry(url, time, results, depth, availableProperties.size(), size);
                stats.addDataSearch(currentStep);

                urlBuilder = new StringBuilder();
                urlBuilder.append(baseURL);
                urlBuilder.append("properties?instance=");
                urlBuilder.append(instance);
                for (String property : currentProperties) {
                    urlBuilder.append("&selected=");
                    urlBuilder.append(URLEncoder.encode(property, "UTF-8"));
                }
                urlBuilder.append("&property=");
                urlBuilder.append(selectedProperty);
                String propertiesUrl = urlBuilder.toString();

                JsonArray jsonResponse;
                //synchronized(mutex) {
                    start = System.currentTimeMillis();
                    InputStream stream = new URL(propertiesUrl).openStream();
                    JsonReader reader = Json.createReader(stream);
                    jsonResponse = reader.readArray();
                    end = System.currentTimeMillis();
                //}
                time = end - start;

                int index = random.nextInt(jsonResponse.size());
                JsonObject selectedPo = jsonResponse.getJsonObject(index);
                String poCode = selectedPo.getString("id");

                PropertyEntry propertyEntry = new PropertyEntry(propertiesUrl, jsonResponse.size(), poCode, time);
                stats.addPropertySearch(propertyEntry);
                currentProperties.add(poCode);
                depth++;
            } while (results > 1);
        } catch (Exception e) {
            System.err.println(this.getName() + " throws an exception");
            e.printStackTrace();
        }
    }
}
