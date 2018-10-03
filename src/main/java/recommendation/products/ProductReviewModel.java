package recommendation.products;

import database.Database;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductReviewModel {
    private List<String> products;
    private List<Map<String,Object>> reviewData;
    public ProductReviewModel() throws SQLException {
        this.products = Database.loadProducts();
        this.reviewData = Database.loadData("product_reviews",
                "product_id", "author", "rating", "upvotes", "downvotes");
    }

    public Map<String,Double> similarity(Map<String,Double> knownProductRatings) {
        Map<String,Double> map = new HashMap<>();

        return map;
    }
}
