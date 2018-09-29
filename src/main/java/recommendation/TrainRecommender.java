package recommendation;

import database.Database;

import java.util.*;

public class TrainRecommender {
    public static void main(String[] args) throws Exception {
        final int numTests = 5000;
        final List<Map<String,Object>> allReviewData = new ArrayList<>(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
        Collections.shuffle(allReviewData, new Random(2352));

        final List<Map<String,Object>> trainData = allReviewData.subList(0, allReviewData.size()-numTests);
        final List<Map<String,Object>> testData = allReviewData.subList(allReviewData.size()-numTests, allReviewData.size());

        System.out.println("Num train: "+trainData.size());
        System.out.println("Num test: "+testData.size());

        Recommender trainRecommender = new Recommender(trainData);
        Recommender testRecommender = new Recommender(testData);


    }
}
