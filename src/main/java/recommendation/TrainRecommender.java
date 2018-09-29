package recommendation;

import database.Database;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class TrainRecommender {
    public static void main(String[] args) throws Exception {
        final int numTests = 5000;
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

        Recommender trainRecommender = new Recommender(trainData);

        test(testProfiles, testReviewData, trainRecommender);
    }


    private static double test(List<String> testProfiles, Map<String,List<Pair<String,Integer>>> testReviewData, Recommender trainRecommender) {
        int count = 0;
        double score = 0d;
        for(String testProfile : testProfiles) {
            List<Pair<String,Integer>> data = testReviewData.get(testProfile);
            if(data!=null && data.size()>1) {
                // find best rating
                data = new ArrayList<>(data);
                data.sort(Comparator.comparingInt(d->d.getValue()));
                Pair<String,Integer> best = data.remove(data.size()-1);
                if(best.getValue()>=4) {
                    Map<String, Double> ratings = data.stream().collect(Collectors.groupingBy(e -> e.getKey(), Collectors.averagingDouble(e -> e.getValue())));
                    List<Recommendation> recommendations = trainRecommender.topRecommendations(10, ratings);
                    boolean found = false;
                    for (Recommendation recommendation : recommendations) {
                        if (recommendation.getStrain().equals(best.getKey())) {
                            found = true;
                        }
                    }
                    if (found) {
                        score++;
                    }
                    count++;
                }
            }
        }
        System.out.println("Score: "+score/count + " (Count: "+count+")");
        return score/count;
    }
}



