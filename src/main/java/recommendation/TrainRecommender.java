package recommendation;

import database.Database;
import javafx.util.Pair;
import smile.classification.LogisticRegression;

import java.util.*;
import java.util.stream.Collectors;

public class TrainRecommender {
    public static void main(String[] args) throws Exception {
        final int numTests = 5000;
        final Random rand = new Random(23521);
        final List<Map<String,Object>> allReviewData = new ArrayList<>(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
        Map<String, List<Pair<String,Integer>>> profileData = new ReviewsModel(allReviewData).getProfileToReviewMap();
        List<String> allProfiles = new ArrayList<>(profileData.keySet());
        Collections.shuffle(allProfiles, new Random(2352));

        final List<String> trainProfiles = allProfiles.subList(0, allProfiles.size()-numTests);
        final List<String> testProfiles = allProfiles.subList(allProfiles.size()-numTests, allProfiles.size());

        System.out.println("Train Profiles: "+trainProfiles.size());
        System.out.println("Train Profiles: "+testProfiles.size());

        List<Map<String,Object>> trainData = trainProfiles.stream().flatMap(profile->profileData.get(profile).stream().map(pair->{
                    Map<String,Object> map = new HashMap<>();
                    map.put("strain_id", pair.getKey());
                    map.put("review_rating", pair.getValue());
                    map.put("review_profile", profile);
                    return map;
                })).collect(Collectors.toList());

        // create train dataset
        Map<String,List<Pair<String,Integer>>> trainReviewData = new ReviewsModel(trainData).getProfileToReviewMap()
                .entrySet().stream().filter(e->e.getValue().size()>1)
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

        Recommender trainRecommender = new Recommender(trainData);

        int[] y = new int[trainReviewData.size()];
        double[][] x = new double[trainReviewData.size()][];
        List<String> keys = new ArrayList<>(trainReviewData.keySet());
        for(int i = 0; i < keys.size(); i++) {
            String profile = keys.get(i);
            List<Pair<String,Integer>> data = trainReviewData.get(profile);
            Pair<String,Integer> best = data.remove(rand.nextInt(data.size()));
            Map<String, Double> ratings = data.stream().collect(Collectors.groupingBy(e -> e.getKey(), Collectors.averagingDouble(e -> e.getValue())));
            Recommendation recommendation = trainRecommender.recommendationScoreFor(best.getKey(), ratings);
            y[i] = best.getValue().doubleValue()>=4 ? 1 : 0;
            x[i] = new double[]{
                    recommendation.getEffectSimilarity(),
                    recommendation.getFlavorSimilarity(),
                    recommendation.getLineageSimilarity(),
                    recommendation.getReviewSimilarity(),
                    recommendation.getTypeSimilarity()
            };
        }

        LogisticRegression logit = new LogisticRegression(x, y);
        System.out.println("Log likelihood: "+logit.loglikelihood());



        List<Map<String,Object>> testData = testProfiles.stream().flatMap(profile->profileData.get(profile).stream().map(pair->{
            Map<String,Object> map = new HashMap<>();
            map.put("strain_id", pair.getKey());
            map.put("review_rating", pair.getValue());
            map.put("review_profile", profile);
            return map;
        })).collect(Collectors.toList());

        Map<String,List<Pair<String,Integer>>> testReviewData = new ReviewsModel(testData).getProfileToReviewMap();

        System.out.println("Num train: "+trainData.size());
        System.out.println("Num test: "+testData.size());

        test(testProfiles, testReviewData, trainRecommender, logit);
    }


    private static double test(List<String> testProfiles, Map<String,List<Pair<String,Integer>>> testReviewData, Recommender trainRecommender, LogisticRegression logit) {
        int count = 0;
        double score = 0d;
        Random rand = new Random(23521);
        for(String testProfile : testProfiles) {
            List<Pair<String,Integer>> data = testReviewData.get(testProfile);
            if(data!=null && data.size()>1) {
                // find best rating
                Pair<String,Integer> best = data.remove(rand.nextInt(data.size()));
                Map<String, Double> ratings = data.stream().collect(Collectors.groupingBy(e -> e.getKey(), Collectors.averagingDouble(e -> e.getValue())));
                Recommendation recommendation = trainRecommender.recommendationScoreFor(best.getKey(), ratings);
                double prediction = logit.predict(new double[]{
                        recommendation.getEffectSimilarity(),
                        recommendation.getFlavorSimilarity(),
                        recommendation.getLineageSimilarity(),
                        recommendation.getReviewSimilarity(),
                        recommendation.getTypeSimilarity()
                });
                double target = best.getValue() >= 4 ? 1.0 : 0.0;
                score += Math.abs(target-prediction);
                count++;
            }
        }
        System.out.println("Average Error: "+score/count + " (Count: "+count+")");
        return score/count;
    }
}



