package si.um.feri.__Backend.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

@Service
public class cascadeFundingProvider {
    private static final Logger log = LoggerFactory.getLogger(cascadeFundingProvider.class);
    private static final String COLLECTION_RAW = "Listings-Raw(cascadefunding.eu)";
    private static final String SOURCE = "cascadefunding.eu";
    private static final String STATUS_OPEN = "Open";
    private final ListingRepository listingRepository;
    private final WebDriver driver;
    private final MongoTemplate mongoTemplate;
    private final SeleniumDriverManager driverManager;

    public cascadeFundingProvider(ListingRepository listingRepository, MongoTemplate mongoTemplate, SeleniumDriverManager driverManager) throws IOException {
        this.listingRepository = listingRepository;
        this.mongoTemplate = mongoTemplate;
        this.driverManager = driverManager;
        this.driver = driverManager.createChromeDriver();
        log.info("Chrome driver initialized with Selenium Manager");
    }

    public synchronized void scrapeData() throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();

        driver.get("https://cascadefunding.eu/open-calls/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("call-card")));

        List<WebElement> cards = driver.findElements(By.className("call-card"));
        int counter = 1;

        for (WebElement card : cards) {
            Map<String, Object> data = new HashMap<>();
            List<WebElement> titleElements = card.findElements(By.cssSelector("h3.id"));

            if (titleElements.isEmpty() || titleElements.get(0).getText().isBlank()) continue;

            String formattedId = String.format("CHF-%03d", counter++);
            data.put("id", formattedId);

            card.findElements(By.tagName("a")).stream().findFirst().ifPresent(link -> data.put("url", link.getAttribute("href")));
            data.put("title", titleElements.get(0).getText());

            try {
                WebElement summaryP = card.findElement(By.xpath(".//h4[text()='Summary']/following-sibling::p"));
                data.put("summary", summaryP.getText());
            } catch (Exception ignored) {}

            try {
                WebElement closesP = card.findElement(By.xpath(".//h4[text()='Closes']/following-sibling::p"));
                data.put("deadlineDate", closesP.getText());
            } catch (Exception ignored) {}

            data.put("technology", extractTagTexts(card, ".//h4[text()='Technology']/following-sibling::ul/li/a"));
            data.put("domains", extractTagTexts(card, ".//h4[text()='Domains']/following-sibling::ul/li/a"));
            data.put("typeOfBeneficiary", extractTagTexts(card, ".//h4[text()='Type of beneficiary']/following-sibling::ul/li/a"));

            try {
                WebElement fundingP = card.findElement(By.xpath(".//h4[text()='Max funding per project']/following-sibling::p"));
                String fundingText = fundingP.getText().replaceAll("[^\\d,]", "").replace(",", ".");
                data.put("maxFunding", fundingText);
            } catch (Exception ignored) {}
            results.add(data);
            log.info("Processed card: {}", formattedId);
        }
        fetchDescriptionsInParallel(results);
        saveRawLocally(results, "cascadeFundingRawData.json");
        saveRawToMongo(results, SOURCE);
        saveKeywordsToLocalFile(results);
        saveFilteredToMongo(results);
    }

    private List<String> extractTagTexts(WebElement base, String xpath) {
        List<String> texts = new ArrayList<>();

        try {
            List<WebElement> tags = base.findElements(By.xpath(xpath));

            for (WebElement tag : tags) {
                texts.add(tag.getText());
            }
        } catch (Exception ignored) {}
        return texts;
    }

    private void fetchDescriptionsInParallel(List<Map<String, Object>> items) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger counter = new AtomicInteger(0);
        int total = items.size();

        List<Future<?>> futures = new ArrayList<>();

        for (Map<String, Object> item : items) {
            futures.add(executor.submit(() -> {
                int index = counter.incrementAndGet();
                String id = (String) item.get("id");
                log.info("[{}/{}] Fetching details for: {}", index, total, id);

                WebDriver localDriver = driverManager.createParallelChromeDriver();

                try {
                    localDriver.get((String) item.get("url"));
                    WebDriverWait wait = new WebDriverWait(localDriver, Duration.ofSeconds(10));

                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.column.is-8-desktop.is-12-tablet")));

                    // Target the main content field specifically, excluding other elements
                    WebElement mainContent = localDriver.findElement(By.cssSelector("div.column.is-8-desktop.is-12-tablet div.field"));

                    // OPTION 1: Preserve paragraph breaks by replacing <br> and </p> with newlines
                    String description = preserveFormattingWithHTML(localDriver, mainContent);

                    // OPTION 2: Alternative - use getText() but preserve paragraph structure
                    // String description = preserveFormattingWithTextExtraction(details);

                    item.put("description", description);

                } catch (Exception e) {
                    log.warn("Failed to fetch details for {}: {}", id, e.getMessage());
                    item.put("description", "none");

                } finally {
                    driverManager.shutdownDriver(localDriver);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.warn("Error in parallel processing: {}", e.getMessage());
            }
        }
        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("All detail scraping threads complete.");
    }
    private String preserveFormattingWithHTML(WebDriver driver, WebElement element) {
        try {
            // Get the innerHTML using JavaScript
            String innerHTML = (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return arguments[0].innerHTML;", element);

            // Clean up HTML but preserve paragraph structure and bold text
            String cleanText = innerHTML
                    .replaceAll("<br\\s*/?>", "\n")           // Replace <br> tags with newlines
                    .replaceAll("</p>", "\n\n")              // Replace </p> with double newlines
                    .replaceAll("<p[^>]*>", "")              // Remove opening <p> tags
                    .replaceAll("<b[^>]*>", "**")            // Replace <b> with markdown bold
                    .replaceAll("</b>", "**")                // Replace </b> with markdown bold
                    .replaceAll("<strong[^>]*>", "**")       // Replace <strong> with markdown bold
                    .replaceAll("</strong>", "**")           // Replace </strong> with markdown bold
                    .replaceAll("<[^>]+>", "")               // Remove all other HTML tags
                    .replaceAll("&nbsp;", " ")               // Replace &nbsp; with spaces
                    .replaceAll("&amp;", "&")                // Replace &amp; with &
                    .replaceAll("&lt;", "<")                 // Replace &lt; with <
                    .replaceAll("&gt;", ">")                 // Replace &gt; with >
                    .replaceAll("&quot;", "\"")              // Replace &quot; with "
                    .replaceAll("\\s+", " ")                 // Replace multiple spaces with single space
                    .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n") // Replace multiple newlines with double newlines
                    .replaceAll("\\n ", "\n")                // Remove spaces after newlines
                    .replaceAll(" \\n", "\n")                // Remove spaces before newlines
                    .trim();

            // Remove unwanted sections at the end
            cleanText = removeUnwantedSections(cleanText);

            return cleanText;

        } catch (Exception e) {
            log.warn("Failed to extract formatted HTML, falling back to getText(): {}", e.getMessage());
            return element.getText().trim();
        }
    }

    private String removeUnwantedSections(String text) {
        // Remove sections that are already captured separately
        String[] sectionsToRemove = {
                "Links\\s*Website\\s*Guidelines\\s*Apply",
                "Closes\\s*\\d{2}/\\d{2}/\\d{4}",
                "Max funding per project\\s*[\\d.,]+€?"
        };

        for (String section : sectionsToRemove) {
            text = text.replaceAll("(?i)" + section + ".*$", "").trim();
        }

        // Clean up any trailing whitespace or extra newlines at the end
        text = text.replaceAll("\\n\\s*$", "");

        return text;
    }

    public void saveRawLocally(List<Map<String, Object>> dataList, String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String outputPath = System.getProperty("user.dir") + "/output/rawData/cascadeFunding/" + fileName;
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
        List<String> allPossibleIds = results.stream().map(data -> SOURCE + ":" + data.get("id") + ":" + STATUS_OPEN).toList();

        Set<String> existingIdentifiers = new HashSet<>(listingRepository.findAllBySourceIdentifierIn(allPossibleIds)
                .stream()
                .map(Listing::getSourceIdentifier)
                .toList());

        for (Map<String, Object> data : results) {
            String identifier = (String) data.get("id");
            if (identifier == null) continue;

            String sourceIdentifier = SOURCE + ":" + identifier + ":" + STATUS_OPEN;
            if (existingIdentifiers.contains(sourceIdentifier)) {
                log.warn("Duplicate listing found: {}", sourceIdentifier);
                continue;
            }

            Listing listing = new Listing();
            listing.setSourceIdentifier(sourceIdentifier);
            listing.setStatus(STATUS_OPEN);
            listing.setSource(SOURCE);
            listing.setTitle((String) data.get("title"));
            listing.setDeadlineDate(((String) data.get("deadlineDate")).replace("/", "."));
            listing.setUrl((String) data.get("url"));
            listing.setSummary((String) data.get("summary"));
            listing.setBudget((String) data.get("maxFunding"));
            listing.setDescription((String) data.get("description"));

            Object techObj = data.get("technology");
            if (techObj instanceof List<?> rawList) {
                List<String> techList = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof String) {
                        techList.add((String) item);
                    }
                }
                listing.setTechnologies(techList);
            }

            Object domObj = data.get("domains");
            if (domObj instanceof List<?> rawList) {
                List<String> domainList = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof String) {
                        domainList.add((String) item);
                    }
                }
                listing.setIndustries(domainList);
            }

            listings.add(listing);
        }
        listingRepository.saveAll(listings);
        log.info("Saved {} filtered listings to MongoDB.", listings.size());
    }

    public void saveKeywordsToLocalFile(List<Map<String, Object>> dataList) throws IOException {
        Set<String> keywords = new HashSet<>();

        for (Map<String, Object> data : dataList) {
            Object techObj = data.get("technology");
            if (techObj instanceof List<?>) {
                for (Object item : (List<?>) techObj) {
                    if (item instanceof String) {
                        keywords.add((String) item);
                    }
                }
            }

            Object domObj = data.get("domains");
            if (domObj instanceof List<?>) {
                for (Object item : (List<?>) domObj) {
                    if (item instanceof String) {
                        keywords.add((String) item);
                    }
                }
            }
        }

        String filePath = System.getProperty("user.dir") + "/output/keywords/cascadeFunding/keywords.txt";
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());

        List<String> sortedKeywords = new ArrayList<>(keywords);
        Collections.sort(sortedKeywords);
        Files.write(path, sortedKeywords);

        log.info("Saved {} keywords to: {}", sortedKeywords.size(), filePath);
    }
}