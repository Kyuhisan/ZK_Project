package si.um.feri.__Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import si.um.feri.__Backend.model.FetchInterval;
import si.um.feri.__Backend.repository.FetchIntervalRepository;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FetchIntervalService {
    private final FetchIntervalRepository intervalRepos;

    public FetchInterval getInterval () {
        return  intervalRepos.findById("singelton").orElseGet(()->{
            FetchInterval fetchInterval = new FetchInterval(24,72,48,3);
            intervalRepos.save(fetchInterval);
            return fetchInterval;
        });
    }

    public void updateSettings(long shortHours,long longHours , long scrapingHours, int scrapingHourOfDay) {
        FetchInterval fetchInterval = getInterval();
        fetchInterval.setShortHours(shortHours);
        fetchInterval.setLongHours(longHours);
        fetchInterval.setScrapeHours(scrapingHours);
        fetchInterval.setTimeOfScrape(scrapingHourOfDay);
        intervalRepos.save(fetchInterval);
    }

    public Duration getShortInterval() {
        return Duration.ofHours(getInterval().getShortHours());
    }

    public Duration getLongInterval() {
        return Duration.ofHours(getInterval().getLongHours());
    }

    public Duration getScrapingInterval() {return Duration.ofHours(getInterval().getScrapeHours());}

    public int getScrapingHourOfDay() {
        return getInterval().getTimeOfScrape();
    }
}
