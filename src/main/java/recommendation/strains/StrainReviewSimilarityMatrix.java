package recommendation.strains;

import database.Database;
import javafx.util.Pair;
import recommendation.ReviewSimilarityMatrix;
import recommendation.ReviewsModel;

import java.util.*;
import java.util.stream.Collectors;

public class StrainReviewSimilarityMatrix extends ReviewSimilarityMatrix {
    public StrainReviewSimilarityMatrix(List<String> strains, List<Map<String,Object>> reviewData) {
        super(strains, reviewData, d->new ReviewsModel(d, "strain_id", "review_profile", "review_rating").getProfileToReviewMap());
    }
}
