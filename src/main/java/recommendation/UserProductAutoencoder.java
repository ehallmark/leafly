package recommendation;

import database.Database;
import javafx.util.Pair;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.variational.BernoulliReconstructionDistribution;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import smile.projection.PCA;

import java.util.*;
import java.util.stream.Collectors;

public class UserProductAutoencoder {

    public static void main(String[] args) throws Exception {
        final int numTests = 10000;
        final Random rand = new Random(23521);
        final List<Map<String,Object>> allReviewData = new ArrayList<>(Database.loadData("strain_reviews", "strain_id", "review_rating", "review_profile"));
        Map<String, List<Pair<String,Integer>>> profileData = new ReviewsModel(allReviewData).getProfileToReviewMap();
        List<String> allProfiles = new ArrayList<>(profileData.keySet());
        Collections.shuffle(allProfiles, new Random(2352));
        final List<String> trainProfiles = allProfiles.subList(0, allProfiles.size()-numTests);
        final List<String> testProfiles = allProfiles.subList(allProfiles.size()-numTests, allProfiles.size());

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

        List<String> profiles = Database.loadProfiles();
        List<String> strains = Database.loadStrains();

        PCA model = getPCAModel(trainData, profiles, strains);

        projection(model, testData, profiles, strains);
    }

    public static double[][] getDataFrom(List<Map<String,Object>> reviewData, List<String> profiles, List<String> strains) {
        Map<String, Integer> profileIdxMap = new HashMap<>();
        Map<String, Integer> strainIdxMap = new HashMap<>();
        for (int i = 0; i < profiles.size(); i++) {
            profileIdxMap.put(profiles.get(i), i);
        }
        for (int i = 0; i < strains.size(); i++) {
            strainIdxMap.put(strains.get(i), i);
        }
        double[][] data = new double[profiles.size()][strains.size()];
        int[][] counts = new int[profiles.size()][strains.size()];
        for (Map<String, Object> review : reviewData) {
            Integer profileIdx = profileIdxMap.get(review.get("review_profile").toString());
            Integer strainIdx = strainIdxMap.get(review.get("strain_id").toString());
            double rating = ((Integer) review.get("review_rating")).doubleValue() - 2.5;
            if (profileIdx != null && strainIdx != null) {
                data[profileIdx][strainIdx] += rating;
                counts[profileIdx][strainIdx]++;
            }
        }
        for (int i = 0; i < data.length; i++) {
            double[] row = data[i];
            for (int j = 0; j < row.length; j++) {
                int c = counts[i][j];
                if (c > 0) {
                    data[i][j] /= c;
                }
            }
        }
        return data;
    }

    public static PCA getPCAModel(List<Map<String,Object>> reviewData, List<String> profiles, List<String> strains) {
        return new PCA(getDataFrom(reviewData, profiles, strains));
    }

    public static double[][] projection(PCA model, List<Map<String,Object>> reviewData, List<String> profiles, List<String> strains) {
        return model.project(getDataFrom(reviewData, profiles, strains));
    }
}
