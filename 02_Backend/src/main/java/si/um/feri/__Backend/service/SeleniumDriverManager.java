package si.um.feri.__Backend.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@Component
public class SeleniumDriverManager {
    private static final Logger log = LoggerFactory.getLogger(SeleniumDriverManager.class);

    public SeleniumDriverManager() {
        configureSeleniumManager();
    }

    private void configureSeleniumManager() {
        System.setProperty("SE_CACHE_PATH", System.getProperty("user.dir") + "/.selenium-cache");
        System.setProperty("SE_AVOID_STATS", "true");
        System.setProperty("SE_TIMEOUT", "300");

        if (isRunningInDocker()) {
            System.setProperty("SE_FORCE_BROWSER_DOWNLOAD", "true");
            log.info("Docker environment detected - forcing browser download");
        }
        log.info("Selenium Manager configured with cache path: {}", System.getProperty("SE_CACHE_PATH"));
    }

    private boolean isRunningInDocker() {
        return Files.exists(Paths.get("/.dockerenv")) ||
                System.getenv("DOCKER_CONTAINER") != null ||
                System.getProperty("java.awt.headless", "false").equals("true");
    }

    public WebDriver createChromeDriver() {
        ChromeOptions options = createBaseOptions();
        ChromeDriver chromeDriver = new ChromeDriver(options);
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        return chromeDriver;
    }

    public WebDriver createParallelChromeDriver() {
        ChromeOptions options = createBaseOptions();
        ChromeDriver chromeDriver = new ChromeDriver(options);
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        return chromeDriver;
    }

    private ChromeOptions createBaseOptions() {
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        options.addArguments(
//                "--headless=new",
//                "--no-sandbox",
//                "--disable-dev-shm-usage",
//                "--disable-gpu",
//                "--window-size=1920,1080",
//                "--disable-extensions",
//                "--disable-logging",
//                "--disable-features=VizDisplayCompositor",
//                "--memory-pressure-off",
//                "--max_old_space_size=4096"
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--blink-settings=imagesEnabled=false",
                "--disable-extensions",
                "--disable-web-security",
                "--disable-features=VizDisplayCompositor",
                "--disable-background-timer-throttling",
                "--disable-backgrounding-occluded-windows",
                "--disable-renderer-backgrounding",
                "--disable-features=TranslateUI",
                "--disable-ipc-flooding-protection"
        );
        return options;
    }

    public ThreadLocal<WebDriver> createThreadLocalDriver() {
        return ThreadLocal.withInitial(this::createParallelChromeDriver);
    }

    public void shutdownDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Error during driver shutdown: {}", e.getMessage());
            }
        }
    }

    public void cleanupThreadLocalDriver(ThreadLocal<WebDriver> threadLocalDriver) {
        try {
            WebDriver driver = threadLocalDriver.get();
            if (driver != null) {
                driver.quit();
            }
            threadLocalDriver.remove();
        } catch (Exception e) {
            log.warn("Error cleaning up ThreadLocal drivers: {}", e.getMessage());
        }
    }

    public void clearBrowserState(WebDriver driver) {
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception e) {
            log.warn("Error clearing browser state: {}", e.getMessage());
        }
    }
}