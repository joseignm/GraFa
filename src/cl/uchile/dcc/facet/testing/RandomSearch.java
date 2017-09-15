package cl.uchile.dcc.facet.testing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class RandomSearch {

    private static final int TIMES = 10;

    public static void main(String[] args) throws InterruptedException, IOException {

        Thread[] threads = new Thread[TIMES];
        SearchStats ts = new SearchStats();

        for(int i = 0; i < TIMES; i++) {
            threads[i] = new SearchThread(ts);
            threads[i].setName("T"+i);
        }
        for(Thread t : threads) {
            t.start();
            //t.join();
        }

        for(Thread t : threads)
            t.join();

        PrintStream dataStream = new PrintStream(new FileOutputStream("data.csv"));
        PrintStream propStream = new PrintStream(new FileOutputStream("property.csv"));

        System.out.println("Search times:");
        ts.printDataStats(System.out);
        System.out.println("Properties times:");
        ts.printPropertiesStats(System.out);
    }
}
