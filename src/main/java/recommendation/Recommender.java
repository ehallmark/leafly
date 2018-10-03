package recommendation;

import lombok.NonNull;
import java.util.*;

public interface Recommender<T> {
    T recommendationScoreFor(@NonNull String _resource, Map<String,Object> data);
    List<T> topRecommendations(int n, @NonNull Map<String,Double> _previousRatings, double alpha);
}
