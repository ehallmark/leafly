package server;

import com.google.gson.Gson;
import database.Database;
import j2html.tags.ContainerTag;
import recommendation.strains.StrainRecommendation;
import recommendation.strains.StrainRecommender;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) throws Exception {
        port(8080);
        staticFiles.externalLocation(new File("public").getAbsolutePath());
        // get strain data
        List<Map<String,Object>> strainData = Database.loadData("strains", "id", "name", "type");
        List<Map<String,Object>> productData = Database.loadData("products", "product_id", "product_name", "type", "subtype", "brand_name");

        Map<String,String> nameMap = Database.loadMap("strains", "id", "name").entrySet()
                .stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue().get(0).toString()));
        Map<String,List<String>> linkMap = Database.loadMap("strain_photos", "strain_id", "photo_url").entrySet()
                .stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue().stream().map(o->o.toString()).limit(5).collect(Collectors.toList())));

        StrainRecommender recommender = new StrainRecommender();

        get("/", (req, res)->{
            req.session(true);
            return htmlWrapper(div().withClass("container").with(
                div().withClass("row").attr("style", "margin-top: 10%;").with(
                        div().withClass("col-12").with(
                                h4("Strain Recommendation System")
                        ),
                        div().withClass("col-12").with(
                                h5("Select Favorite Strains and Products"),
                                form().withClass("strain_recommendation").with(
                                        label("Strains").with(br(),select().withName("favorite_strains[]").withClass("strain_selection").attr("multiple").with(option()).with(
                                                strainData.stream().map(strain->option(strain.get("name").toString()+" - ("+strain.get("type")+")").withValue((String)strain.get("id")))
                                                .collect(Collectors.toList())
                                        )),br(),
                                        label("Products").with(br(),select().withName("favorite_products[]").withClass("product_selection").attr("multiple").with(option()).with(
                                                productData.stream().map(product->option().withText(product.get("product_name").toString()+" by ").with(b(product.get("brand_name").toString())).withText("("+product.get("type")+"/"+product.get("subtype")+")").withValue((String)product.get("product_id")))
                                                        .collect(Collectors.toList())
                                        )),
                                        button("Suggest").withClass("btn btn-sm btn-outline-primary")
                                )
                        ),div().withClass("col-12").with(
                                h5("Strain Recommendations"),
                                div().withId("results")
                        )
                )
            )).render();
        });


        post("/recommend", (req, res) -> {
            String[] favoriteStrains = req.queryParamsValues("favorite_strains[]");
            String html;
            if(favoriteStrains==null || favoriteStrains.length==0) {
                html = "Please select at least one favorite strain.";
            } else {
                System.out.println("Recommend strains for: " + String.join(", ", favoriteStrains));

                Map<String, Double> ratings = new HashMap<>();
                for (String favoriteStrain : favoriteStrains) {
                    ratings.put(favoriteStrain, 5d);
                }

                final Map<String,Object> recData = new HashMap<>();
                recData.put("alpha", 0.2);
                recData.put("previousStrainRatings", ratings);
                List<StrainRecommendation> topRecommendations = recommender.topRecommendations(10, recData);

                html = div().withClass("col-12").with(
                        topRecommendations.stream().map(recommendation -> {
                            List<String> links = linkMap.getOrDefault(recommendation.getStrain(), Collections.emptyList());
                            return div().with(b(nameMap.get(recommendation.getStrain()))).with(br())
                                    .with(links.stream().map(link->img().withSrc(link)).collect(Collectors.toList()))
                                    .with(
                                    div().with(Stream.of(recommendation.toString().split("\\n")).map(line->{
                                        return div(line);
                                    }).collect(Collectors.toList()))
                            ).with(hr());
                        }).collect(Collectors.toList())
                ).render();

            }
            return new Gson().toJson(Collections.singletonMap("data", html));
        });

    }

    private static ContainerTag htmlWrapper(ContainerTag inner) {
        return html().attr("style", "height: 100%;").with(
                head().with(
                        script().withSrc("/js/jquery-3.3.1.min.js"),
                        script().withSrc("/js/jquery-ui-1.12.1.min.js"),
                        script().withSrc("/js/popper.min.js"),
                        script().withSrc("/js/main.js"),
                        script().withSrc("/js/select2.min.js"),
                        script().withSrc("/js/bootstrap.min.js"),
                        link().withRel("stylesheet").withHref("/css/bootstrap.min.css"),
                        link().withRel("stylesheet").withHref("/css/select2.min.css"),
                        link().withRel("stylesheet").withHref("/css/jquery-ui.min.css")

                ),
                body().with(inner)
        );
    }
}
