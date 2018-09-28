package recommendation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SimilarityEngine {
    private Collection<String> labels;
    private Map<String, Integer> indexMap;
    public SimilarityEngine(Collection<String> labels) {
        this.labels = labels;
        AtomicInteger idx = new AtomicInteger(0);
        indexMap = labels.stream().distinct()
                .sorted().collect(Collectors.toMap(e->e,e->idx.getAndIncrement()));
    }

    public double similarity(Collection<String> labels1, Collection<String> labels2) {
        return similarity(toMap(labels1),toMap(labels2));
    }

    public double similarity(Map<String,Double> labels1, Map<String,Double> labels2) {
        double[] v1 = new double[indexMap.size()];
        double[] v2 = new double[indexMap.size()];
        boolean found1 = false;
        boolean found2 = false;
        for(String lab : labels1.keySet()) {
            Integer idx = indexMap.get(lab);
            double val = labels1.get(lab);
            if(idx!=null && val>0) {
                found1 = true;
                v1[idx]=val;
            }
        }
        for(String lab : labels2.keySet()) {
            Integer idx = indexMap.get(lab);
            double val = labels2.get(lab);
            if(idx!=null && val>0) {
                found2 = true;
                v2[idx]=val;
            }
        }
        if(found1 && found2) {
            return cosineSimilarity(v1, v2);
        } else {
            return 0;
        }
    }


    private static double cosineSimilarity(double[] v1, double[] v2) {
        final int n = v1.length;
        double ab = 0;
        double a = 0;
        double b = 0;
        for(int i = 0; i < n; i++) {
            ab += v1[i]*v2[i];
            a += v1[i]*v1[i];
            b += v2[i]*v2[i];
        }
        if(a>0 && b > 0) {
            return ab/(Math.sqrt(a)*Math.sqrt(b));
        } else {
            return 0;
        }
    };

    private static Map<String,Double> toMap(Collection<String> labels) {
        return labels.stream().distinct().collect(Collectors.toMap(l->l,l->1d));
    }

}
