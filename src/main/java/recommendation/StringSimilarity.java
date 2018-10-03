package recommendation;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;

public class StringSimilarity {
    private static StringDistance stringDistance = new Cosine(3);

    public double similarity(String name1, String name2) {
        return 1.0 - stringDistance.distance(name1, name2);
    }
}
