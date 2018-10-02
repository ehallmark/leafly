package scrape_leafly;

import com.google.common.base.Charsets;
import database.Database;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductScraper {

    public static void run(boolean reseed) throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
        conn.setAutoCommit(false);

        long timeSleep = 100;
        File folder = new File("leafly/");

        WebDriver driver = null;
        String url = "https://www.leafly.com/products";
        ChromeOptions options = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", "C:/Users/inamo/Downloads/chromedriver_win32/chromedriver.exe");
        System.setProperty("webdriver.firefox.driver", "/usr/bin/geckodriver");
        try {
            driver = new ChromeDriver(options);
            driver.get(url);
            for (int i = 0; i < 10; i++) {
                System.out.println("Starting in " + (10 - i));
                TimeUnit.MILLISECONDS.sleep(1000);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Document jsoup = Jsoup.parse(driver.getPageSource());
        List<String> links = jsoup.select("div.left-navigation-item a[href]").stream().skip(1)
                .map(elem->elem.attr("href")).collect(Collectors.toList());

        for(String href : links) {
            if(href.equals("/products")) continue;
            url = "https://www.leafly.com/" + href;
            String id =  href.replace("/", "_");

            File overviewFile = new File(new File(folder, "products"),id);
            String page = null;
            if (!overviewFile.exists() || reseed) {
                try {
                    driver.get(url);
                    TimeUnit.MILLISECONDS.sleep(timeSleep);
                    page = driver.getPageSource();

                    if (page != null && page.length() > 0) {
                        System.out.println("Found products: " + id);
                        TimeUnit.MILLISECONDS.sleep(timeSleep);
                        FileUtils.writeStringToFile(new File(new File(folder, "products"), id), page, Charsets.UTF_8);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(page==null&&overviewFile.exists()) {
                page = FileUtils.readFileToString(overviewFile, Charsets.UTF_8);
            }
            if(page!=null) {
                Document doc = Jsoup.parse(page);
                Elements seeAllLinks = doc.select("a.heading-cta[href]");
                for(Element seeAll : seeAllLinks) {
                    while(seeAll!=null) {
                        String seeAllHref = seeAll.attr("href");
                        String seeAllId = seeAllHref.replace("/", "_");
                        String seeAllUrl = "https://www.leafly.com" + seeAllHref;
                        File seeAllFile = new File(new File(folder, "products"), seeAllId.replace("?",""));
                        if(!seeAllFile.exists()||reseed) {
                            driver.get(seeAllUrl);
                            TimeUnit.MILLISECONDS.sleep(timeSleep);
                            page = driver.getPageSource();
                            FileUtils.writeStringToFile(seeAllFile, page, Charsets.UTF_8);
                        }
                        if(seeAllFile.exists()) {
                            page = FileUtils.readFileToString(seeAllFile, Charsets.UTF_8);
                        }
                        seeAll = null;
                        if(page!=null) {
                            Document productPage = Jsoup.parse(page);
                            Elements next = productPage.select(".leafly-pagination a.next.page-numbers[href]");
                            if (next.size() > 0) {
                                // get next page
                                seeAll = next.get(0);
                            }

                            // get product reviews
                            Elements itemLinks = productPage.select(".product-grid a.item[href]");
                            for(Element itemLink : itemLinks) {
                                String productId;
                                int offset = 0;
                                {
                                    // get product page
                                    String itemHref = itemLink.attr("href");
                                    String itemId = itemHref.replace("/", "_");
                                    String itemUrl = "https://www.leafly.com" + itemHref;
                                    File itemFile = new File(new File(folder, "products"), itemId.replace("?",""));
                                    if (!itemFile.exists() || reseed) {
                                        driver.get(itemUrl);
                                        TimeUnit.MILLISECONDS.sleep(timeSleep);
                                        page = driver.getPageSource();
                                        FileUtils.writeStringToFile(itemFile, page, Charsets.UTF_8);
                                    }
                                    if (itemFile.exists()) {
                                        page = FileUtils.readFileToString(itemFile, Charsets.UTF_8);
                                    }
                                    productId = itemId;
                                    handleProductPage(page, productId, conn);
                                }
                                if(itemLink.select(".rating").size()>0) {
                                    String itemHref = itemLink.attr("href")+"/reviews";
                                    while(itemLink!=null) {
                                        String itemId = itemHref.replace("/", "_").replace("?","");
                                        String itemUrl = "https://www.leafly.com" + itemHref;
                                        File itemFile = new File(new File(folder, "products"), itemId);
                                        if (!itemFile.exists() || reseed) {
                                            driver.get(itemUrl);
                                            TimeUnit.MILLISECONDS.sleep(timeSleep);
                                            page = driver.getPageSource();
                                            FileUtils.writeStringToFile(itemFile, page, Charsets.UTF_8);
                                        }
                                        if (itemFile.exists()) {
                                            page = FileUtils.readFileToString(itemFile, Charsets.UTF_8);
                                        }
                                        itemLink = null;
                                        if (page != null) {
                                            offset += handleRatingsPage(page, productId, conn, offset);
                                            Document itemPage = Jsoup.parse(page);
                                            Elements nextItem = itemPage.select(".leafly-pagination a.next.page-numbers[href]");
                                            if (nextItem.size() > 0) {
                                                // get next page
                                                itemLink = nextItem.get(0);
                                                itemHref = itemLink.attr("href");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        }
        conn.commit();
        conn.close();
    }

    private static void handleProductPage(@NonNull String page, String productId, Connection conn) throws SQLException {
        Document document = Jsoup.parse(page);
        String brandName = document.select(".product-title .brand-name").text().trim();
        String productName = document.select(".product-title .product-name").text().trim();
        String shortDescription = document.select(".product-short-description").text().trim();
        String description = document.select(".product-description").text().trim();
        String productPrice = document.select(".product-price").text().trim();
        String starRating = document.select(".product-rating .star-rating span[star-rating]").attr("star-rating");
        System.out.println("Brand: "+brandName+", Product: "+productName+", Price: "+productPrice+", Rating: "+starRating+"\nDescription: "+shortDescription+"\n"+description+"\n");
        Double productPriceDouble = productPrice.length()>0 ? Double.valueOf(productPrice.replace("$","").replace(",","")) : null;
        Double starRatingDouble = starRating!=null && starRating.length()>0 ? Double.valueOf(starRating) : null;
        final PreparedStatement ps = conn.prepareStatement("insert into products (product_id,product_name, brand_name, short_description, description, price, rating) values (?,?,?,?,?,?,?) on conflict (product_id) do nothing");
        ps.setString(1, productId);
        ps.setString(2, productName);
        ps.setString(3, brandName);
        ps.setString(4, shortDescription);
        ps.setString(5, description);
        ps.setObject(6, productPriceDouble);
        ps.setObject(7, starRatingDouble);
        ps.executeUpdate();
        ps.close();
        conn.commit();
    }


    private static int handleRatingsPage(@NonNull String page, String productId,  Connection conn, int offset) throws SQLException {
        final PreparedStatement ps = conn.prepareStatement("insert into product_reviews (product_id,author,rating,upvotes,downvotes,text,review_num) values (?,?,?,?,?,?,?) on conflict (product_id,review_num) do nothing");
        Document document = Jsoup.parse(page);
        Elements reviews = document.select("div.product-review");
        int reviewNum = offset;
        for(Element review : reviews) {
            String author = review.select("div.author").text().trim();
            String rating = review.select(".review-rating span[star-rating]").attr("star-rating");
            Double ratingDouble = rating!=null && rating.length()>0 ? Double.valueOf(rating) : null;
            String text = review.select(".text").text().trim();
            Elements reviewButtons = review.select("button.review-button");
            String upvotes = null;
            String downvotes = null;
            Double upvotesDouble = null;
            Double downvotesDouble = null;
            if(reviewButtons.size()==2) {
                upvotes = reviewButtons.get(0).select(".vote-count").text();
                downvotes = reviewButtons.get(1).select(".vote-count").text();
                upvotesDouble = upvotes!=null && upvotes.length()>0 ? Double.valueOf(upvotes) : null;
                downvotesDouble = downvotes!=null && downvotes.length()>0 ? Double.valueOf(downvotes) : null;
            }
            System.out.println("Author: "+author+", Rating: "+rating+", upvotes: "+upvotes+", downvotes: "+downvotes+"\n"+text+"\n");
            ps.setString(1, productId);
            ps.setString(2, author);
            ps.setObject(3, ratingDouble);
            ps.setObject(4, upvotesDouble);
            ps.setObject(5, downvotesDouble);
            ps.setString(6, text);
            ps.setInt(7, reviewNum);
            ps.executeUpdate();
            reviewNum++;
        }
        ps.close();
        conn.commit();
        return reviews.size();
    }

    public static void main(String[] args) throws Exception {
        run(false);
    }
}
