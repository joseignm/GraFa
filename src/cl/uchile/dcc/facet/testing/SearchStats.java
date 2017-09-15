package cl.uchile.dcc.facet.testing;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class SearchStats {

    private List<DataEntry> dataStats;
    private List<PropertyEntry> propertiesStats;

    SearchStats() {
        dataStats = new ArrayList<>();
        propertiesStats = new ArrayList<>();
    }

    synchronized void addDataSearch(DataEntry entry) {
        dataStats.add(entry);
    }

    synchronized void addPropertySearch(PropertyEntry entry) {
        propertiesStats.add(entry);
    }

    void printDataStats(PrintStream ps) {
        ps.println("URL,time,results,depth,properties,size");
        for(DataEntry entry : dataStats) {
            entry.printAsCsv(ps);
        }
    }

    void printPropertiesStats(PrintStream ps) {
        ps.println("URL,values,selected,time");
        for(PropertyEntry entry : propertiesStats) {
            entry.printAsCsv(ps);
        }
    }

}
