package recommendation;

import database.Database;
import javafx.util.Pair;
import smile.classification.*;
import smile.stat.distribution.BetaDistribution;
import smile.stat.distribution.Distribution;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TrainRecommender {
    private static final String LOGIT_FILE = "recommendation.logit";

    public static void main(String[] args) throws Exception {
        final int numTests = 2000;
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
            Recommendation recommendation = trainRecommender.recommendationScoreFor(best.getKey(), ratings, null);
            y[i] = best.getValue().doubleValue()>=4 ? 1 : 0;
            x[i] = new double[]{
                    recommendation.getEffectSimilarity(),
                    recommendation.getFlavorSimilarity(),
                    recommendation.getLineageSimilarity(),
                    recommendation.getReviewSimilarity(),
                    recommendation.getTypeSimilarity(),
                    recommendation.getNumReviews()
            };
        }

        SoftClassifier<double[]> logit = new RandomForest(x, y, 300);
       // SoftClassifier<double[]> logit = new LogisticRegression(x, y);

        //System.out.println("Log likelihood: "+logit.loglikelihood());
        try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(LOGIT_FILE))))) {
            oos.writeObject(logit);
            oos.flush();
            System.out.println("Saved...");
        } catch(Exception e) {
            e.printStackTrace();
        }



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

    public static SoftClassifier<double[]> loadClassificationModel() {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(LOGIT_FILE))))) {
            return (SoftClassifier<double[]>) ois.readObject();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static double test(List<String> testProfiles, Map<String,List<Pair<String,Integer>>> testReviewData, Recommender trainRecommender, SoftClassifier<double[]> logit) {
        int count = 0;
        double score = 0d;
        Random rand = new Random(23521);
        for(String testProfile : testProfiles) {
            List<Pair<String,Integer>> data = testReviewData.get(testProfile);
            if(data!=null && data.size()>1) {
                // find best rating
                Pair<String,Integer> best = data.remove(rand.nextInt(data.size()));
                Map<String, Double> ratings = data.stream().collect(Collectors.groupingBy(e -> e.getKey(), Collectors.averagingDouble(e -> e.getValue())));
                Recommendation recommendation = trainRecommender.recommendationScoreFor(best.getKey(), ratings, logit);
                double target = best.getValue()>=4 ? 1.0 : 0.0;
                score+=Math.abs(target - recommendation.getOverallSimilarity());
                count++;
            }
        }
        System.out.println("Average Error: "+score/count + " (Count: "+count+")");
        return score/count;
    }
}



