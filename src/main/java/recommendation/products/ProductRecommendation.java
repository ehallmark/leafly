package recommendation.products;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class ProductRecommendation {
    @Getter @Setter
    private double overallSimilarity;
    @Getter @Setter
    private double strainSimilarity;
    @Getter @Setter
    private double brandSimilarity;
    @Getter @Setter
    private double descriptionSimilarity;
    @Getter @Setter
    private double nameSimilarity;
    @Getter @Setter
    private double reviewSimilarity;
    @Getter @Setter
    private double typeSimilarity;
    @Getter @Setter
    private String productId;
    public ProductRecommendation(@NonNull String productId) {
        this.productId = productId;
    }

    @Override
    public String toString() {
        return "Product: " + productId + "\n\tSimilarity: " + formatDouble(overallSimilarity)
                + "\n\tStrain Similarity: " + strainSimilarity
                + "\n\tBrand Similarity: " + brandSimilarity
                + "\n\tType Similarity: " + typeSimilarity
                + "\n\tReview Similarity: " + reviewSimilarity
                + "\n\tDescription Similarity: "+descriptionSimilarity
                + "\n\tName Similarity: " + nameSimilarity;
    }

    private static String formatDouble(double x) {
        return String.format("%.2f", x);
    }

}
