package recommendation.strains;

import database.Database;
import javafx.util.Pair;
import recommendation.ReviewsModel;

import java.util.*;
import java.util.stream.Collectors;

public class StrainReviewSimilarityMatrix {
    private double[][] matrix;
    private Map<String,Integer> idxMap;
    public StrainReviewSimilarityMatrix(List<String> strains, List<Map<String,Object>> reviewData) {
        this.matrix=new double[strains.size()][strains.size()];
        this.idxMap = new HashMap<>();
        for(int i = 0; i < strains.size(); i++) {
            idxMap.put(strains.get(i), i);
        }
        int[][] counts = new int[strains.size()][strains.size()];
        Map<String, List<Pair<String,Integer>>> profileData = new ReviewsModel(reviewData).getProfileToReviewMap();
        for(List<Pair<String,Integer>> ratedStrains : profileData.values()) {
            for(int i = 0; i < ratedStrains.size(); i++) {
                Pair<String,Integer> strain1 = ratedStrains.get(i);
                Integer strain1Idx = idxMap.get(strain1.getKey());
                int rating1 = strain1.getValue();
                if(rating1 < 0 || rating1 > 5) throw new RuntimeException("Illegal rating: "+rating1);
                for(int j = 0; j < ratedStrains.size(); j++) {
                    Pair<String,Integer> strain2 = ratedStrains.get(j);
                    Integer strain2Idx = idxMap.get(strain2.getKey());
                    double rating2 = strain2.getValue().doubleValue();
                    if(strain1Idx!=null && strain2Idx!=null) {
                        if(rating2 >= 4 && rating1 >= 4) {
                            matrix[strain1Idx][strain2Idx] += (2.0 / Math.log(Math.E+ratedStrains.stream()
                            .filter(r->r.getValue()>=5).count()));
                        } else if(Math.abs(rating1-rating2)>2) {
                            matrix[strain1Idx][strain2Idx] -= Math.abs(rating1-rating2) / Math.log(Math.E+ratedStrains.stream()
                                    .filter(r->r.getValue() < 3).count());
                        }
                        counts[strain1Idx][strain2Idx]++;
                    }
                }
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            double[] row = matrix[i];
            int[] cRow = counts[i];
            for (int j = 0; j < row.length; j++) {
                int c = cRow[j];
                if (c > 0) {
                    row[j] /= c;
                }
            }
        }
    }

    public double similarity(String strain1, String strain2) {
        Integer idx1 = idxMap.get(strain1);
        Integer idx2 = idxMap.get(strain2);
        if(idx1!=null && idx2!=null) {
            return matrix[idx1][idx2];
        }
        return 0d;
    }

    public Map<String,Double> similarity(List<String> strains) {
        int[] indices = strains.stream().filter(idxMap::containsKey)
        .mapToInt(strain->(Integer)idxMap.get(strain)).toArray();
        Map<String,Double> map = new HashMap<>();
        double[] totals = new double[matrix.length];
        for(int idx : indices) {
            double[] scores = matrix[idx];
            for(int i = 0; i < scores.length; i++) {
                totals[i]+=scores[i];
            }
        }
        idxMap.forEach((strain, i)->{
            map.put(strain, totals[i]);
        });
        return map;
    }
}
