package si.um.feri.__Backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import si.um.feri.__Backend.model.FetchLog;
import si.um.feri.__Backend.repository.FetchLogRepository;
import si.um.feri.__Backend.repository.ListingRepository;
import si.um.feri.__Backend.scheduler.DynamicEcEuropaSheduler;
import si.um.feri.__Backend.scheduler.DynamicScraperScheduler;
import si.um.feri.__Backend.service.ApiKeyService;
import si.um.feri.__Backend.service.FetchIntervalService;
import si.um.feri.__Backend.service.FetchLogService;
import si.um.feri.__Backend.service.provider.cascadeFundingProvider;
import si.um.feri.__Backend.service.provider.ecEuropaProvider;
import si.um.feri.__Backend.service.provider.getOnePassProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
@CrossOrigin
@RequiredArgsConstructor
public class ManagementController {
    private final FetchLogRepository fetchLogRepository;
    private final ListingRepository  listingRepository;
    private final ecEuropaProvider ecEuropaProvider;
    private final FetchLogService fetchLogService;
    private final FetchIntervalService fetchIntervalService;
    private final DynamicEcEuropaSheduler dynamicEcEuropaSheduler;
    private final ApiKeyService apiKeyService;
    private final cascadeFundingProvider cascadeFundingProvider;
    private final getOnePassProvider getOnePassProvider;
    private final DynamicScraperScheduler dynamicScraperScheduler;


    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("healthStatus", "OK");
        response.put("totalListings", listingRepository.count());

        response.put("openCount", listingRepository.countByStatusIgnoreCase("Open"));
        response.put("closedCount", listingRepository.countByStatusIgnoreCase("Closed"));
        response.put("forthcomingCount", listingRepository.countByStatusIgnoreCase("Forthcoming"));

        response.put("fetchLogs", fetchLogRepository.findTop20ByOrderByTimeOfFetchDesc());

        FetchLog lastLog = fetchLogRepository.findTop1BySourceOrderByTimeOfFetchDesc("ecEuropa");
        if (lastLog != null) {
            LocalDateTime nextAllowed = lastLog.getTimeOfFetch().plusHours(6);
            response.put("nextAllowedScrapeTime", nextAllowed.toString()); // ISO format
        }

        return ResponseEntity.ok(response);
    }
    @PostMapping("/geather-now")
    public ResponseEntity<?> manualScrape() {
        FetchLog lastLog = fetchLogRepository.findTop1BySourceOrderByTimeOfFetchDesc("ecEuropa");

        if (lastLog != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = lastLog.getTimeOfFetch();
            long hoursDiff = java.time.Duration.between(last, now).toHours();

            if (hoursDiff < 6) {
                long minutesLeft = 360 - java.time.Duration.between(last, now).toMinutes();
                return ResponseEntity
                        .status(429)
                        .body("❌ Scraping je dovoljen vsakih 6 ur. Počakaj še " + minutesLeft + " minut.");
            }
        }

//        Map<String, Object> result = new HashMap<>();
        try {
            ecEuropaProvider.fetchListings("queryOpen.json");
            fetchLogService.logFetch("ecEuropa", "Open");

            ecEuropaProvider.fetchListings("queryForthcoming.json");
            fetchLogService.logFetch("ecEuropa", "Forthcoming");

            ecEuropaProvider.fetchListings("queryClosed.json");
            fetchLogService.logFetch("ecEuropa", "Closed");

//            result.put("ecEuropa", "✅ OK");
        } catch (IOException e) {
//            result.put("ecEuropa", "❌ " + e.getMessage());
        }

        try {
            cascadeFundingProvider.scrapeData();
            fetchLogService.logFetch("cascadeFunding", "All statuses");
//            result.put("cascadeFunding", "✅ OK");
        } catch (IOException e) {
//            result.put("cascadeFunding", "❌ " + e.getMessage());
        }

        try {
            getOnePassProvider.scrapeData();
            fetchLogService.logFetch("getOnePass", "All statuses");
//            result.put("getOnePass", "✅ OK");
        } catch (IOException e) {
//            result.put("getOnePass", "❌ " + e.getMessage());
        }

        return ResponseEntity.ok( "✅ Gather all Listings" );
    }

    @PostMapping("/set-intervals")
    public ResponseEntity<String> setIntervals(@RequestBody Map<String, Integer> body) {
        Integer shortHours = body.get("shortHours");
        Integer longHours = body.get("longHours");
        Integer scrapingHours = body.get("scrapingHours");
        Integer scrapingHourOfDay = body.get("scrapingHourOfDay");
        if (shortHours == null || longHours == null || scrapingHours == null || scrapingHourOfDay == null ||
                shortHours <= 0 || longHours <= 0 || scrapingHours <= 0 ||
                scrapingHourOfDay < 0 || scrapingHourOfDay > 23) {
            return ResponseEntity.badRequest().body("Error, invalid values");
        }


        fetchIntervalService.updateSettings(shortHours, longHours, scrapingHours, scrapingHourOfDay);
        dynamicEcEuropaSheduler.rescheduleTasks();
        dynamicScraperScheduler.rescheduleTasks();
        return ResponseEntity.ok("OK, intervals updated.");
    }
    @PostMapping("/set-api-key")
    public ResponseEntity<String> setApiKey(@RequestBody Map<String, String> body) {
        String key = body.get("apiKey");
        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("API key cannot be empty.");
        }
        apiKeyService.updateApiKey(key.trim());
        return ResponseEntity.ok("✅ API key saved.");
    }
}
