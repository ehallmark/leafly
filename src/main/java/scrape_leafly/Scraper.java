package scrape_leafly;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scraper {
    public static String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }

    public static void main(String[] args) throws Exception {
        run(false);
    }

    public static void run(boolean reseed) throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
        conn.setAutoCommit(false);
        long timeSleep = 1000;
        File folder = new File("leafly/");
        Document jsoup;
        WebDriver driver = null;
        String url = "https://www.leafly.com/explore/sort-alpha";
        ChromeOptions options = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        System.setProperty("webdriver.firefox.driver", "/usr/bin/geckodriver");
        driver = new ChromeDriver(options);

        driver.get(url);
        for (int i = 0; i < 10; i++) {
            System.out.println("Starting in " + (10 - i));
            TimeUnit.MILLISECONDS.sleep(1000);
        }

        if(new File(folder, "data.html").exists() && !reseed) {
            jsoup = Jsoup.parse(FileUtils.readFileToString(new File(folder, "data.html"), Charsets.UTF_8));
        } else {
            int cnt = 0;
            WebElement element = driver.findElement(By.cssSelector("button.ga_Explore_LoadMore"));
            String prevText = null;
            while (element != null && (prevText == null || !prevText.equals(driver.getPageSource()))) {
                prevText = driver.getPageSource();
                element.click();
                System.out.println("Page: " + cnt);
                TimeUnit.MILLISECONDS.sleep(timeSleep);
                try {
                    element = driver.findElement(By.cssSelector("button.ga_Explore_LoadMore"));
                } catch (Exception e) {
                    e.printStackTrace();
                    driver.close();
                    System.exit(1);
                }
                cnt++;
            }
            FileUtils.writeStringToFile(new File(folder, "data.html"), driver.getPageSource(), Charsets.UTF_8);
            jsoup = Jsoup.parse(driver.getPageSource());
        }

        Elements strainLinks = jsoup.select("div.explore-tiles a[href]");
        System.out.println("Num strains: "+strainLinks.size());
        int n = 0;
        for(Element strainLink : strainLinks) {
            n++;
            String href = strainLink.attr("href");
            if (href != null && href.length() > 0) {
                String strainId = href.replace("/","_");
                String overViewPage;
                File overviewFile = new File(new File(folder, "overviews"), strainId);
                if(!overviewFile.exists() || reseed) {
                    driver.get("https://www.leafly.com" + href);
                    System.out.println("Strain: " + href);
                    TimeUnit.MILLISECONDS.sleep(timeSleep);
                    FileUtils.writeStringToFile(new File(new File(folder, "overviews"), strainId), driver.getPageSource(), Charsets.UTF_8);
                }
                overViewPage = FileUtils.readFileToString(overviewFile, Charsets.UTF_8);
                handleOverviews(strainId, overViewPage, conn);

                {
                    // photos
                    File photoFile = new File(new File(folder, "photos"), href.replace("/", "_"));
                    WebElement element = null;
                    if(!photoFile.exists() || reseed) {
                        // now get reviews
                        driver.get("https://www.leafly.com" + href + "/photos");
                        String prevPhotos = null;
                        while (prevPhotos == null || !prevPhotos.equals(driver.getPageSource())) {
                            TimeUnit.MILLISECONDS.sleep(timeSleep);
                            prevPhotos = driver.getPageSource();
                            try {
                                JavascriptExecutor js = (JavascriptExecutor) driver;
                                js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                                TimeUnit.MILLISECONDS.sleep(timeSleep);
                                element = driver.findElement(By.cssSelector("button.photos__load-more"));
                                element.click();
                                TimeUnit.MILLISECONDS.sleep(timeSleep);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Photos of strain: " + n);
                        }
                        FileUtils.writeStringToFile(photoFile, driver.getPageSource(), Charsets.UTF_8);
                    }
                    String photoPage = FileUtils.readFileToString(photoFile, Charsets.UTF_8);
                    handlePhotos(strainId, photoPage, conn);
                }
                {
                    // reviews
                    int cnt = 0;
                    File reviewFile = new File(new File(folder, "reviews"), href.replace("/", "_") + "_" + cnt);
                    WebElement element = null;
                    if(!reviewFile.exists() || reseed) {
                        // now get reviews
                        driver.get("https://www.leafly.com" + href + "/reviews?sort=date");
                        try {
                            element = driver.findElement(By.cssSelector("a.strain-reviews__load-more"));
                        } catch (Exception e) {
                            element = null;
                        }

                        FileUtils.writeStringToFile(reviewFile, driver.getPageSource(), Charsets.UTF_8);
                    }
                    String prevReview = null;
                    String reviewPage = FileUtils.readFileToString(reviewFile, Charsets.UTF_8);
                    int offset = handleReviews(strainId, reviewPage, conn, 0);
                    boolean seekNext = false;
                    while (seekNext || prevReview == null || !prevReview.equals(driver.getPageSource())) {
                        boolean wasSeeking = seekNext;
                        seekNext = false;
                        cnt++;
                        prevReview = driver.getPageSource();
                        if (element != null) {
                            try {
                                element.click();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                       // System.out.println("Strain " + n + " -> Page: " + cnt);
                        reviewFile = new File(new File(folder, "reviews"), href.replace("/", "_") + "_" + cnt);
                        if (!reviewFile.exists() || reseed) {
                            if (!wasSeeking) {
                                TimeUnit.MILLISECONDS.sleep(timeSleep);
                                FileUtils.writeStringToFile(reviewFile, driver.getPageSource(), Charsets.UTF_8);
                                try {
                                    element = driver.findElement(By.cssSelector("a.strain-reviews__load-more"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            seekNext = true;
                        }
                        if (reviewFile.exists()) {
                            reviewPage = FileUtils.readFileToString(reviewFile, Charsets.UTF_8);
                            offset += handleReviews(strainId, reviewPage, conn, offset);
                        }
                    }
                }
            }
            conn.commit();
        }
        conn.commit();
        conn.close();
        driver.close();
    }

    private static void handleOverviews(String strainId, String reviewPage, Connection conn) throws Exception {
        Document document = Jsoup.parse(reviewPage);
        // name, type, rating, effects, flavors, description
        String type;
        if(strainId.startsWith("_indica")) {
            type = "Indica";
        } else if(strainId.startsWith("_sativa")) {
            type = "Sativa";
        } else if (strainId.startsWith("_hybrid")) {
            type = "Hybrid";
        } else {
            return;
        }
        PreparedStatement ps = conn.prepareStatement("insert into strains (id,name,type,description,rating) values (?,?,?,?,?) on conflict do nothing");
        String name = null;
        String description = null;
        Double rating = null;
        List<Object[]> effects = new ArrayList<>();
        List<String> flavors = new ArrayList<>();
        List<String> lineage = new ArrayList<>();
        Elements descriptionElem = document.select("div.description-wrapper div.description");
        if(descriptionElem.size()>0) {
            name = descriptionElem.get(0).parent().previousElementSibling().text().replace("What is", "").replace("?", "").trim();
            description = descriptionElem.text();
        }
        Elements ratingElem = document.select(".rating-number");
        if(ratingElem.size()>0) {
            rating = Double.valueOf(ratingElem.get(0).text());
        }
        Elements lineageElem = document.select("div.strain__lineage ul a[href]");
        for(Element line : lineageElem) {
            lineage.add(line.attr("href").replace("/", "_").trim());
        }
        Elements flavorsSection = document.select("section.strain__flavors li");
        for(Element flavor : flavorsSection) {
            flavors.add(flavor.text().replaceAll("[0-9.]", "").trim());
        }
        // check for test data image
        Elements testDataSection = document.select(".strain__testGraph img[src]");
        if(testDataSection.size()>0) {
            String src = testDataSection.get(0).attr("src");
            String[] split = src.split("/");
            String imageName = split[split.length-1];
            File imageFile = new File("leafly/fingerprints/" + imageName);
            if (!imageFile.exists()) {
                System.out.println("Downloading image: " + imageName);
                try {
                    FileUtils.copyURLToFile(new URL(src), imageFile);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            imageFile = new File("leafly/fingerprints/" + imageName);
            if(imageFile.exists()) {
                // process image
            }
        }
        Elements effectsSections = document.select("div.m-strain-attributes div.m-histogram");
        for(Element effectsSection: effectsSections) {
            String effectType = effectsSection.attr("ng-show").split("===")[1].replace("'","");
            for (Element effect : effectsSection.select("div.m-histogram-item-wrapper")) {
                String effectName = effect.text().trim();
                double effectScore = Double.valueOf(effect.select(".m-attr-bar").attr("style").replace("width:","").replace("%","").trim())/100.0;
                effects.add(new Object[]{effectName, effectType, effectScore});
            }
        }
        if(name==null) {
            return;
        }
        ps.setString(1, strainId);
        ps.setString(2, name);
        ps.setString(3, type);
        ps.setString(4, description);
        ps.setObject(5, rating);
        ps.executeUpdate();
        // flavors
        ps.close();
        if(flavors.size()>0) {
            ps = conn.prepareStatement("insert into strain_flavors (strain_id, flavor) values (?,?) on conflict do nothing");
            for (String flavor : flavors) {
                ps.setString(1, strainId);
                ps.setString(2, flavor);
                ps.executeUpdate();
            }
            ps.close();
        }
        if(effects.size()>0) {
            ps = conn.prepareStatement("insert into strain_effects (strain_id, effect, effect_type, effect_percent) values (?,?,?,?) on conflict do nothing");
            for (Object[] effect : effects) {
                ps.setString(1, strainId);
                ps.setString(2, (String)effect[0]);
                ps.setString(3, (String)effect[1]);
                ps.setDouble(4, (Double)effect[2]);
                ps.executeUpdate();
            }
            ps.close();
        }
        if(lineage.size()>0) {
            ps = conn.prepareStatement("insert into strain_lineage (strain_id, parent_strain_id) values (?,?) on conflict do nothing");
            for (String parentStrainId : lineage) {
                ps.setString(1, strainId);
                ps.setString(2, parentStrainId);
                ps.executeUpdate();
            }
            ps.close();
        }
        System.out.println("Name: "+name);
        //System.out.println("Type: "+type);
        //System.out.println("Desc: "+description);
        //System.out.println("Rating: "+rating);
        //System.out.println("Flavors: "+String.join("; ", flavors));
        //System.out.println("Effects: "+String.join("; ", effects));
        //System.out.println("Lineage: "+String.join("; ", lineage));
    }

    private static int handleReviews(String strainId, String reviewPage, Connection conn, int offset) throws Exception{
        if(!(strainId.startsWith("_indica") || strainId.startsWith("_sativa")||strainId.startsWith("_hybrid"))) {
            return 0;
        }
        PreparedStatement ps = conn.prepareStatement("insert into strain_reviews (strain_id,review_num,review_text,review_rating,review_profile) values (?,?,?,?,?) on conflict do nothing");
        // author, rating, review text
        Document document = Jsoup.parse(reviewPage);
        Elements reviews = document.select(".strain-reviews__review-container li.page-item div.m-review");
        int i = offset;
        for(Element review : reviews) {
       //     System.out.println("Found review for "+strainId+": "+review.html());
            String profile = review.select("a[href]").attr("href").replace("/profile/","").trim();
            int rating = Integer.valueOf(review.select("span[star-rating]").attr("star-rating"));
            String text = review.select("div.l-grid p").text().trim();
            if(text.length()>2) {
                text = text.substring(1, text.length() - 1);
            }
           // System.out.println("Name: "+strainId);
           // System.out.println("Profile: "+profile);
           // System.out.println("Rating: "+rating);
           // System.out.println("Text: "+text);
            ps.setString(1, strainId);
            ps.setInt(2, i);
            ps.setString(3, text);
            ps.setObject(4, rating);
            ps.setString(5, profile);
            ps.executeUpdate();
            i++;
        }
        ps.close();
        return reviews.size();
    }

    private static void handlePhotos(String strainId, String photoPage, Connection conn) throws Exception{
        if(!(strainId.startsWith("_indica") || strainId.startsWith("_sativa")||strainId.startsWith("_hybrid"))) {
            return;
        }
        PreparedStatement ps = conn.prepareStatement("insert into strain_photos (strain_id,photo_id,photo_url) values (?,?,?) on conflict do nothing");

        // download photo if isn't already saved
        Document document = Jsoup.parse(photoPage);
        Elements images = document.select("div.photos__photo-container img[src]");
        for(Element image : images) {
            String src = image.attr("src");
            String imageName = src.split("/reviews/")[1];
            File imageFile = new File("leafly/images/" + imageName);
            if (!imageFile.exists()) {
                System.out.println("Downloading image: " + imageName);
                try {
                    FileUtils.copyURLToFile(new URL(src), imageFile);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            imageFile = new File("leafly/images/" + imageName);
            if(imageFile.exists()) {
                ps.setString(1, strainId);
                ps.setString(2, imageName);
                ps.setString(3, src);
                ps.executeUpdate();
            }
        }
        ps.close();
    }

}

