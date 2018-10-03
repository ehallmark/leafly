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
    private Map<String,List<Object>> brandData;
    private Map<String,List<Object>> strainData;
    private Map<String,List<Object>> typeData;
    private Map<String,List<Object>> subTypeData;
    private ProductReviewModel productReviewModel;
    private SimilarityEngine brandSimilarity;
    private SimilarityEngine typeSimilarity;
    private StringSimilarity nameSimilarity;
    public ProductRecommender() throws SQLException {
        strainRecommender = new StrainRecommender();
        this.products = Database.loadProducts();
        productReviewModel = new ProductReviewModel(this.products, Database.loadData("product_reviews",
                "product_id", "author", "rating", "upvotes", "downvotes"));
        brandSimilarity = new SimilarityEngine(Database.loadBrands());
        List<String> types = new ArrayList<>(Database.loadTypes());
        types.addAll(Database.loadSubTypes());
        nameSimilarity = new StringSimilarity(4);
        typeSimilarity = new SimilarityEngine(types);

        strainData = Database.loadMap("products", "product_id", "strain_id");
        brandData = Database.loadMap("products", "product_id", "brand_name");
        subTypeData = Database.loadMap("products", "product_id", "subtype");
        typeData = Database.loadMap("products", "product_id", "type");

    }

    @Override
    public ProductRecommendation recommendationScoreFor(@NonNull String _productId, Map<String, Object> data) {
        Map<String,Double> strainSimilarityMap = (Map<String,Double>) data.get("strainSimilarityMap");
        Map<String,Double> knownBrands = (Map<String,Double>)data.get("knownBrands");
        Map<String,Double> knownTypesAndSubtypes = (Map<String,Double>)data.get("knownTypesAndSubtypes");
        Map<String,Double> rScores = (Map<String,Double>)data.get("rScores");
        Map<String,Double> previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        double alpha = (Double)data.get("alpha");
        List<String> associatedStrains = strainData.getOrDefault(_productId, Collections.emptyList())
                .stream().filter(o->o!=null).map(Object::toString).collect(Collectors.toList());
        Map<String, Double> brands = brandData.getOrDefault(_productId, Collections.emptyList())
                .stream().collect(Collectors.toMap(Object::toString, e->1d));
        Map<String, Double> types = typeData.getOrDefault(_productId, Collections.emptyList())
                .stream().collect(Collectors.toMap(Object::toString, e->1d));
        Map<String, Double> subtypes = subTypeData.getOrDefault(_productId, Collections.emptyList())
                .stream().collect(Collectors.toMap(Object::toString, e->1d));
        Map<String,Double> typesAndSubTypes = new HashMap<>(types);
        typesAndSubTypes.putAll(subtypes);
        double sScore = 1d * associatedStrains.stream().mapToDouble(strain->{
            return strainSimilarityMap.getOrDefault(strain, 0.5);
        }).average().orElse(0.5);
        double bScore = 0.25 * brandSimilarity.similarity(brands, knownBrands);
        double tScore = 0.1 * typeSimilarity.similarity(typesAndSubTypes, knownTypesAndSubtypes);
        double rScore = 2d * rScores.getOrDefault(_productId, 0d);
        double nScore = 0.2 * previousProductRatings.keySet().stream()
                .mapToDouble(product->
                        nameSimilarity.similarity(_productId, product))
                .average().orElse(0d);
        ProductRecommendation recommendation = new ProductRecommendation(_productId);
        recommendation.setBrandSimilarity(bScore);
        recommendation.setTypeSimilarity(tScore);
        recommendation.setReviewSimilarity(rScore);
        recommendation.setNameSimilarity(nScore);
        recommendation.setStrainSimilarity(sScore);
        double score = bScore + rScore + tScore + nScore + sScore;
        recommendation.setOverallSimilarity(score * alpha);
        return recommendation;
    }

    @Override
    public List<ProductRecommendation> topRecommendations(int n, @NonNull Map<String,Object> data) {
        Map<String,Double> _previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        Map<String,Double> previousProductRatings = _previousProductRatings.entrySet().stream()
                .filter(e->e.getValue()>=3.5)
                .collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        if(previousProductRatings.isEmpty()) {
            throw new RuntimeException("Unable to get recommendations without previous strain ratings...");
        }
        Set<String> previousProducts = new HashSet<>(previousProductRatings.keySet());
        List<String> goodProducts = new ArrayList<>(previousProducts);
        String currentProductId = (String) data.get("currentProductId");
        Map<String,Double> knownTypesAndSubtypes = new HashMap<>();
        Map<String,Double> knownBrands = new HashMap<>();
        Map<String,Double> rScores = productReviewModel.similarity(goodProducts);

        previousProductRatings.forEach((product, score)->{
            List<String> prevBrands = brandData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevTypes = typeData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevSubTypes = subTypeData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            for(String brand : prevBrands) {
                knownBrands.putIfAbsent(brand, 0d);
                knownBrands.put(brand, knownBrands.get(brand)+1d);
            }
            for(String type : prevTypes) {
                knownTypesAndSubtypes.putIfAbsent(type, 0d);
                knownTypesAndSubtypes.put(type, knownTypesAndSubtypes.get(type)+1d);
            }
            for(String subtype : prevSubTypes) {
                knownTypesAndSubtypes.putIfAbsent(subtype, 0d);
                knownTypesAndSubtypes.put(subtype, knownTypesAndSubtypes.get(subtype)+1d);
            }
        });
        Map<String,Double> strainSimilarityMap = strainRecommender
                .topRecommendations(-1, data)
                .stream().collect(Collectors.toMap(e->e.getStrain(),e->e.getOverallSimilarity()));

        Map<String,Object> newData = new HashMap<>(data);
        newData.put("strainSimilarityMap", strainSimilarityMap);
        newData.put("currentProductId", currentProductId);
        newData.put("knownBrands", knownBrands);
        newData.put("knownTypesAndSubtypes", knownTypesAndSubtypes);
        newData.put("rScores", rScores);
        newData.put("alpha", 0.2);

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
