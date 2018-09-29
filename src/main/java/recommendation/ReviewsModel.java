package recommendation;

import javafx.util.Pair;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class ReviewsModel {
    @Getter
    private Map<String, List<Pair<String,Integer>>> strainToReviewMap = new HashMap<>();
    @Getter
    private Map<String, List<Pair<String,Integer>>> profileToReviewMap = new HashMap<>();
    private SimilarityEngine strainSim;
    @Getter
    private Map<String,Integer> reviewSizeMap;
    public ReviewsModel(List<Map<String,Object>> data) {
        for(Map<String,Object> row : data) {
            String strain = (String) row.get("strain_id");
            String profile = (String) row.get("review_profile");
            int rating = (Integer) row.get("review_rating");
            strainToReviewMap.putIfAbsent(strain, new ArrayList<>());
            strainToReviewMap.get(strain).add(new Pair<>(profile, rating));
            profileToReviewMap.putIfAbsent(profile, new ArrayList<>());
            profileToReviewMap.get(profile).add(new Pair<>(strain, rating));
        }

        strainSim = new SimilarityEngine(strainToReviewMap.keySet());
    }


    public Map<String,Double> similarity(Map<String,Double> knownStrainsNormalizedWeighted) {
        reviewSizeMap = new HashMap<>();
        Map<String,Double> similarityMap = new HashMap<>();

        // get reviewers of known strains
        List<Pair<String,Integer>> reviews = knownStrainsNormalizedWeighted.keySet().stream()
                .flatMap(strain->strainToReviewMap.getOrDefault(strain, Collections.emptyList()).stream())
                .collect(Collectors.toList());

        Set<String> authors = reviews.stream().map(p->p.getKey()).collect(Collectors.toSet());

        // find similar profiles
        Map<String,Double> profileSimilarityMap = authors.stream()
                .map(author->{
                    // similarity of author to knownStrains
                    Map<String,Double> authorStrains = profileToReviewMap.getOrDefault(author, Collections.emptyList())
                            .stream().collect(Collectors.groupingBy(p->p.getKey(), Collectors.summingDouble(p->p.getValue().doubleValue()-2.5)));
                    return new Pair<>(author,strainSim.similarity(knownStrainsNormalizedWeighted, authorStrains));
                }).filter(p->p.getValue()>0)
                .collect(Collectors.toMap(p->p.getKey(),p->p.getValue()));

        // look at other strains reviewed by reviewers
        Map<String,List<Pair<String,Integer>>> relevantReviews = profileSimilarityMap.keySet().stream()
                .collect(Collectors.toMap(author->author, author->profileToReviewMap.getOrDefault(author, Collections.emptyList())));

        Set<String> otherStrains = relevantReviews.values().stream()
                .flatMap(p->p.stream().map(o->o.getKey())).collect(Collectors.toCollection(HashSet::new));

        otherStrains.removeAll(knownStrainsNormalizedWeighted.keySet());

        Map<String,Map<String,List<Integer>>> relevantReviewMap = relevantReviews.entrySet().stream()
                .collect(Collectors.toMap(e->e.getKey(), e->{
                    Map<String,List<Integer>> tmp = new HashMap<>();
                    for(Pair<String,Integer> pair : e.getValue()) {
                        tmp.putIfAbsent(pair.getKey(), new ArrayList<>());
                        tmp.get(pair.getKey()).add(pair.getValue());
                    }
                    return  tmp;
                }));
        // rank scores
        for(String strain : otherStrains) {
            double sim = profileSimilarityMap.entrySet().stream().mapToDouble(e->{
                double authorSim = e.getValue();
                String author = e.getKey();
                List<Integer> reviewScores = relevantReviewMap.getOrDefault(author, Collections.emptyMap())
                        .getOrDefault(strain, Collections.emptyList());
                int s = reviewScores.stream()
                        .mapToInt(p->p)
                        .sum();
                return authorSim * s;
            }).average().orElse(0d);
            similarityMap.put(strain, sim);
            reviewSizeMap.put(strain, profileSimilarityMap.size());
        }
        return similarityMap;
    }

}
