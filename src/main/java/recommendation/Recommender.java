package recommendation;

import com.google.common.base.Functions;
import com.google.gson.Gson;
import database.Database;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import smile.classification.LogisticRegression;
import smile.classification.SoftClassifier;

import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class Recommender {
    private SimilarityEngine effectSim;
    private SimilarityEngine typeSim;
    private SimilarityEngine flavorSim;
    private SimilarityEngine parentSim;
    private Map<String,List<Object>> flavorData;
    private Map<String,List<Object>> typeData;
    private Map<String,Map<String,Double>> effectData;
    private StringSimilarity stringSimilarity;
    private LineageGraph lineageGraph;
    private StrainReviewSimilarityMatrix reviewsModel;
    private List<String> strains;
    public Recommender() throws SQLException {
        this(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
    }

    public Recommender(List<Map<String,Object>> reviewData) throws SQLException {
        // initialize categorical data similarity engines
        List<String> effects = Database.loadEffects().stream().distinct().sorted().collect(Collectors.toList());
        List<String> flavors = Database.loadFlavors().stream().distinct().sorted().collect(Collectors.toList());
        effectSim = new SimilarityEngine(effects, null);//Database.loadSimilarityMatrix("strain_effects", "strain_id", "effect", effects));
        typeSim = new SimilarityEngine(Arrays.asList("Hybrid", "Indica", "Sativa"), SimilarityEngine.TYPE_SIMILARITY_MATRIX);
        flavorSim = new SimilarityEngine(flavors,  null);//Database.loadSimilarityMatrix("strain_flavors", "strain_id", "flavor", flavors));
        strains = Database.loadStrains();
        parentSim = new SimilarityEngine(strains);

        // get actual data
        lineageGraph = new LineageGraph(Database.loadData("strain_lineage", "strain_id", "parent_strain_id"));
        flavorData = Database.loadMap("strain_flavors", "strain_id", "flavor");
        effectData = Database.loadMapWithValue("strain_effects", "strain_id", "effect", "effect_percent");
        typeData = Database.loadMap("strains", "id", "type");
        reviewsModel = new StrainReviewSimilarityMatrix(strains, reviewData);
        stringSimilarity = new StringSimilarity();

        System.out.println("Flavor data size: "+flavorData.size());
        System.out.println("Effect data size: "+effectData.size());
    }

    public Recommendation recommendationScoreFor(@NonNull String _strain, @NonNull Map<String,Double> previousStrainRatings, double alpha) {
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
        List<String> goodRatings = previousStrainRatings.entrySet()
                .stream().filter(e->e.getValue()>=5).map(e->e.getKey()).collect(Collectors.toList());
        final Map<String,Double> rScores = reviewsModel.similarity(goodRatings);
        return recommendationScoreFor(_strain, previousStrainRatings,
                knownEffects, knownFlavors, knownLineage, knownTypes, rScores, alpha);
    }

    public Recommendation recommendationScoreFor(@NonNull String _strain, Map<String,Double> previousStrainRatings,
                                                 Map<String,Double> knownEffects, Map<String,Double> knownFlavors, Map<String,Double> knownLineage,
                                                 Map<String,Double> knownTypes, Map<String,Double> rScores, double alpha) {
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
        //double rcScore = rcScores.getOrDefault(_strain, 0);
        double nScore = previousStrainRatings.keySet().stream()
                .mapToDouble(strain->
                        stringSimilarity.similarity(_strain.split("_")[2], strain.split("_")[2]))
                .average().orElse(0d);
        double tScore = typeSim.similarity(type, knownTypes);
        Recommendation recommendation = new Recommendation(_strain);
        recommendation.setEffectSimilarity(eScore);
        recommendation.setLineageSimilarity(lScore);
        recommendation.setFlavorSimilarity(fScore);
        recommendation.setTypeSimilarity(tScore);
        recommendation.setReviewSimilarity(rScore);
        recommendation.setNameSimilarity(nScore);
        double score = eScore + fScore + lScore + rScore + tScore + nScore;
        recommendation.setOverallSimilarity(score * alpha);
        return recommendation;
    }

    public List<Recommendation> topRecommendations(int n, @NonNull Map<String,Double> _previousStrainRatings, double alpha) {
        Set<String> previousStrains = new HashSet<>(_previousStrainRatings.keySet());
        Map<String,Double> previousStrainRatings = _previousStrainRatings.entrySet().stream()
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
        List<String> goodRatings = previousStrainRatings.entrySet()
                .stream().filter(e->e.getValue()>=5).map(e->e.getKey()).collect(Collectors.toList());
        final Map<String,Double> rScores = reviewsModel.similarity(goodRatings);
        Stream<Recommendation> stream = strains.stream().filter(strain->!previousStrains.contains(strain)).map(_strain->{
            return recommendationScoreFor(_strain, previousStrainRatings,
                    knownEffects, knownFlavors, knownLineage, knownTypes, rScores, alpha);
        });
        if(n > 0) {
            return stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n).collect(Collectors.toList());
        } else {
            return stream.collect(Collectors.toList());
        }
    }
}
