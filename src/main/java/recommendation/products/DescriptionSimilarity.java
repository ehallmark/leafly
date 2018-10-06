package recommendation.products;

import database.Database;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DescriptionSimilarity {
    private static Function<String,String[]> tokenizer = desc->desc.toLowerCase().replaceAll("[^a-z0-9 ]"," ").split("\\s");
    private List<String[]> descriptions;
    private Map<String,Integer> wordIdxMap;
    private Map<String,Integer> wordDocumentCountMap;
    public DescriptionSimilarity() throws SQLException {
        /*this.descriptions = Database.loadDescriptions().stream()
                .filter(desc->desc!=null&&desc.trim().length()>0)
                .map(tokenizer)
                .collect(Collectors.toList());
        // vocab
        this.wordDocumentCountMap = new HashMap<>();
        for(int i = 0; i < descriptions.size(); i++) {
            Set<String> words = new HashSet<>(Arrays.asList(descriptions.get(i)));
            for(String word: words) {
                if (!STOP_WORDS.contains(word)) {
                    wordDocumentCountMap.putIfAbsent(word, 0);
                    wordDocumentCountMap.put(word, wordDocumentCountMap.get(word) + 1);
                }
            }
        }


        List<String> words = new ArrayList<>(wordDocumentCountMap.keySet());
        words.sort(Comparator.naturalOrder());
        this.wordIdxMap = new HashMap<>();
        for(int i = 0; i < words.size(); i++) {
            wordIdxMap.put(words.get(i), i);
        }
        System.out.println("Num words: "+wordIdxMap.size());*/
    }

    public double similarity(String text1, String text2) {
        String[] words1 = tokenizer.apply(text1);
        String[] words2 = tokenizer.apply(text2);
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        set1.removeIf(STOP_WORDS::contains);
        set2.removeIf(STOP_WORDS::contains);
        if(set1.size() < 5 || set2.size() < 5) {
            return 0d;
        }
        Set<String> set3 = new HashSet<>();
        set3.addAll(set1);
        set3.addAll(set2);
        double wi = set1.size();
        double wj = set2.size();
        double wij = wi + wj - set3.size();
        return wij / Math.max(wi, wj);
    }

    public static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"
    ));

    public static void main(String[] args) throws Exception {
        // test
        new DescriptionSimilarity();
        for(String subtype : Database.loadSubTypes().stream().sorted().collect(Collectors.toList())) {
            System.out.println("BRAND_MULTIPLIER_BY_TYPE.put(\""+subtype+"\", 1.0);");
        }
    }
}
