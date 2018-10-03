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
    private double nameSimilarity;
    @Getter @Setter
    private double lineageSimilarity;
    @Getter @Setter
    private String strain;
    public Recommendation(@NonNull String strain) {
        this.strain=strain;
    }

    @Override
    public String toString() {
        return "Type: "+typeFromId(strain)+"\nID: "+strain+"\n\tSimilarity: "+formatDouble(overallSimilarity) +
                "\n\t\tEffect Similarity: "+formatDouble(effectSimilarity) +
                "\n\t\tFlavor Similarity: "+formatDouble(flavorSimilarity) +
                "\n\t\tReview Similarity: "+formatDouble(reviewSimilarity) +
                "\n\t\tType Similarity: "+formatDouble(typeSimilarity) +
                "\n\t\tLineage Similarity: "+formatDouble(lineageSimilarity) +
                "\n\t\tName Similarity: "+formatDouble(nameSimilarity);
    }

    private static String formatDouble(double x) {
        return String.format("%.2f", x);
    }

    private static String typeFromId(String id) {
        if(id.startsWith("_indica")) {
            return "Indica";
        } else if (id.startsWith("_sativa")) {
            return "Sativa";
        } else if(id.startsWith("_hybrid")) {
            return "Hybrid";
        } else {
            throw new RuntimeException("Invalid strain id");
        }
    }
}
