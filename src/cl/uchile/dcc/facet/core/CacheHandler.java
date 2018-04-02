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

    private TreeSet<String> poSet;
    private Map<String, Integer> map;

    private Resource last;
    private List<String> subjectPoList;
    private Properties properties;
    private int read;

    CacheHandler(List<String> poList) throws IOException {
        // use for log(n) contains
        poSet = new TreeSet<String>();
        poSet.addAll(poList);
        
        map = new HashMap<>();
        last = null;
        read = 0;
        properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);
    }

    private List<String> getAllCombinations(List<String> list) {
    	Set<String> types = new TreeSet<>();
        List<String> results = new ArrayList<>();
        String instanceOfCode = properties.getProperty("instanceOf");
        for(String e : list) {
            if(e.startsWith(instanceOfCode)) {
                results.add(e);
                types.add(e);
            }
        }
        if(types.isEmpty()) {
            return results;
        }
        for(String element : list) {
        	if(!types.contains(element)){
	            int resultsLength = results.size();
	            for(int j = 0; j < resultsLength; j++) {
	                results.add(results.get(j) + "||" + element);
	            }
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
        	updateEntity();
            // Start a new list for the new subject
            last = subject;
            subjectPoList = new ArrayList<>();
        }
        // PROPERTIES
        String predicate = s.getPredicate().toString();
        if(predicate.startsWith(propertyIRI)) {
        	String p = predicate.substring(propertyIRI.length());
            String object = s.getObject().toString();
            String q = object.substring(entityIRI.length());
            String value = p + "##" + q;
            if(poSet.contains(value)) {
                subjectPoList.add(value);
            }
        }
    }
    
    void updateEntity(){
    	Collections.sort(subjectPoList);
        List<String> combinations = getAllCombinations(subjectPoList);
        // Add to the map all possible combinations
        for(String combination : combinations) {
        	Integer count = map.get(combination);
        	if(count==null) count = 0;
        	count = count + 1;
            map.put(combination, 1);
        }
    }
    
    @Override
    public void endRDF(){
    	updateEntity();
    }

    List<String> getResults() {
        // Return results
        return map.entrySet().stream()
                .filter(e -> e.getValue() > M_PRIME)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
