package server;

import com.google.gson.Gson;
import database.Database;
import j2html.tags.ContainerTag;
import recommendation.products.ProductRecommendation;
import recommendation.products.ProductRecommender;
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
    private static String truncateString(String str, int n) {
        if(str==null||str.length()<=n) {
            return str;
        }
        return str.substring(0, n);
    }

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
        Map<String,List<String>> productNameMap = Database.loadMap("products", "product_id", "product_name").entrySet()
                .stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue().stream().map(o->o.toString()).limit(1).collect(Collectors.toList())));

        StrainRecommender strainRecommender = new StrainRecommender();
        ProductRecommender productRecommender = new ProductRecommender();

        get("/products_ajax", (req,res)->{
            Map<String,Object> response = new HashMap<>();
            String _query = req.queryParams("q");
            if(_query!=null&&_query.trim().length()==0) {
                _query = null;
            }
            Integer page = null;
            try {
                page = Integer.valueOf(req.queryParams("page"));
            } catch(Exception e) {
                System.out.println("No page param found...");
            }
            final String query = _query;
            List<Map<String,Object>> results = productData.stream().map(product->{
                Map<String, Object> result = new HashMap<>();
                result.put("text", truncateString(product.get("product_name").toString(), 50)+" by "+product.get("brand_name")+"("+product.get("type")+"/"+product.get("subtype")+")");
                result.put("id", (String)product.get("product_id"));
                return result;
            }).filter(m->query==null||m.get("text").toString().toLowerCase().contains(query)).collect(Collectors.toList());

            if(page!=null) {
                int start = page*20;
                if(results.size()>start) {
                    results = results.subList(start, results.size());
                }
            }
            Map<String,Object> pagination = new HashMap<>();
            if(results.size()>20) {
                pagination.put("more", true);
                results = results.subList(0, 20);
            }
            response.put("results", results);
            response.put("pagination", pagination);
            return new Gson().toJson(response);
        });

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
                                        label("Strains").with(br(),select().attr("style", "width: 300px;").withName("favorite_strains[]").withClass("strain_selection").attr("multiple").with(option()).with(
                                                strainData.stream().map(strain->option(strain.get("name").toString()+" - ("+strain.get("type")+")").withValue((String)strain.get("id")))
                                                .collect(Collectors.toList())
                                        )),br(),
                                        label("Products").with(br(),select().attr("style", "width: 300px;").withName("favorite_products[]").withClass("product_selection").attr("multiple").with(option())),br(), br(),button("Suggest").withClass("btn btn-sm btn-outline-primary")
                                )
                        ),div().withClass("col-12").with(
                                h5("Recommendations"),
                                div().withId("results")
                        )
                )
            )).render();
        });


        post("/recommend", (req, res) -> {
            String[] favoriteStrains = req.queryParamsValues("favorite_strains[]");
            String[] favoriteProducts = req.queryParamsValues("favorite_products[]");
            String html;
            if(favoriteStrains==null || favoriteStrains.length==0) {
                html = "Please select at least one favorite strain.";
            } else  if(favoriteProducts==null || favoriteProducts.length==0) {
                html = "Please select at least one favorite product.";
            } else {
                System.out.println("Recommend strains for: " + String.join(", ", favoriteStrains));
                Map<String, Double> strainRatings = new HashMap<>();
                Map<String, Double> productRatings = new HashMap<>();
                for (String favoriteStrain : favoriteStrains) {
                    strainRatings.put(favoriteStrain, 5d);
                }
                for(String favoriteProduct : favoriteProducts) {
                    productRatings.put(favoriteProduct, 5d);
                }

                final Map<String,Object> recData = new HashMap<>();
                recData.put("alpha", 0.2);
                recData.put("previousStrainRatings", strainRatings);
                recData.put("previousProductRatings", productRatings);
                List<ProductRecommendation> topProductRecommendations = productRecommender.topRecommendations(10, recData);
                List<StrainRecommendation> topStrainRecommendations = strainRecommender.topRecommendations(15, recData);

                html = div().withClass("col-12").with(
                        h6("Products")
                ).with(
                        topProductRecommendations.stream().map(recommendation -> {
                           // List<String> links = productLinkMap.getOrDefault(recommendation.getProductId(), Collections.emptyList());
                            return div().with(b(String.join("",productNameMap.getOrDefault(recommendation.getProductId(), Collections.emptyList())))).with(br())
                                   // .with(links.stream().map(link->img().withSrc(link)).collect(Collectors.toList()))
                                    .with(
                                            div().with(Stream.of(recommendation.toString().split("\\n")).map(line->{
                                                return div(line);
                                            }).collect(Collectors.toList()))
                                    ).with(hr());
                        }).collect(Collectors.toList())
                ).with(
                        h6("Strains")
                ).with(
                        topStrainRecommendations.stream().map(recommendation -> {
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
