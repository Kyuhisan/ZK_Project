package si.um.feri.__Backend.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.um.feri.__Backend.service.FetchIntervalService;
import si.um.feri.__Backend.service.FetchLogService;
import si.um.feri.__Backend.service.provider.cascadeFundingProvider;
import si.um.feri.__Backend.service.provider.getOnePassProvider;

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
public class DynamicScraperScheduler {
    private final FetchIntervalService fetchIntervalService;
    private final FetchLogService fetchLogService;
    private final getOnePassProvider getOnePassProvider;
    private final cascadeFundingProvider cascadeFundingProvider;

    private final ScheduledExecutorService scheduler =  Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> scrapingTask;
    private static final Logger log = LoggerFactory.getLogger(DynamicScraperScheduler.class);

    @PostConstruct
    public void init(){
        scheduleTasks();
    }

    public void scheduleTasks() {
        int scrapingHour = fetchIntervalService.getScrapingHourOfDay();
        long initialDelay = computeInitialDelay(scrapingHour);
        long interval = fetchIntervalService.getScrapingInterval().toSeconds();



        scrapingTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                cascadeFundingProvider.scrapeData();
                fetchLogService.logFetch("cascadeFunding", "All statuses");
            } catch (IOException e) {
                log.error("Failed to scrape cascadeFunding data", e);
            }

            try {
                getOnePassProvider.scrapeData();
                fetchLogService.logFetch("getOnePass", "All statuses");
            } catch (IOException e) {
                log.error("Failed to scrape getOnePass data", e);
            }
        }, initialDelay, interval, TimeUnit.SECONDS);

        log.info("Scheduled funding scrape every {} seconds", interval);
    }

    public void rescheduleTasks() {
        if (scrapingTask != null) scrapingTask.cancel(false);
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
