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
                handleOverviews(strainId, overViewPage);

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
                    handlePhotos(strainId, photoPage);
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
                    handleReviews(strainId, reviewPage);
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
                        System.out.println("Strain " + n + " -> Page: " + cnt);
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
                            handleReviews(strainId, reviewPage);
                        }
                    }
                }
            }
        }
        driver.close();
    }

    private static void handleOverviews(String strainId, String reviewPage) throws Exception {
        Document document = Jsoup.parse(reviewPage);
        // name, type, rating, effects, flavors, description
        Elements reviews = document.select(".strain-reviews__review-container li.page-item div.m-review");
        for(Element review : reviews) {
       //     System.out.println("Found review for "+strainId+": "+review.html());
        }
    }

    private static void handleReviews(String strainId, String reviewPage) throws Exception{
        // num reviews, review text
        Document document = Jsoup.parse(reviewPage);
        Elements reviews = document.select(".strain-reviews__review-container li.page-item div.m-review");
        for(Element review : reviews) {
       //     System.out.println("Found review for "+strainId+": "+review.html());
        }
    }

    private static void handlePhotos(String strainId, String reviewPage) throws Exception{
        // num reviews, review text
    }

}

