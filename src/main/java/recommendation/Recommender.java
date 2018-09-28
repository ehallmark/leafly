package recommendation;

import com.google.common.base.Functions;
import database.Database;
import javafx.util.Pair;
import lombok.NonNull;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Recommender {
    private SimilarityEngine effectSim;
    private SimilarityEngine flavorSim;
    private SimilarityEngine parentSim;
    private Map<String,List<Object>> flavorData;
    private Map<String,Map<String,Double>> effectData;
    private LineageGraph lineageGraph;
    private ReviewsModel reviewsModel;
    private List<String> strains;

    public Recommender() throws SQLException {
        // initialize categorical data similarity engines
        effectSim = new SimilarityEngine(Database.loadEffects());
        flavorSim = new SimilarityEngine(Database.loadFlavors());
        strains = Database.loadStrains();
        parentSim = new SimilarityEngine(strains);

        // get actual data
        lineageGraph = new LineageGraph(Database.loadData("strain_lineage", "strain_id", "parent_strain_id"));
        flavorData = Database.loadMap("strain_flavors", "strain_id", "flavor");
        effectData = Database.loadMapWithValue("strain_effects", "strain_id", "effect", "effect_percent");
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
        Map<String,Double> knownLineage = new HashMap<>();
        previousStrainRatings.forEach((strain, score)->{
            Map<String,Double> prevEffects = effectData.getOrDefault(strain, Collections.emptyMap());
            List<String> prevFlavors = flavorData.getOrDefault(strain, Collections.emptyList())
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
        });
        final Map<String,Double> rScores = reviewsModel.similarity(previousStrainRatings);
        return strains.stream().filter(previousStrainRatings::containsKey).map(strain->{
            Map<String, Double> effects = effectData.getOrDefault(strain, Collections.emptyMap());
            Map<String, Double> flavors = flavorData.getOrDefault(strain, Collections.emptyList())
                    .stream().collect(Collectors.toMap(Object::toString, e->1d));
            Map<String,Double> lineage = lineageGraph.getAncestorsOf(strain)
                                .stream().collect(Collectors.toMap(Functions.identity(),e->1d));

            double eScore = effectSim.similarity(effects, knownEffects);
            double fScore = flavorSim.similarity(flavors, knownFlavors);
            double lScore = parentSim.similarity(lineage, knownLineage);
            double rScore = rScores.getOrDefault(strain, 0d);
            double score = eScore + fScore + lScore + rScore;
            return new Pair<>(strain, score);

        }).sorted((e1,e2)->e2.getValue().compareTo(e1.getValue())).limit(n).collect(Collectors.toList());
    }




    public static void main(String[] args) throws Exception {

        // should add genetic fingerprint data when applicable

        Recommender recommender = new Recommender();
    }
}
