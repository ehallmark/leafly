package recommendation;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;

public class StringSimilarity {
    private StringDistance stringDistance;
    public StringSimilarity(int k) {
        this.stringDistance = new Cosine(k);
    }
    public double similarity(String name1, String name2) {
        return 1.0 - stringDistance.distance(name1, name2);
    }
}
