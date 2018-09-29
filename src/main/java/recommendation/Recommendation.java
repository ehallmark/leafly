package recommendation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class Recommendation {
    @Getter @Setter
    private double overallSimilarity;
    @Getter @Setter
    private double effectSimilarity;
    @Getter @Setter
    private double flavorSimilarity;
    @Getter @Setter
    private double reviewSimilarity;
    @Getter @Setter
    private double typeSimilarity;
    @Getter @Setter
    private double lineageSimilarity;
    @Getter @Setter
    private String strain;
    public Recommendation(@NonNull String strain) {
        this.strain=strain;
    }

    @Override
    public String toString() {
        return "Strain: "+strain+"\n\tSimilarity: "+overallSimilarity +
                "\n\t\tEffect Similarity: "+effectSimilarity +
                "\n\t\tFlavor Similarity: "+flavorSimilarity +
                "\n\t\tReview Similarity: "+reviewSimilarity +
                "\n\t\tType Similarity: "+typeSimilarity +
                "\n\t\tLineage Similarity: "+lineageSimilarity;
    }
}
