package recommendation;

import com.google.common.base.Functions;
import com.google.gson.Gson;
import database.Database;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import smile.classification.LogisticRegression;

import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Recommender {
    private SimilarityEngine effectSim;
    private SimilarityEngine typeSim;
    private SimilarityEngine flavorSim;
    private SimilarityEngine parentSim;
    private Map<String,List<Object>> flavorData;
    private Map<String,List<Object>> typeData;
    private Map<String,Map<String,Double>> effectData;
    private LineageGraph lineageGraph;
    private ReviewsModel reviewsModel;
    private List<String> strains;
    public Recommender() throws SQLException {
        this(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
    }

    public Recommender(List<Map<String,Object>> reviewData) throws SQLException {
        // initialize categorical data similarity engines
        effectSim = new SimilarityEngine(Database.loadEffects());
        typeSim = new SimilarityEngine(Arrays.asList("Hybrid", "Indica", "Sativa"), SimilarityEngine.TYPE_SIMILARITY_MATRIX);
        flavorSim = new SimilarityEngine(Database.loadFlavors());
        strains = Database.loadStrains();
        parentSim = new SimilarityEngine(strains);

        // get actual data
        lineageGraph = new LineageGraph(Database.loadData("strain_lineage", "strain_id", "parent_strain_id"));
        flavorData = Database.loadMap("strain_flavors", "strain_id", "flavor");
        effectData = Database.loadMapWithValue("strain_effects", "strain_id", "effect", "effect_percent");
        typeData = Database.loadMap("strains", "id", "type");
        reviewsModel = new ReviewsModel(reviewData);

        System.out.println("Flavor data size: "+flavorData.size());
        System.out.println("Effect data size: "+effectData.size());
    }

    public Recommendation recommendationScoreFor(@NonNull String _strain, @NonNull Map<String,Double> previousStrainRatings, LogisticRegression logit) {
        Map<String,Double> knownFlavors = new HashMap<>();
        Map<String,Double> knownEffects = new HashMap<>();
        Map<String,Double> knownTypes = new HashMap<>();
        Map<String,Double> knownLineage = new HashMap<>();
        previousStrainRatings.forEach((strain, score)->{
            Map<String,Double> prevEffects = effectData.getOrDefault(strain, Collections.emptyMap());
            List<String> prevFlavors = flavorData.getOrDefault(strain, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevTypes = typeData.getOrDefault(strain, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            Collection<String> prevLineage = lineageGraph.getAncestorsOf(strain);
            for(String effect : prevEffects.keySet()) {
                knownEffects.putIfAbsent(effect, 0d);
                knownEffects.put(effect, knownEffects.get(effect)+prevEffects.get(effect));
            }
            for(String flavor : prevFlavors) {
                knownFlavors.putIfAbsent(flavor, 0d);
                knownFlavors.put(flavor, knownFlavors.get(flavor)+1d);
            }
            for(String ancestorId : prevLineage) {
                knownLineage.putIfAbsent(ancestorId, 0d);
                knownLineage.put(ancestorId, knownLineage.get(ancestorId)+1d);
            }
            for(String type : prevTypes) {
                knownTypes.putIfAbsent(type, 0d);
                knownTypes.put(type, knownTypes.get(type)+1d);
            }
        });
        Map<String,Double> normalizedRatings = previousStrainRatings.entrySet()
                .stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue()-2.5));
        final Map<String,Double> rScores = reviewsModel.similarity(normalizedRatings);
        Map<String,Integer> rcScores = reviewsModel.getReviewSizeMap();
        return recommendationScoreFor(_strain, logit,
                knownEffects, knownFlavors, knownLineage, knownTypes, rScores, rcScores);
    }

    public Recommendation recommendationScoreFor(@NonNull String _strain, LogisticRegression logit,
                                                 Map<String,Double> knownEffects, Map<String,Double> knownFlavors, Map<String,Double> knownLineage,
                                                 Map<String,Double> knownTypes, Map<String,Double> rScores, Map<String,Integer> rcScores) {

        Map<String, Double> effects = effectData.getOrDefault(_strain, Collections.emptyMap());
        Map<String, Double> flavors = flavorData.getOrDefault(_strain, Collections.emptyList())
                .stream().collect(Collectors.toMap(Object::toString, e->1d));
        Map<String,Double> lineage = lineageGraph.getAncestorsOf(_strain)
                .stream().collect(Collectors.toMap(Functions.identity(),e->1d));
        Map<String,Double> type = typeData.getOrDefault(_strain, Collections.emptyList())
                .stream().collect(Collectors.toMap(Object::toString, e->1d));

        double eScore = effectSim.similarity(effects, knownEffects);
        double fScore = flavorSim.similarity(flavors, knownFlavors);
        double lScore = parentSim.similarity(lineage, knownLineage);
        double rScore = rScores.getOrDefault(_strain, 0d);
        double rcScore = rcScores.getOrDefault(_strain, 0);
        double tScore = typeSim.similarity(type, knownTypes);
        Recommendation recommendation = new Recommendation(_strain);
        recommendation.setEffectSimilarity(eScore);
        recommendation.setLineageSimilarity(lScore);
        recommendation.setFlavorSimilarity(fScore);
        recommendation.setTypeSimilarity(tScore);
        recommendation.setReviewSimilarity(rScore);
        recommendation.setNumReviews(rcScore);
        double score;
        if(logit==null) {
            score = eScore + fScore + lScore + rScore + tScore;
        } else {
            double[] post = new double[2];
            logit.predict(new double[]{
                    recommendation.getEffectSimilarity(),
                    recommendation.getFlavorSimilarity(),
                    recommendation.getLineageSimilarity(),
                    recommendation.getReviewSimilarity(),
                    recommendation.getTypeSimilarity(),
                    recommendation.getNumReviews()
            }, post);
            score = post[1];
        }
        recommendation.setOverallSimilarity(score);
        return recommendation;
    }

    public List<Recommendation> topRecommendations(int n, @NonNull Map<String,Double> previousStrainRatings, LogisticRegression logit) {
        Set<String> previousStrains = new HashSet<>(previousStrainRatings.keySet());
        previousStrainRatings = previousStrainRatings.entrySet().stream()
                .filter(e->e.getValue()>=3.5)
                .collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        if(previousStrainRatings.isEmpty()) {
            throw new RuntimeException("Unable to get recommendations without previous strain ratings...");
        }
        Map<String,Double> knownFlavors = new HashMap<>();
        Map<String,Double> knownEffects = new HashMap<>();
        Map<String,Double> knownTypes = new HashMap<>();
        Map<String,Double> knownLineage = new HashMap<>();
        previousStrainRatings.forEach((strain, score)->{
            Map<String,Double> prevEffects = effectData.getOrDefault(strain, Collections.emptyMap());
            List<String> prevFlavors = flavorData.getOrDefault(strain, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            List<String> prevTypes = typeData.getOrDefault(strain, Collections.emptyList())
                    .stream().map(Object::toString).collect(Collectors.toList());
            Collection<String> prevLineage = lineageGraph.getAncestorsOf(strain);
            for(String effect : prevEffects.keySet()) {
                knownEffects.putIfAbsent(effect, 0d);
                knownEffects.put(effect, knownEffects.get(effect)+prevEffects.get(effect));
            }
            for(String flavor : prevFlavors) {
                knownFlavors.putIfAbsent(flavor, 0d);
                knownFlavors.put(flavor, knownFlavors.get(flavor)+1d);
            }
            for(String ancestorId : prevLineage) {
                knownLineage.putIfAbsent(ancestorId, 0d);
                knownLineage.put(ancestorId, knownLineage.get(ancestorId)+1d);
            }
            for(String type : prevTypes) {
                knownTypes.putIfAbsent(type, 0d);
                knownTypes.put(type, knownTypes.get(type)+1d);
            }
        });
        Map<String,Double> normalizedRatings = previousStrainRatings.entrySet()
                .stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue()-2.5));
        final Map<String,Double> rScores = reviewsModel.similarity(normalizedRatings);
        final Map<String,Integer> rcScores = reviewsModel.getReviewSizeMap();
        Stream<Recommendation> stream = strains.stream().filter(strain->!previousStrains.contains(strain)).map(_strain->{
            return recommendationScoreFor(_strain, logit,
                    knownEffects, knownFlavors, knownLineage, knownTypes, rScores, rcScores);
        });
        if(n > 0) {
            return stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n).collect(Collectors.toList());
        } else {
            return stream.collect(Collectors.toList());
        }
    }

    public static void main(String[] args) throws Exception {
        Random rand = new Random(2352);
        LogisticRegression logit = TrainRecommender.loadLogitModel();
        // should add genetic fingerprint data when applicable
        Recommender recommender = new Recommender();

        // iterate thru strains and randomly generate prior ratings to get new suggestions
        List<String> strains = Database.loadStrains();

        for(int i = 0; i < 1000; i++) {
            int numPrior = rand.nextInt(10)+1;
            Map<String,Double> ratings = new HashMap<>();
            for(int j = 0; j < numPrior; j++) {
                ratings.put(strains.get(rand.nextInt(strains.size())), 4d+rand.nextInt(1));
            }
            List<Recommendation> topRecommendations = recommender.topRecommendations(5, ratings, logit);
            System.out.println("----------------------------------------------------------------------------------");
            System.out.println("Recommendation for: "+new Gson().toJson(ratings));
            for(Recommendation recommendation : topRecommendations) {
                System.out.println("Recommended: "+recommendation.toString());
            }
        }
    }
}
