package recommendation.products;

import com.google.common.base.Functions;
import database.Database;
import javafx.util.Pair;
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
    static Map<String,Double> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put("dabbingstorage", 0.0);
        TYPE_MAP.put("dabbingbooks-games", 0.0);
        TYPE_MAP.put("dabbingedibles", 0.1);
        TYPE_MAP.put("dabbingcannabis", 0.15);
        TYPE_MAP.put("dabbinggrowing", 0.0);
        TYPE_MAP.put("dabbingsmoking", 0.15);
        TYPE_MAP.put("dabbinghemp-cbd", 0.1);
        TYPE_MAP.put("dabbingconcentrates", 0.1);
        TYPE_MAP.put("dabbingtopicals", 0.1);
        TYPE_MAP.put("dabbingtourism", 0.0);
        TYPE_MAP.put("dabbingservices", 0.0);
        TYPE_MAP.put("dabbingpets", 0.0);
        TYPE_MAP.put("dabbingother", 0.0);
        TYPE_MAP.put("dabbingvaping", 0.15);
        TYPE_MAP.put("dabbingapparel", 0.0);
        TYPE_MAP.put("storagebooks-games", 0.0);
        TYPE_MAP.put("storageedibles", 0.0);
        TYPE_MAP.put("storagecannabis", 0.0);
        TYPE_MAP.put("storagegrowing", 0.0);
        TYPE_MAP.put("storagesmoking", 0.0);
        TYPE_MAP.put("storagehemp-cbd", 0.0);
        TYPE_MAP.put("storageconcentrates", 0.0);
        TYPE_MAP.put("storagetopicals", 0.0);
        TYPE_MAP.put("storagetourism", 0.0);
        TYPE_MAP.put("storageservices", 0.0);
        TYPE_MAP.put("storagepets", 0.0);
        TYPE_MAP.put("storageother", 0.0);
        TYPE_MAP.put("storagevaping", 0.0);
        TYPE_MAP.put("storageapparel", 0.0);
        TYPE_MAP.put("books-gamesedibles", 0.0);
        TYPE_MAP.put("books-gamescannabis", 0.0);
        TYPE_MAP.put("books-gamesgrowing", 0.0);
        TYPE_MAP.put("books-gamessmoking", 0.0);
        TYPE_MAP.put("books-gameshemp-cbd", 0.0);
        TYPE_MAP.put("books-gamesconcentrates", 0.0);
        TYPE_MAP.put("books-gamestopicals", 0.0);
        TYPE_MAP.put("books-gamestourism", 0.0);
        TYPE_MAP.put("books-gamesservices", 0.0);
        TYPE_MAP.put("books-gamespets", 0.0);
        TYPE_MAP.put("books-gamesother", 0.0);
        TYPE_MAP.put("books-gamesvaping", 0.0);
        TYPE_MAP.put("books-gamesapparel", 0.0);
        TYPE_MAP.put("ediblescannabis", 0.15);
        TYPE_MAP.put("ediblesgrowing", 0.0);
        TYPE_MAP.put("ediblessmoking", 0.15);
        TYPE_MAP.put("edibleshemp-cbd", 0.15);
        TYPE_MAP.put("ediblesconcentrates", 0.15);
        TYPE_MAP.put("ediblestopicals", 0.1);
        TYPE_MAP.put("ediblestourism", 0.0);
        TYPE_MAP.put("ediblesservices", 0.0);
        TYPE_MAP.put("ediblespets", 0.0);
        TYPE_MAP.put("ediblesother", 0.0);
        TYPE_MAP.put("ediblesvaping", 0.1);
        TYPE_MAP.put("ediblesapparel", 0.0);
        TYPE_MAP.put("cannabisgrowing", 0.0);
        TYPE_MAP.put("cannabissmoking", 0.15);
        TYPE_MAP.put("cannabishemp-cbd", 0.15);
        TYPE_MAP.put("cannabisconcentrates", 0.15);
        TYPE_MAP.put("cannabistopicals", 0.15);
        TYPE_MAP.put("cannabistourism", 0.0);
        TYPE_MAP.put("cannabisservices", 0.0);
        TYPE_MAP.put("cannabispets", 0.0);
        TYPE_MAP.put("cannabisother", 0.0);
        TYPE_MAP.put("cannabisvaping", 0.15);
        TYPE_MAP.put("cannabisapparel", 0.0);
        TYPE_MAP.put("growingsmoking", 0.15);
        TYPE_MAP.put("growinghemp-cbd", 0.0);
        TYPE_MAP.put("growingconcentrates", 0.0);
        TYPE_MAP.put("growingtopicals", 0.0);
        TYPE_MAP.put("growingtourism", 0.0);
        TYPE_MAP.put("growingservices", 0.0);
        TYPE_MAP.put("growingpets", 0.0);
        TYPE_MAP.put("growingother", 0.0);
        TYPE_MAP.put("growingvaping", 0.0);
        TYPE_MAP.put("growingapparel", 0.0);
        TYPE_MAP.put("smokinghemp-cbd", 0.15);
        TYPE_MAP.put("smokingconcentrates", 0.15);
        TYPE_MAP.put("smokingtopicals", 0.1);
        TYPE_MAP.put("smokingtourism", 0.0);
        TYPE_MAP.put("smokingservices", 0.0);
        TYPE_MAP.put("smokingpets", 0.0);
        TYPE_MAP.put("smokingother", 0.0);
        TYPE_MAP.put("smokingvaping", 0.1);
        TYPE_MAP.put("smokingapparel", 0.0);
        TYPE_MAP.put("hemp-cbdconcentrates", 0.15);
        TYPE_MAP.put("hemp-cbdtopicals", 0.15);
        TYPE_MAP.put("hemp-cbdtourism", 0.0);
        TYPE_MAP.put("hemp-cbdservices", 0.0);
        TYPE_MAP.put("hemp-cbdpets", 0.0);
        TYPE_MAP.put("hemp-cbdother", 0.0);
        TYPE_MAP.put("hemp-cbdvaping", 0.15);
        TYPE_MAP.put("hemp-cbdapparel", 0.0);
        TYPE_MAP.put("concentratestopicals", 0.1);
        TYPE_MAP.put("concentratestourism", 0.0);
        TYPE_MAP.put("concentratesservices", 0.0);
        TYPE_MAP.put("concentratespets", 0.0);
        TYPE_MAP.put("concentratesother", 0.0);
        TYPE_MAP.put("concentratesvaping", 0.15);
        TYPE_MAP.put("concentratesapparel", 0.0);
        TYPE_MAP.put("topicalstourism", 0.0);
        TYPE_MAP.put("topicalsservices", 0.0);
        TYPE_MAP.put("topicalspets", 0.0);
        TYPE_MAP.put("topicalsother", 0.0);
        TYPE_MAP.put("topicalsvaping", 0.15);
        TYPE_MAP.put("topicalsapparel", 0.0);
        TYPE_MAP.put("tourismservices", 0.0);
        TYPE_MAP.put("tourismpets", 0.0);
        TYPE_MAP.put("tourismother", 0.0);
        TYPE_MAP.put("tourismvaping", 0.0);
        TYPE_MAP.put("tourismapparel", 0.0);
        TYPE_MAP.put("servicespets", 0.0);
        TYPE_MAP.put("servicesother", 0.0);
        TYPE_MAP.put("servicesvaping", 0.0);
        TYPE_MAP.put("servicesapparel", 0.0);
        TYPE_MAP.put("petsother", 0.0);
        TYPE_MAP.put("petsvaping", 0.0);
        TYPE_MAP.put("petsapparel", 0.0);
        TYPE_MAP.put("othervaping", 0.0);
        TYPE_MAP.put("otherapparel", 0.0);
        TYPE_MAP.put("vapingapparel", 0.0);
        try {
            Map<String,List<String>> subTypesByType = Database.loadSubTypesByType();
            subTypesByType.forEach((type, subtypes)-> {
                int n = subtypes.size();
                for (int i = 0; i < n; i++) {
                    String t1 = subtypes.get(i);
                    TYPE_MAP.put(t1+type, 0.5);
                    for (int j = i + 1; j < n; j++) {
                        String t2 = subtypes.get(j);
                        TYPE_MAP.put(t1 + t2, 1.0 / subtypes.size());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private StrainRecommender strainRecommender;
    private List<String> products;
    private Map<String,List<Object>> brandData;
    private Map<String,List<Object>> strainData;
    private Map<String,List<Object>> typeData;
    private Map<String,List<Object>> descriptionData;
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
        double[][] typeSimMatrix = new double[types.size()][types.size()];
        for(int i = 0; i < types.size(); i++) {
            for(int j = 0; j < types.size(); j++) {
                if (i == j) typeSimMatrix[i][j] = 1;
                else {
                    typeSimMatrix[i][j] = Math.max(TYPE_MAP.getOrDefault(types.get(i) + types.get(j), 0d), TYPE_MAP.getOrDefault(types.get(j) + types.get(i), 0d));
                }
            }
        }
        typeSimilarity = new SimilarityEngine(types, typeSimMatrix);

        strainData = Database.loadMap("products", "product_id", "strain_id");
        brandData = Database.loadMap("products", "product_id", "brand_name");
        subTypeData = Database.loadMap("products", "product_id", "subtype");
        typeData = Database.loadMap("products", "product_id", "type");
        descriptionData = Database.loadMap("products", "product_id", "description");

    }

    @Override
    public ProductRecommendation recommendationScoreFor(@NonNull String _productId, Map<String, Object> data) {
        Map<String,Double> strainSimilarityMap = (Map<String,Double>) data.get("strainSimilarityMap");
        Map<String,Double> knownBrands = (Map<String,Double>)data.get("knownBrands");
        Map<String,Double> knownTypesAndSubtypes = (Map<String,Double>)data.get("knownTypesAndSubtypes");
        Map<String,Double> rScores = (Map<String,Double>)data.get("rScores");
        Map<String,Double> previousProductRatings = (Map<String,Double>)data.get("previousProductRatings");
        List<String> previousDescriptions = (List<String>)data.get("previousDescriptions");
        double alpha = (Double)data.get("alpha");
        String description = descriptionData.getOrDefault(_productId, Collections.emptyList())
                .stream().findAny().map(o->o.toString()).orElse(null);
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
        double bScore = 0.05 * brandSimilarity.similarity(brands, knownBrands);
        double tScore = 0.25 * typeSimilarity.similarity(typesAndSubTypes, knownTypesAndSubtypes);
        double rScore = 2d * rScores.getOrDefault(_productId, 0d);
        double nScore = - 2.0 * previousProductRatings.keySet().stream()
                .mapToDouble(product->
                        nameSimilarity.similarity(_productId, product))
                .average().orElse(0d);
        double dScore = 1.0 * nameSimilarity.similarity(description, String.join(" ", previousDescriptions));
        ProductRecommendation recommendation = new ProductRecommendation(_productId);
        recommendation.setBrandSimilarity(bScore);
        recommendation.setTypeSimilarity(tScore);
        recommendation.setReviewSimilarity(rScore);
        recommendation.setNameSimilarity(nScore);
        recommendation.setStrainSimilarity(sScore);
        recommendation.setDescriptionSimilarity(dScore);
        double score = bScore + rScore + tScore + nScore + sScore + dScore;
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
        List<String> previousDescriptions = new ArrayList<>();
        previousProductRatings.forEach((product, score)->{
            List<String> prevBrands = brandData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevTypes = typeData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevSubTypes = subTypeData.getOrDefault(product, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            String description = (String)descriptionData.getOrDefault(product, Collections.emptyList())
                    .stream().findAny().orElse(null);
            if(description!=null) {
                previousDescriptions.add(description);
            }
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
        newData.put("previousDescriptions", previousDescriptions);
        newData.put("rScores", rScores);
        newData.put("alpha", 0.2);

        Stream<ProductRecommendation> stream = products.stream().filter(strain->!previousProducts.contains(strain))
                // filter for association rules

                .map(product->{
            return recommendationScoreFor(product, newData);
        });
        if(n > 0) {
            // TODO refine suggestions
            List<ProductRecommendation> recommendationsExtra = stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n*3).collect(Collectors.toList());
            // now narrow down to best n / 3
            double[] features = new double[6];
            for(int i = 0; i < recommendationsExtra.size(); i++) {
                ProductRecommendation recommendation = recommendationsExtra.get(i);
                features[0] += recommendation.getBrandSimilarity();
                features[1] += recommendation.getNameSimilarity();
                features[2] += recommendation.getDescriptionSimilarity();
                features[3] += recommendation.getReviewSimilarity();
                features[4] += recommendation.getStrainSimilarity();
                features[5] += recommendation.getTypeSimilarity();
            }
            return recommendationsExtra.stream().map(recommendation->{
                double[] recFeatures = new double[]{
                        recommendation.getBrandSimilarity(),
                        recommendation.getNameSimilarity(),
                        recommendation.getDescriptionSimilarity(),
                        recommendation.getReviewSimilarity(),
                        recommendation.getStrainSimilarity(),
                        recommendation.getTypeSimilarity()
                };
                double score = SimilarityEngine.cosineSimilarity(features, recFeatures);
                return new Pair<>(recommendation, score);
            }).sorted((e1,e2)->Double.compare(e2.getValue(), e1.getValue()))
                    .limit(n).map(e->e.getKey()).collect(Collectors.toList());

        } else {
            return stream.collect(Collectors.toList());
        }
    }

    public static void main(String[] args) throws Exception {
        // test
        List<String> types = Database.loadTypes();
        for(int i = 0; i < types.size(); i++) {
            for(int j = i+1; j < types.size(); j++) {
                System.out.println("TYPE_MAP.put(\""+types.get(i)+types.get(j)+"\", 0.0);");
            }
        }
    }
}
