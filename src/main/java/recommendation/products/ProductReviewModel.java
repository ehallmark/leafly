package recommendation.products;

import database.Database;
import recommendation.ReviewSimilarityMatrix;
import recommendation.ReviewsModel;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductReviewModel extends ReviewSimilarityMatrix {
    public ProductReviewModel(List<String> products, List<Map<String,Object>> reviewData) throws SQLException {
        super(products, reviewData, d->new ReviewsModel(d, "product_id", "author", "rating").getProfileToReviewMap());
    }
}
