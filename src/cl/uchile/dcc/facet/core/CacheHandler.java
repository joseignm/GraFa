package cl.uchile.dcc.facet.core;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

class CacheHandler extends AbstractRDFHandler {

    private static final int M_PRIME = 50000;
    private static final int TICKS = 100000;

    private List<String> poList;
    private Map<String, Integer> map;

    private Resource last;
    private List<String> subjectPoList;
    private Properties properties;
    private int read;

    CacheHandler(List<String> poList) throws IOException {
        this.poList = poList;
        map = new HashMap<>();
        last = null;
        read = 0;
        properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);
    }

    private List<String> getAllCombinations(List<String> list) {
        List<String> results = new ArrayList<>();
        String instanceOfCode = properties.getProperty("instanceOf");
        String instance = null;
        for(String e : list) {
            if(e.startsWith(instanceOfCode)) {
                results.add(e);
                instance = e;
            }
        }
        if(instance == null) {
            return results;
        }
        list.remove(instance);
        for(String element : list) {
            int resultsLength = results.size();
            for(int j = 0; j < resultsLength; j++) {
                results.add(results.get(j) + "||" + element);
            }
        }
        return results;
    }

    @Override
    public void handleStatement(Statement s) {
        read++;
        if(read%TICKS == 0)
            System.err.println(read + "lines read...");

        final String entityIRI = properties.getProperty("entityIRI");
        final String propertyIRI = properties.getProperty("propertyIRI");

        Resource subject = s.getSubject();
        // FIRST LINE
        if(last == null) {
            last = subject;
            subjectPoList = new ArrayList<>();
        }
        // NEW SUBJECT
        if(!last.toString().equals(subject.toString())) {
            subjectPoList = subjectPoList.stream()
                    .sorted((e1, e2) -> poList.indexOf(e1) - poList.indexOf(e2))
                    .collect(Collectors.toList());
            List<String> combinations = getAllCombinations(subjectPoList);
            // Add to the map all possible combinations
            for(String combination : combinations) {
                if(map.containsKey(combination)) {
                    map.replace(combination, map.get(combination) + 1);
                } else {
                    map.put(combination, 1);
                }
            }
            // Start a new list for the new subject
            last = subject;
            subjectPoList = new ArrayList<>();
        }
        // PROPERTIES
        String predicate = s.getPredicate().toString();
        if(predicate.startsWith(propertyIRI)) {
            String p = predicate.replace(propertyIRI, "");
            String object = s.getObject().toString();
            String q = object.replace(entityIRI, "");
            String value = p + "##" + q;
            if(poList.contains(value)) {
                subjectPoList.add(value);
            }
        }
    }

    List<String> getResults() {
        // Process the last subject
        List<String> combinations = getAllCombinations(subjectPoList);
        // Add to the map all possible combinations
        for(String combination : combinations) {
            if(map.containsKey(combination)) {
                map.replace(combination, map.get(combination) + 1);
            } else {
                map.put(combination, 1);
            }
        }
        // Return results
        return map.entrySet().stream()
                .filter(e -> e.getValue() > M_PRIME)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
