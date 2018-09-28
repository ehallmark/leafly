package recommendation;

import javafx.util.Pair;

import java.util.*;

public class ReviewsModel {
    private Map<String, List<Pair<String,Integer>>> strainToReviewMap = new HashMap<>();
    public ReviewsModel(List<Map<String,Object>> data) {
        for(Map<String,Object> row : data) {
            String strain = (String) row.get("strain_id");
            strainToReviewMap.putIfAbsent(strain, new ArrayList<>());
            strainToReviewMap.get(strain).add(new Pair<>((String)row.get("review_profile"),(Integer)row.get("review_rating")));
        }
    }


    public Map<String,Double> similarity(Map<String,Double> knownStrains) {
        Map<String,Double> similarityMap = new HashMap<>();

        

        return similarityMap;
    }

}
