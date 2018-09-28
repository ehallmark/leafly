package recommendation;

import com.google.common.base.Functions;
import com.google.gson.Gson;
import database.Database;
import javafx.util.Pair;
import lombok.NonNull;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Recommender {
    private static final double DEFAULT_E_WEIGHT = 1d;
    private static final double DEFAULT_F_WEIGHT = 1d;
    private static final double DEFAULT_L_WEIGHT = 1d;
    private static final double DEFAULT_R_WEIGHT = 1d;
    private static final double DEFAULT_T_WEIGHT = 1d;


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
    // weights
    private double[] weights;
    public Recommender() throws SQLException {
        this(new double[]{DEFAULT_E_WEIGHT, DEFAULT_F_WEIGHT, DEFAULT_L_WEIGHT, DEFAULT_R_WEIGHT, DEFAULT_T_WEIGHT});
    }

    public Recommender(double[] weights) throws SQLException {
        this.weights=weights;
        // initialize categorical data similarity engines
        effectSim = new SimilarityEngine(Database.loadEffects());
        typeSim = new SimilarityEngine(Arrays.asList("Hybrid", "Indica", "Sativa"));
        flavorSim = new SimilarityEngine(Database.loadFlavors());
        strains = Database.loadStrains();
        parentSim = new SimilarityEngine(strains);

        // get actual data
        lineageGraph = new LineageGraph(Database.loadData("strain_lineage", "strain_id", "parent_strain_id"));
        flavorData = Database.loadMap("strain_flavors", "strain_id", "flavor");
        effectData = Database.loadMapWithValue("strain_effects", "strain_id", "effect", "effect_percent");
        typeData = Database.loadMap("strains", "id", "type");
        reviewsModel = new ReviewsModel(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));

        System.out.println("Flavor data size: "+flavorData.size());
        System.out.println("Effect data size: "+effectData.size());
    }


    public List<Pair<String,Double>> topRecommendations(int n, @NonNull Map<String,Double> previousStrainRatings) {
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
        return strains.stream().filter(strain->!previousStrainRatings.containsKey(strain)).map(strain->{
            Map<String, Double> effects = effectData.getOrDefault(strain, Collections.emptyMap());
            Map<String, Double> flavors = flavorData.getOrDefault(strain, Collections.emptyList())
                    .stream().collect(Collectors.toMap(Object::toString, e->1d));
            Map<String,Double> lineage = lineageGraph.getAncestorsOf(strain)
                                .stream().collect(Collectors.toMap(Functions.identity(),e->1d));
            Map<String,Double> type = typeData.getOrDefault(strain, Collections.emptyList())
                    .stream().collect(Collectors.toMap(Object::toString, e->1d));
            double eScore = effectSim.similarity(effects, knownEffects) * weights[0];
            double fScore = flavorSim.similarity(flavors, knownFlavors) * weights[1];
            double lScore = parentSim.similarity(lineage, knownLineage) * weights[2];
            double rScore = rScores.getOrDefault(strain, 0d) * weights[3];
            double tScore = typeSim.similarity(type, knownTypes) * weights[4];
            double score = eScore + fScore + lScore + rScore + tScore;
            return new Pair<>(strain, score);

        }).sorted((e1,e2)->e2.getValue().compareTo(e1.getValue())).limit(n).collect(Collectors.toList());
    }




    public static void main(String[] args) throws Exception {
        Random rand = new Random(2352);
        // should add genetic fingerprint data when applicable

        Recommender recommender = new Recommender();

        // iterate thru strains and randomly generate prior ratings to get new suggestions
        List<String> strains = Database.loadStrains();

        for(int i = 0; i < 1000; i++) {
            int numPrior = rand.nextInt(10)+1;
            Map<String,Double> ratings = new HashMap<>();
            for(int j = 0; j < numPrior; j++) {
                ratings.put(strains.get(rand.nextInt(strains.size())), 1d+rand.nextInt(4));
            }
            List<Pair<String,Double>> topRecommendations = recommender.topRecommendations(5, ratings);
            System.out.println("----------------------------------------------------------------------------------");
            System.out.println("Recommendation for: "+new Gson().toJson(ratings));
            for(Pair<String,Double> recommendation : topRecommendations) {
                System.out.println("Recommended "+recommendation.getKey()+" with confidence: "+recommendation.getValue());
            }
        }
    }
}
