package si.um.feri.__Backend.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import si.um.feri.__Backend.model.Listing;
import si.um.feri.__Backend.model.rawListings.ecEuropaRaw;
import si.um.feri.__Backend.repository.ListingRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ecEuropaProvider {
    private static final Logger log = LoggerFactory.getLogger(ecEuropaProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ListingRepository listingRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${app.output.base-path}")
    private String basePath;

    public ecEuropaProvider(ListingRepository listingRepository, MongoTemplate mongoTemplate) {
        this.listingRepository = listingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void fetchListings(String queryFileName) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        Resource queryFile = new ClassPathResource("filters/ecEuropaFilters/" + queryFileName);
        Resource sortFile = new ClassPathResource("filters/ecEuropaFilters/sortQuery.json");
        String suffix = queryFileName.replace("query", "ecEuropa").replace(".json", "RawData.json");
        String outputPath = Paths.get(System.getProperty("user.dir"), basePath, "ecEuropa", suffix).toString();

        // Delete existing listings
        Files.deleteIfExists(Paths.get(outputPath));

        String provider = "ec.europa.eu:" + queryFileName.replace("query", "").replace(".json", "");
        mongoTemplate.getCollection("Listings-Raw(ec.europa.eu)")
                .deleteMany(new Document("sourceProvider", provider));

        int page = 1, pageSize = 100;

        while (true) {
            String url = "https://api.tech.ec.europa.eu/search-api/prod/rest/search?apiKey=SEDIA&text=***&isExactMatch=true&pageSize=" + pageSize + "&pageNumber=" + page;
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

            body.add("query", new HttpEntity<>(queryFile, jsonHeaders));
            body.add("sort", new HttpEntity<>(sortFile, jsonHeaders));
            body.add("languages", new HttpEntity<>("[\"en\"]", jsonHeaders));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            String jsonBody = response.getBody();

            JsonNode root = MAPPER.readTree(jsonBody);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) break;
            saveRawToMongo(jsonBody, "ec.europa.eu:" + queryFileName.replace("query", "").replace(".json", ""));
            saveRawLocally(jsonBody, outputPath);

            // process filtered items
            List<JsonNode> pageResults = new ArrayList<>();
            results.forEach(pageResults::add);
            saveFilteredToMongo(pageResults);

            // log and continue
            log.info("Fetched page {} with {} listings", page, results.size());
            page++;
        }
        generateKeywordsFromMongo();
        log.info("Finished fetching listings from ec.europa.eu");
    }

    public void saveRawToMongo(String pageJson, String provider) throws IOException {
        JsonNode results = MAPPER.readTree(pageJson).path("results");
        if (!results.isArray()) return;

        List<Document> documents = new ArrayList<>();
        for (JsonNode resultNode : results) {
            Document bsonDoc = Document.parse(resultNode.toString());
            bsonDoc.put("sourceProvider", provider);
            documents.add(bsonDoc);
        }
        if (!documents.isEmpty()) {
            mongoTemplate.getCollection("Listings-Raw(ec.europa.eu)").insertMany(documents);
            log.info("Inserted {} raw documents into MongoDB for provider: {}", documents.size(), provider);
        }
    }

    public void saveRawLocally(String jsonPage, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(jsonPage);
        }
    }

    private void saveFilteredToMongo(List<JsonNode> items) throws IOException {
        List<Listing> listingsToSave = new ArrayList<>();
        Set<String> identifiersToCheck = new HashSet<>();
        Map<String, JsonNode> nodesBySourceId = new HashMap<>();

        for (JsonNode itemNode : items) {
            ecEuropaRaw raw = MAPPER.treeToValue(itemNode, ecEuropaRaw.class);
            ecEuropaRaw.Metadata m = raw.getMetadata();
            String id = first(m.getIdentifier());
            String status = mapStatus(first(m.getStatus()));
            String sourceId = "ec.europa.eu:" + id + ":" + status;
            identifiersToCheck.add(sourceId);
            nodesBySourceId.put(sourceId, itemNode);
        }

        Set<String> existingIds = listingRepository.findAllBySourceIdentifierIn(identifiersToCheck)
                .stream()
                .map(Listing::getSourceIdentifier)
                .collect(Collectors.toSet());

        AtomicInteger counter = new AtomicInteger(0);
        int total = identifiersToCheck.size();

        for (Map.Entry<String, JsonNode> entry : nodesBySourceId.entrySet()) {
            String sourceId = entry.getKey();
            int current = counter.incrementAndGet();

            if (existingIds.contains(sourceId)) {
                log.warn("[{}/{}] Skipping duplicate: {}", current, total, sourceId);
                continue;
            }

            ecEuropaRaw raw = MAPPER.treeToValue(entry.getValue(), ecEuropaRaw.class);
            ecEuropaRaw.Metadata m = raw.getMetadata();

            if (!Objects.equals(first(m.getDeadlineModel()), "single-stage")) {
                log.debug("[{}/{}] Skipping multi-stage deadline model: {}", current, total, first(m.getIdentifier()));
                continue;
            }

            Listing listing = new Listing();
            String identifier = first(m.getIdentifier());
            listing.setSourceIdentifier(sourceId);
            listing.setSource("ec.europa.eu");
            listing.setStatus(mapStatus(first(m.getStatus())));
            listing.setTitle(first(m.getTitle()));
            listing.setStartDate(convertDate(first(m.getStartDate())));
            listing.setDeadlineDate(convertDate(first(m.getDeadlineDate())));
            listing.setUrl("https://ec.europa.eu/info/funding-tenders/opportunities/portal/screen/opportunities/topic-details/" + identifier);
            String description = cleanString(first(m.getDescriptionByte()));
            listing.setSummary(description);
            listing.setDescription(description);
            listing.setKeywords(m.getTags());
            listing.setIndustries(m.getCrossCuttingPriorities());

            Optional.ofNullable(m.getTypesOfAction())
                    .ifPresent(list -> listing.setTechnologies(
                            list.stream()
                                    .map(s -> s.replace("HORIZON", "").trim())
                                    .toList()
                    ));

            String budget = first(m.getBudget());
            if (budget != null && !budget.isBlank()) {
                listing.setBudget(budget);
            } else {
                listing.setBudget(parseBudgetFromOverview(m.getBudgetOverview(), identifier));
            }
            listingsToSave.add(listing);
            log.info("[{}/{}] Processed listing: {}", current, total, sourceId);
        }
        listingRepository.saveAll(listingsToSave);
        log.info("Saved {} filtered listings to MongoDB", listingsToSave.size());
    }

    private String mapStatus(String code) {
        return switch (code) {
            case "31094501" -> "Forthcoming";
            case "31094502" -> "Open";
            case "31094503" -> "Closed";
            default -> "Unknown";
        };
    }

    private String parseBudgetFromOverview(List<String> overview, String identifier) throws IOException {
        if (overview == null || overview.isEmpty()) return null;
        JsonNode budgetJson = MAPPER.readTree(overview.get(0));
        JsonNode topicMap = budgetJson.path("budgetTopicActionMap");

        double matched = 0;
        if (topicMap.isObject()) {
            for (JsonNode actions : topicMap) {
                for (JsonNode action : actions) {
                    String actionName = action.path("action").asText();
                    if (actionName.contains(identifier)) {
                        JsonNode yearMap = action.path("budgetYearMap");
                        for (JsonNode value : yearMap) {
                            matched += value.asDouble();
                        }
                    }
                }
            }
        }
        return matched > 0 ? String.format("%.0f", matched) : null;
    }

    private String first(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public static String convertDate(String inputDate) {
        if (inputDate == null) return null;
        OffsetDateTime odt = OffsetDateTime.parse(inputDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        return odt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String cleanString(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        String[] sentences = getSentences(raw);
        StringBuilder meaningful = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 80 && !trimmed.matches("(?i).*\\b(section|area|topic|proposal|expected|scope|duration|criteria)\\b.*")) {
                meaningful.append(trimmed).append(". ");
            }
        }
        return meaningful.toString().trim();
    }

    private static String[] getSentences(String raw) {
        String cleaned = raw.replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<style.*?>.*?</style>", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ").trim();

        cleaned = cleaned.replaceAll("(?i)(expected outcome|scope|area \\w:|summary):", "");
        cleaned = cleaned.replaceAll("(?i)(expected outcome|scope|area \\w:|summary projects? (are|is) expected to|proposals? (should|are expected to)|this topic (is|will be|implements)).*?([.;])", "");

        return cleaned.split("\\. ");
    }

    public void saveIndustriesAndTechnologiesToFile(List<Listing> listings) throws IOException {
        Set<String> uniqueItems = new HashSet<>();

        for (Listing listing : listings) {
            if (listing.getIndustries() != null) {
                uniqueItems.addAll(listing.getIndustries());
            }
            if (listing.getTechnologies() != null) {
                uniqueItems.addAll(listing.getTechnologies());
            }
        }

        List<String> sorted = new ArrayList<>(uniqueItems);
        Collections.sort(sorted);

        Path path = Paths.get(System.getProperty("user.dir"), "/output/keywords/ecEuropa", "keywords.txt");

        Files.createDirectories(path.getParent());
        Files.write(path, sorted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Saved {} keywords to file: {}", sorted.size(), path);
    }

    public void generateKeywordsFromMongo() throws IOException {
        List<Listing> allListings = listingRepository.findAllBySource("ec.europa.eu");
        saveIndustriesAndTechnologiesToFile(allListings);
    }
}