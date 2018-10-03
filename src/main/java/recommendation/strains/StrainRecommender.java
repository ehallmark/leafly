package recommendation.strains;

import com.google.common.base.Functions;
import database.Database;
import lombok.NonNull;
import recommendation.Recommender;
import recommendation.SimilarityEngine;
import recommendation.StringSimilarity;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StrainRecommender implements Recommender<StrainRecommendation> {
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
    public StrainRecommender() throws SQLException {
        this(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
    }

    private StrainRecommender(List<Map<String,Object>> reviewData) throws SQLException {
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
        stringSimilarity = new StringSimilarity(3);

        System.out.println("Flavor data size: "+flavorData.size());
        System.out.println("Effect data size: "+effectData.size());
    }

    @Override
    public StrainRecommendation recommendationScoreFor(@NonNull String _strain, Map<String,Object> data) {
        Map<String,Double> knownEffects = (Map<String,Double>)data.get("knownEffects");
        Map<String,Double> knownFlavors = (Map<String,Double>)data.get("knownFlavors");
        Map<String,Double> knownLineage = (Map<String,Double>)data.get("knownLineage");
        Map<String,Double> knownTypes = (Map<String,Double>)data.get("knownTypes");
        Map<String,Double> rScores = (Map<String,Double>)data.get("rScores");
        Map<String,Double> previousStrainRatings = (Map<String,Double>)data.get("previousStrainRatings");
        double alpha = (Double)data.get("alpha");

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
        StrainRecommendation recommendation = new StrainRecommendation(_strain);
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

    @Override
    public List<StrainRecommendation> topRecommendations(int n, Map<String,Object> _data) {
        Map<String,Double> _previousStrainRatings = (Map<String,Double>) _data.get("previousStrainRatings");
        double alpha = (Double)_data.get("alpha");
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
        List<String> goodRatings = new ArrayList<>(previousStrainRatings.keySet());
        final Map<String,Double> rScores = reviewsModel.similarity(goodRatings);
        final Map<String,Object> data = new HashMap<>();
        data.put("previousStrainRatings", previousStrainRatings);
        data.put("knownEffects", knownEffects);
        data.put("knownLineage", knownLineage);
        data.put("knownFlavors", knownFlavors);
        data.put("knownTypes", knownTypes);
        data.put("rScores", rScores);
        data.put("alpha", alpha);
        Stream<StrainRecommendation> stream = strains.stream().filter(strain->!previousStrains.contains(strain)).map(_strain->{
            return recommendationScoreFor(_strain, data);
        });
        if(n > 0) {
            return stream.sorted((e1, e2) -> Double.compare(e2.getOverallSimilarity(), e1.getOverallSimilarity())).limit(n).collect(Collectors.toList());
        } else {
            return stream.collect(Collectors.toList());
        }
    }
}
