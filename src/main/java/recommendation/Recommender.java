package recommendation;

import database.Database;

public class Recommender {




    public static void main(String[] args) throws Exception {
        // get categorical data
        SimilarityEngine effectSim = new SimilarityEngine(Database.loadEffects());
        SimilarityEngine flavorSim = new SimilarityEngine(Database.loadFlavors());
        SimilarityEngine parentSim = new SimilarityEngine(Database.loadParentStrains());
        // get author rating strain triplets

    }
}
