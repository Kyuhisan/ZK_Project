package si.um.feri.__Backend.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.um.feri.__Backend.service.FetchIntervalService;
import si.um.feri.__Backend.service.FetchLogService;
import si.um.feri.__Backend.service.provider.ecEuropaProvider;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DynamicEcEuropaSheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicEcEuropaSheduler.class);

    private final FetchIntervalService fetchIntervalService;
    private final FetchLogService fetchLogService;
    private final ecEuropaProvider listingService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private ScheduledFuture<?> openListing;
    private ScheduledFuture<?> forthcomingListing;
    private ScheduledFuture<?> closedListing;

    @PostConstruct
    public void init() {
        scheduleTasks();
    }

    private void scheduleTasks() {
        Duration shortInterval = fetchIntervalService.getShortInterval();
        Duration longInterval = fetchIntervalService.getLongInterval();
        int scrapingHour = fetchIntervalService.getScrapingHourOfDay();
        long initialDelayShort = computeInitialDelay(scrapingHour);
        long initialDelayLong = initialDelayShort;

        openListing = scheduler.scheduleWithFixedDelay(() -> {
            try {
                listingService.fetchListings("queryOpen.json");
                fetchLogService.logFetch("ecEuropa", "Open");
            } catch (IOException e) {
                log.error("Failed to fetch 'Open' listings from ecEuropa", e);
            }
        }, initialDelayShort, shortInterval.toSeconds(), TimeUnit.SECONDS);

        forthcomingListing = scheduler.scheduleWithFixedDelay(() -> {
            try {
                listingService.fetchListings("queryForthcoming.json");
                fetchLogService.logFetch("ecEuropa", "Forthcoming");
            } catch (IOException e) {
                log.error("Failed to fetch 'Forthcoming' listings from ecEuropa", e);
            }
        }, initialDelayShort, shortInterval.toSeconds(), TimeUnit.SECONDS);

        closedListing = scheduler.scheduleWithFixedDelay(() -> {
            try {
                listingService.fetchListings("queryClosed.json");
                fetchLogService.logFetch("ecEuropa", "Closed");
            } catch (IOException e) {
                log.error("Failed to fetch 'Closed' listings from ecEuropa", e);
            }
        }, initialDelayLong, longInterval.toSeconds(), TimeUnit.SECONDS);

        log.info("Scheduled ecEuropa fetch: open/forthcoming starts in {}s, closed in {}s",
                initialDelayShort, initialDelayLong);
    }

    public void rescheduleTasks() {
        if (openListing != null) openListing.cancel(false);
        if (forthcomingListing != null) forthcomingListing.cancel(false);
        if (closedListing != null) closedListing.cancel(false);

        scheduleTasks();
    }

    private long computeInitialDelay(int targetHour) {
        if (targetHour < 0 || targetHour > 23) {
            log.warn("Invalid hour {}, defaulting to 3 AM", targetHour);
            targetHour = 3;
        }

        ZoneId ljubljanaZone = ZoneId.of("Europe/Ljubljana");
        LocalDateTime now = LocalDateTime.now(ljubljanaZone);
        LocalDateTime nextRun = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();
    }
}
