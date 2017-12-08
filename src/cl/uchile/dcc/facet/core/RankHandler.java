package cl.uchile.dcc.facet.core;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class RankHandler extends AbstractRDFHandler {

    private Resource last;
    private int read;
    private int[][] graph;
    private int currentIndex;
    private Set<Integer> outLinksList;
    private Map<String, Integer> map;
    private Properties properties;

    RankHandler(int[][] graph, Map<String, Integer> map) throws IOException {
        super();
        last = null;
        outLinksList = null;
        read = 0;
        currentIndex = 0;
        this.graph = graph;
        this.map = map;
        properties = new Properties();
        InputStream input = new FileInputStream("facet.properties");
        properties.load(input);
    }

    @Override
    public void handleStatement(Statement s) {
        final int TICKS = 100000;

        final String entityIRI = properties.getProperty("entityIRI");
        read++;
        if(read%TICKS==0) {
            System.err.println(read + " lines read...");

            long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

            System.err.println("Free memory: " + freeMemory);
        }

        Resource subject = s.getSubject();
        // FIRST LINE
        if(last == null) {
            last = subject;
            String name = last.toString();
            name = name.replace(entityIRI, "");
            if(map.containsKey(name)) {
                currentIndex = map.get(name);
                outLinksList = new TreeSet<>();
            } else {
                outLinksList = null;
            }
        }
        // NEW SUBJECT
        if(!last.toString().equals(subject.toString())) {
            if(outLinksList != null && !outLinksList.isEmpty()) {
                graph[currentIndex] = outLinksList.stream().mapToInt(a -> a).toArray();
            }

            last = subject;
            String name = last.toString();
            name = name.replace(entityIRI, "");
            if(map.containsKey(name)) {
                currentIndex = map.get(name);
                outLinksList = new TreeSet<>();
            } else {
                outLinksList = null;
            }
        }
        // PROPERTIES
        if((s.getObject() instanceof Literal)) return;
        String object = s.getObject().toString();
        String value = object.replace(entityIRI, "");
        if(outLinksList != null && map.containsKey(value)) {
            int valueId = map.get(value);
            if(!outLinksList.contains(valueId))
                outLinksList.add(valueId);
        }
    }

    void finish() {
        if(outLinksList != null && !outLinksList.isEmpty()) {
            graph[currentIndex] = outLinksList.stream().mapToInt(a -> a).toArray();
        }
    }
}
