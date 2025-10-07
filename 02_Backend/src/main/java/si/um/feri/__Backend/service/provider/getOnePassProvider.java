package si.um.feri.__Backend.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import si.um.feri.__Backend.model.Listing;
import si.um.feri.__Backend.repository.ListingRepository;
import si.um.feri.__Backend.service.SeleniumDriverManager;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class getOnePassProvider {
    private static final Logger log = LoggerFactory.getLogger(getOnePassProvider.class);
    private static final String COLLECTION_RAW = "Listings-Raw(getonepass.eu)";
    private static final String SOURCE = "getonepass.eu";
    private static final String STATUS_OPEN = "Open";
    private static final String STATUS_FORTHCOMING = "Forthcoming";
    private static final String STATUS_UNKNOWN = "Unknown";
    private final ListingRepository listingRepository;
    private final WebDriver driver;
    private final MongoTemplate mongoTemplate;
    private final ThreadLocal<WebDriver> threadLocalDriver;
    private final SeleniumDriverManager driverManager;

    public getOnePassProvider(ListingRepository listingRepository, MongoTemplate mongoTemplate, SeleniumDriverManager driverManager) {
        this.listingRepository = listingRepository;
        this.mongoTemplate = mongoTemplate;
        this.driverManager = driverManager;
        this.driver = driverManager.createChromeDriver();
        this.threadLocalDriver = driverManager.createThreadLocalDriver();
        log.info("Chrome driver initialized with Selenium Manager");
    }

    public synchronized void scrapeData() throws IOException {
        List<Map<String, Object>> results = scrapeListingPages();
        fetchDetailsInParallel(results);
        saveRawLocally(results, "getOnePassRawData.json");
        saveRawToMongo(results, SOURCE);
        saveKeywordsToLocalFile(results);
        saveFilteredToMongo(results);
    }

    private List<Map<String, Object>> scrapeListingPages() {
        List<Map<String, Object>> results = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int currentPage = 1;
        int globalCounter = 1;

        while (true) {
            String pageUrl = "https://hub.getonepass.eu/search/opportunities?page=" + currentPage;
            driver.get(pageUrl);
            log.info("Processing page: {}", currentPage);

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.discoveryCard")));
                List<WebElement> cards = driver.findElements(By.cssSelector("div.discoveryCard"));

                if (cards.isEmpty()) {
                    log.info("No cards found on page {}, stopping pagination", currentPage);
                    break;
                }

                for (WebElement card : cards) {
                    try {
                        Map<String, Object> data = processCard(card, globalCounter++);
                        results.add(data);
                        log.info("Processed card: {}", data.get("id"));
                    } catch (Exception e) {
                        log.warn("Error processing card {} on page {}: {}", globalCounter, currentPage, e.getMessage());
                        globalCounter++;
                    }
                }
                currentPage++;
            } catch (Exception e) {
                log.warn("Error processing page {}: {}", currentPage, e.getMessage());
                break;
            }
        }
        return results;
    }

    private Map<String, Object> processCard(WebElement card, int counter) {
        Map<String, Object> data = new HashMap<>();
        String formattedId = String.format("ONEPASS-%03d", counter);
        data.put("id", formattedId);

        try {
            WebElement titleLink = card.findElement(By.cssSelector("h3.ui.wide.header a"));
            data.put("title", titleLink.getText().trim());

            String link = titleLink.getAttribute("href");
            if (link != null) {
                data.put("url", link.startsWith("http") ? link : "https://hub.getonepass.eu" + link);
            }
        } catch (Exception e) {
            log.warn("Error extracting title/URL for {}: {}", formattedId, e.getMessage());
        }

        try {
            card.findElements(By.xpath(".//p")).stream()
                    .findFirst()
                    .ifPresent(p -> data.put("summary", p.getText().trim()));
        } catch (Exception ignored) {}

        try {
            List<String> industries = card.findElements(By.cssSelector("div.tags a.industrieTag")).stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .collect(Collectors.toList());
            data.put("industry", industries);
        } catch (Exception ignored) {
            data.put("industry", new ArrayList<>());
        }

        try {
            card.findElements(By.xpath(".//p")).stream()
                    .filter(p -> p.getText().toLowerCase().contains("apply before"))
                    .findFirst()
                    .ifPresent(p -> {
                        String[] parts = p.getText().split("·");
                        if (parts.length >= 1) {
                            data.put("status", parts[0].trim());
                        }
                    });
        } catch (Exception ignored) {}

        return data;
    }

    private void fetchDetailsInParallel(List<Map<String, Object>> results) {
        if (results.isEmpty()) return;

        int availableCores = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.max(2, Math.min(availableCores, 4));
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Semaphore pageLoadSemaphore = new Semaphore(threadCount * 2, true);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger progress = new AtomicInteger(0);
        int total = results.size();

        List<CompletableFuture<Void>> futures = results.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    int index = progress.incrementAndGet();
                    String id = (String) item.get("id");

                    try {
                        // Acquire permit before loading page
                        if (!pageLoadSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                            log.warn("[{}/{}] Timeout waiting for resources: {}", index, total, id);
                            failureCount.incrementAndGet();
                            return;
                        }

                        log.info("[{}/{}] Fetching details for: {}", index, total, id);
                        fetchDetailsWithRetry(item); // 3 retry attempts
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("[{}/{}] Interrupted while processing: {}", index, total, id);
                        failureCount.incrementAndGet();
                    } finally {
                        pageLoadSemaphore.release();
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        log.info("Detail scraping complete. Success: {}, Failures: {}, Total: {}", successCount.get(), failureCount.get(), total);
    }

    private void fetchDetailsWithRetry(Map<String, Object> item) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                fetchDetails(item);
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.warn("Final attempt failed for {}: {}", item.get("id"), e.getMessage());
                    item.put("deadlineDate", STATUS_UNKNOWN);
                    item.put("description", "none");
                    item.put("technology", new ArrayList<>());
                } else {
                    log.debug("Retry attempt {}/{} for {}: {}", attempt, maxRetries, item.get("id"), e.getMessage());
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during backoff", ie);
                    }
                }
            }
        }
    }

    private void fetchDetails(Map<String, Object> item) {
        WebDriver driver = threadLocalDriver.get();
        try {
            String url = (String) item.get("url");
            if (url == null) {
                throw new IllegalArgumentException("No URL found");
            }

            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            try {
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("div.ui.big.horizontal.divided.menu")),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("div.ui.eleven.wide.column"))
                ));
            } catch (Exception ignored) {}

            try {
                driver.findElements(By.cssSelector("div.ui.big.horizontal.divided.menu > .item")).stream()
                        .filter(menuItem -> {
                            try {
                                return menuItem.findElement(By.cssSelector(".header")).getText().trim()
                                        .equalsIgnoreCase("Apply before");
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .ifPresent(menuItem -> {
                            String text = menuItem.getText().replace("Apply before", "").trim();
                            item.put("deadlineDate", text);
                        });
            } catch (Exception ignored) {}

            if (!item.containsKey("deadlineDate")) {
                item.put("deadlineDate", STATUS_UNKNOWN);
            }

            try {
                WebElement descEl = driver.findElement(By.cssSelector("div.ui.eleven.wide.column"));
                String description = preserveFormattingWithH3Headers(driver, descEl);
                item.put("description", description);
            } catch (Exception e) {
                item.put("description", "none");
            }

            try {
                List<String> technologies = driver.findElements(By.cssSelector("div.meta.search-engine-technologies a.ui.label")).stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
                item.put("technology", technologies);
            } catch (Exception e) {
                item.put("technology", new ArrayList<>());
            }

        } finally {
            driverManager.clearBrowserState(driver);
        }
    }

    private String preserveFormattingWithH3Headers(WebDriver driver, WebElement element) {
        try {
            String innerHTML = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", element);

            assert innerHTML != null;
            return innerHTML
                    .replaceAll("<!--[\\s\\S]*?-->", "")      // Remove HTML comments completely
                    .replaceAll("--\\\\u003E", "\n")          // Replace --\u003E with newlines
                    .replaceAll("--&gt;", "\n")               // Replace --&gt; with newlines
                    .replaceAll("--&amp;gt;", "\n")           // Replace --&amp;gt; with newlines
                    .replaceAll("<br\\s*/?>", "\n")           // Replace <br> tags with newlines
                    .replaceAll("</p>", "\n\n")               // Replace </p> with double newlines
                    .replaceAll("<p[^>]*>", "")               // Remove opening <p> tags
                    .replaceAll("<h3[^>]*>", "**")            // Replace <h3> with markdown bold start
                    .replaceAll("</h3>", "**:")               // Replace </h3> with markdown bold end + colon
                    .replaceAll("<li[^>]*>", "\n• ")          // Replace <li> with bullet points
                    .replaceAll("</li>", "")                  // Remove </li> tags
                    .replaceAll("<ul[^>]*>", "\n")            // Replace <ul> with newline
                    .replaceAll("</ul>", "\n")                // Replace </ul> with newline
                    .replaceAll("<[^>]+>", "")                // Remove all other HTML tags
                    .replaceAll("&nbsp;", " ")                // Replace &nbsp; with spaces
                    .replaceAll("&amp;", "&")                 // Replace &amp; with &
                    .replaceAll("&lt;", "<")                  // Replace &lt; with <
                    .replaceAll("&gt;", ">")                  // Replace &gt; with >
                    .replaceAll("&quot;", "\"")               // Replace &quot; with "
                    .replaceAll("\\\\u003E", ">")             // Replace Unicode escape for >
                    .replaceAll("\\s+", " ")                  // Replace multiple spaces with single space
                    .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n") // Replace multiple newlines with double newlines
                    .replaceAll("\\n ", "\n")                 // Remove spaces after newlines
                    .replaceAll(" \\n", "\n")                 // Remove spaces before newlines
                    .trim();

        } catch (Exception e) {
            log.warn("Failed to extract formatted HTML, falling back to getText(): {}", e.getMessage());
            return element.getText().trim();
        }
    }

    public void saveRawLocally(List<Map<String, Object>> dataList, String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String outputPath = System.getProperty("user.dir") + "/output/rawData/getOnePass/" + fileName;
        Files.createDirectories(Paths.get(outputPath).getParent());

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(mapper.writeValueAsString(dataList));
        }
        log.info("Saved scraped data to: {}", outputPath);
    }

    public void saveRawToMongo(List<Map<String, Object>> results, String provider) {
        mongoTemplate.getCollection(COLLECTION_RAW).deleteMany(new Document("sourceProvider", provider));
        List<Document> documents = results.stream().map(item -> {
            Document doc = new Document(item);
            doc.put("sourceProvider", provider);
            return doc;
        }).toList();

        if (!documents.isEmpty()) {
            mongoTemplate.getCollection(COLLECTION_RAW).insertMany(documents);
        }
        log.info("Saved {} raw items to MongoDB for provider: {}", documents.size(), provider);
    }

    private void saveFilteredToMongo(List<Map<String, Object>> results) {
        List<Listing> listings = new ArrayList<>();
        List<String> allPossibleIds = results.stream()
                .map(data -> {
                    String id = (String) data.get("id");
                    String status = normalizeStatus((String) data.get("status"));
                    return id != null ? SOURCE + ":" + id + ":" + status : null;
                })
                .filter(Objects::nonNull)
                .toList();

        Set<String> existingIdentifiers = new HashSet<>(listingRepository.findAllBySourceIdentifierIn(allPossibleIds)
                .stream()
                .map(Listing::getSourceIdentifier)
                .toList());

        for (Map<String, Object> data : results) {
            String identifier = (String) data.get("id");
            if (identifier == null) continue;

            String status = normalizeStatus((String) data.get("status"));
            String sourceIdentifier = SOURCE + ":" + identifier + ":" + status;

            if (existingIdentifiers.contains(sourceIdentifier)) {
                log.warn("Duplicate listing found: {}", sourceIdentifier);
                continue;
            }

            Listing listing = new Listing();
            listing.setSourceIdentifier(sourceIdentifier);
            listing.setStatus(status);
            listing.setSource(SOURCE);
            listing.setTitle((String) data.get("title"));
            listing.setDeadlineDate(((String) data.get("deadlineDate")).replace("/", "."));            listing.setUrl((String) data.get("url"));
            listing.setSummary((String) data.get("summary"));
            listing.setDescription((String) data.get("description"));

            Object techObj = data.get("technology");
            if (techObj instanceof List<?>) {
                listing.setTechnologies(((List<?>) techObj).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toList()));
            }

            Object industryObj = data.get("industry");
            if (industryObj instanceof List<?>) {
                listing.setIndustries(((List<?>) industryObj).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toList()));
            }
            listings.add(listing);
        }
        listingRepository.saveAll(listings);
        log.info("Saved {} filtered listings to MongoDB.", listings.size());
    }

    private String normalizeStatus(String status) {
        if (status == null) return STATUS_UNKNOWN;
        return switch (status) {
            case "Open for applications", "Always open" -> STATUS_OPEN;
            case "Coming soon" -> STATUS_FORTHCOMING;
            default -> status.isEmpty() ? STATUS_UNKNOWN : status;
        };
    }

    public void saveKeywordsToLocalFile(List<Map<String, Object>> dataList) throws IOException {
        Set<String> keywords = new HashSet<>();

        for (Map<String, Object> data : dataList) {
            Object techObj = data.get("technology");
            Object industryObj = data.get("industry");

            if (techObj instanceof List<?>) {
                ((List<?>) techObj).forEach(item -> {
                    if (item instanceof String) {
                        Collections.addAll(keywords, ((String) item).split("\\s*,\\s*"));
                    }
                });
            }

            if (industryObj instanceof List<?>) {
                ((List<?>) industryObj).forEach(item -> {
                    if (item instanceof String) {
                        Collections.addAll(keywords, ((String) item).split("\\s*,\\s*"));
                    }
                });
            }
        }

        String filePath = System.getProperty("user.dir") + "/output/keywords/getOnePass/keywords.txt";
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());

        List<String> sortedKeywords = new ArrayList<>(keywords);
        Collections.sort(sortedKeywords);
        Files.write(path, sortedKeywords);

        log.info("Saved {} keywords to: {}", sortedKeywords.size(), filePath);
    }

    @PreDestroy
    public void shutdown() {
        driverManager.shutdownDriver(driver);
        driverManager.cleanupThreadLocalDriver(threadLocalDriver);
    }
}