package cl.uchile.dcc.facet.testing;

import java.io.PrintStream;

class DataEntry {
    private String URL;
    private long time;
    private int results;
    private int depth;
    private int properties;
    private int size;

    DataEntry(String URL, long time, int results, int depth, int properties, int size) {
        this.URL = URL;
        this.time = time;
        this.results = results;
        this.depth = depth;
        this.properties = properties;
        this.size = size;
    }

    void printAsCsv(PrintStream ps) {
        ps.print(URL);
        ps.print(",");
        ps.print(time);
        ps.print(",");
        ps.print(results);
        ps.print(",");
        ps.print(depth);
        ps.print(",");
        ps.print(properties);
        ps.print(",");
        ps.print(size);
        ps.println();
    }

}
