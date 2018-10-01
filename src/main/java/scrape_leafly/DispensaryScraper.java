package scrape_leafly;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DispensaryScraper {

    public static void run(boolean reseed) throws Exception {
//        final Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/leaflydb?user=postgres&password=password&tcpKeepAlive=true");
//        conn.setAutoCommit(false);
        long timeSleep = 1000;
        File folder = new File("leafly/");

        List<String> cityIds = Stream.of(FileUtils.readFileToString(new File("cities.psv"), Charsets.UTF_8).split("\\n+"))
                .skip(1).map(line->{
                    String[] cells = line.split("\\|");
                    if(cells.length>3) {
                        return cells[0].toLowerCase().trim().replace(" ","-") + "-" + cells[1].toLowerCase();
                    }
                    return null;
                }).filter(n->n!=null).distinct()
                .collect(Collectors.toList());

        WebDriver driver;
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

        for(String cityId : cityIds) {
            url = "https://www.leafly.com/finder/" + cityId;

            File overviewFile = new File(new File(folder, "dispensaries"), cityId);
            if (!overviewFile.exists() || reseed) {
                try {
                    driver.get(url);
                    TimeUnit.MILLISECONDS.sleep(timeSleep);
                    String cityPage = driver.getPageSource();

                    if (cityPage != null && cityPage.length() > 0) {
                        System.out.println("Found city: " + url);
                        TimeUnit.MILLISECONDS.sleep(timeSleep);
                        FileUtils.writeStringToFile(new File(new File(folder, "dispensaries"), cityId), cityPage, Charsets.UTF_8);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) throws Exception {
        run(true);
    }
}
