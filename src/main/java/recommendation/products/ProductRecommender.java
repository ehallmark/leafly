package recommendation.products;

import com.google.common.base.Functions;
import database.Database;
import lombok.NonNull;
import recommendation.Recommender;
import recommendation.strains.LineageGraph;
import recommendation.SimilarityEngine;
import recommendation.StringSimilarity;
import recommendation.strains.StrainRecommendation;
import recommendation.strains.StrainRecommender;
import recommendation.strains.StrainReviewSimilarityMatrix;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductRecommender implements Recommender<ProductRecommendation> {
    private StrainRecommender strainRecommender;
    public ProductRecommender() throws SQLException {
        strainRecommender = new StrainRecommender();
    }

    public ProductRecommendation recommendationScoreFor(@NonNull String _productId, Map<String, Object> data) {
        ProductRecommendation productRecommendation = new ProductRecommendation(_productId);
        // double strainSim = strainRecommender.recommendationScoreFor()
        // productRecommendation.setStrainSimilarity(strainSim);

        return productRecommendation;
    }

    public List<ProductRecommendation> topRecommendations(int n, @NonNull Map<String,Object> data) {
        Map<String,Double> previousStrainRatings = (Map<String,Double>)data.get("previousStrainRatings");
        Map<String,Double> previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        List<ProductRecommendation> recommendations = new ArrayList<>();


        return recommendations;
    }
}
