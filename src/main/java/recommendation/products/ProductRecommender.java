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
    private List<String> products;
    public ProductRecommender() throws SQLException {
        strainRecommender = new StrainRecommender();
        this.products = Database.loadProducts();
    }

    public ProductRecommendation recommendationScoreFor(@NonNull String _productId, Map<String, Object> data) {
        ProductRecommendation productRecommendation = new ProductRecommendation(_productId);

        Map<String,Double> strainSimilarityMap = (Map<String,Double>) data.get("strainSimilarityMap");

        // double strainSim = strainRecommender.recommendationScoreFor()
        // productRecommendation.setStrainSimilarity(strainSim);

        return productRecommendation;
    }

    public List<ProductRecommendation> topRecommendations(int n, @NonNull Map<String,Object> data) {
        Map<String,Double> previousStrainRatings = (Map<String,Double>)data.get("previousStrainRatings");
        Map<String,Double> previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        Set<String> previousProducts = new HashSet<>(previousProductRatings.keySet());
        double alpha = (Double)data.get("alpha");

        Map<String,Double> strainSimilarityMap = strainRecommender
                .topRecommendations(-1, data)
                .stream().collect(Collectors.toMap(e->e.getStrain(),e->e.getOverallSimilarity()));

        Map<String,Object> newData = new HashMap<>(data);
        newData.put("strainSimilarityMap", strainSimilarityMap);

        List<ProductRecommendation> recommendations = new ArrayList<>();

        Stream<ProductRecommendation> stream = products.stream().filter(strain->!previousProducts.contains(strain)).map(_strain->{
            return recommendationScoreFor(_strain, data);
        });
        if(n > 0) {
            return stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n).collect(Collectors.toList());
        } else {
            return stream.collect(Collectors.toList());
        }
    }
}
