package scrape_leafly;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductScraper {

    public static void run(boolean reseed) throws Exception {
//        final Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
//        conn.setAutoCommit(false);
        long timeSleep = 100;
        File folder = new File("leafly/");

        WebDriver driver;
        String url = "https://www.leafly.com/products";
        ChromeOptions options = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        System.setProperty("webdriver.firefox.driver", "/usr/bin/geckodriver");
        driver = new ChromeDriver(options);

        driver.get(url);
        for (int i = 0; i < 10; i++) {
            System.out.println("Starting in " + (10 - i));
            TimeUnit.MILLISECONDS.sleep(1000);
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
                        driver.get(seeAllUrl);
                        TimeUnit.MILLISECONDS.sleep(timeSleep);
                        page = driver.getPageSource();
                        FileUtils.writeStringToFile(new File(new File(folder, "products"), seeAllId), page, Charsets.UTF_8);
                        Document productPage = Jsoup.parse(page);
                        Elements next = productPage.select(".leafly-pagination a.next.page-numbers[href]");
                        seeAll = null;
                        if (next.size() > 0) {
                            // get next page
                            seeAll = next.get(0);
                        }
                    }
                }
            }


        }

    }

    public static void main(String[] args) throws Exception {
        run(true);
    }
}
