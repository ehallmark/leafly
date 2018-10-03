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
    private ProductReviewModel productReviewModel;
    public ProductRecommender() throws SQLException {
        strainRecommender = new StrainRecommender();
        this.products = Database.loadProducts();
        productReviewModel = new ProductReviewModel();
    }

    public ProductRecommendation recommendationScoreFor(@NonNull String _productId, Map<String, Object> data) {
        ProductRecommendation productRecommendation = new ProductRecommendation(_productId);

        Map<String,Double> strainSimilarityMap = (Map<String,Double>) data.get("strainSimilarityMap");

        // double strainSim = strainRecommender.recommendationScoreFor()
        // productRecommendation.setStrainSimilarity(strainSim);

        return productRecommendation;
    }

    public List<ProductRecommendation> topRecommendations(int n, @NonNull Map<String,Object> data) {
        Map<String,Double> _previousStrainRatings = (Map<String,Double>)data.get("previousStrainRatings");
        Map<String,Double> _previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        Map<String,Double> previousStrainRatings = _previousStrainRatings.entrySet().stream()
                .filter(e->e.getValue()>=3.5)
                .collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));
        Map<String,Double> previousProductRatings = _previousProductRatings.entrySet().stream()
                .filter(e->e.getValue()>=3.5)
                .collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        if(previousProductRatings.isEmpty()) {
            throw new RuntimeException("Unable to get recommendations without previous strain ratings...");
        }
        Set<String> previousProducts = new HashSet<>(previousProductRatings.keySet());
        String currentProductId = (String) data.get("currentProductId");
        double alpha = (Double)data.get("alpha");
        Map<String,Double> knownTypes = new HashMap<>();
        Map<String,Double> knownSubtypes = new HashMap<>();
        Map<String,Double> rScores = productReviewModel.similarity(previousProductRatings);

        Map<String,Double> strainSimilarityMap = strainRecommender
                .topRecommendations(-1, data)
                .stream().collect(Collectors.toMap(e->e.getStrain(),e->e.getOverallSimilarity()));

        Map<String,Object> newData = new HashMap<>(data);
        newData.put("strainSimilarityMap", strainSimilarityMap);

        Stream<ProductRecommendation> stream = products.stream().filter(strain->!previousProducts.contains(strain)).map(product->{
            return recommendationScoreFor(product, newData);
        });
        if(n > 0) {
            return stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n).collect(Collectors.toList());
        } else {
            return stream.collect(Collectors.toList());
        }
    }
}
