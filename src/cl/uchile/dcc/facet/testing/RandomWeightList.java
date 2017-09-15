package cl.uchile.dcc.facet.testing;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class RandomWeightList<E> {

    private final Random random = ThreadLocalRandom.current();
    private final NavigableMap<Double, E> map;
    private double total;

    RandomWeightList() {
        map = new TreeMap<>();
        total = 0;
    }

    void add(E element, double weight) {
        total += weight;
        map.put(total, element);
    }

    E nextElement() {
        double key = random.nextDouble() * total;
        Map.Entry<Double, E> result = map.higherEntry(key);
        return result.getValue();
    }

    int size() {
        return map.size();
    }
}
