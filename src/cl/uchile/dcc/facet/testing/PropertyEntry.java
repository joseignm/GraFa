package cl.uchile.dcc.facet.testing;

import java.io.PrintStream;

class PropertyEntry {

    private String URL;
    private int values;
    private String selected;
    private long time;

    PropertyEntry(String URL, int values, String selected, long time) {
        this.URL = URL;
        this.values = values;
        this.selected = selected;
        this.time = time;
    }

    void printAsCsv(PrintStream ps) {
        ps.print(URL);
        ps.print(",");
        ps.print(values);
        ps.print(",");
        ps.print(selected);
        ps.print(",");
        ps.print(time);
        ps.println();
    }
}
