package recommendation;

import javafx.util.Pair;
import lombok.Getter;

import java.util.*;

public class ReviewsModel {
    @Getter
    private Map<String, List<Pair<String,Integer>>> strainToReviewMap = new HashMap<>();
    @Getter
    private Map<String, List<Pair<String,Integer>>> profileToReviewMap = new HashMap<>();
    public ReviewsModel(List<Map<String,Object>> data, String resourceStr, String profileStr, String ratingStr) {
        for(Map<String,Object> row : data) {
            String strain = (String) row.get(resourceStr);
            String profile = (String) row.get(profileStr);
            int rating = (Integer) row.get(ratingStr);
            strainToReviewMap.putIfAbsent(strain, new ArrayList<>());
            strainToReviewMap.get(strain).add(new Pair<>(profile, rating));
            profileToReviewMap.putIfAbsent(profile, new ArrayList<>());
            profileToReviewMap.get(profile).add(new Pair<>(strain, rating));
        }
    }

}
