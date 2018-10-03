package strain_recommendation;

import java.util.*;
import java.util.stream.Collectors;

public class LineageGraph {
    private Map<String,Set<String>> parentLinkMap = new HashMap<>();
    public LineageGraph(List<Map<String,Object>> data) {
        for(Map<String,Object> row : data) {
            String link = (String)row.get("strain_id");
            String parentLink = (String)row.get("parent_strain_id");
            parentLinkMap.putIfAbsent(link, new HashSet<>());
            parentLinkMap.get(link).add(parentLink);
        }
    }

    public Collection<String> getAncestorsOf(String strainId) {
        Set<String> ancestors = new HashSet<>();
        List<String> tmp = Collections.singletonList(strainId);
        while(tmp.size()>0) {
            ancestors.addAll(tmp);
            tmp = tmp.stream().flatMap(t->parentLinkMap.getOrDefault(t,Collections.emptySet()).stream())
                    .filter(t->!ancestors.contains(t))
                    .collect(Collectors.toList());
        }
        return ancestors;
    }
}
