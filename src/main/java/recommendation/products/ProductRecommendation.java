package recommendation.products;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class ProductRecommendation {
    @Getter
    @Setter
    private double overallSimilarity;
    @Getter
    @Setter
    private double strainSimilarity;
    @Getter
    @Setter
    private String productId;

    public ProductRecommendation(@NonNull String productId) {
        this.productId = productId;
    }

    @Override
    public String toString() {
        return "Product: " + productId + "\n\tSimilarity: " + formatDouble(overallSimilarity)
                + "\n\tStrain Similarity: " + strainSimilarity;
    }

    private static String formatDouble(double x) {
        return String.format("%.2f", x);
    }

}
