package strain_recommendation;

import javafx.util.Pair;
import lombok.Getter;

import java.util.*;

public class ReviewsModel {
    @Getter
    private Map<String, List<Pair<String,Integer>>> strainToReviewMap = new HashMap<>();
    @Getter
    private Map<String, List<Pair<String,Integer>>> profileToReviewMap = new HashMap<>();
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
    }

}
