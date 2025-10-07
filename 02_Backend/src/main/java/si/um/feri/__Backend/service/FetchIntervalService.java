package si.um.feri.__Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import si.um.feri.__Backend.model.FetchInterval;
import si.um.feri.__Backend.repository.FetchIntervalRepository;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FetchIntervalService {

    private final FetchIntervalRepository fetchIntervalRepository;

    public FetchInterval getInterval () {
        return  fetchIntervalRepository.findById("singelton").orElseGet(()->{
            FetchInterval fetchInterval = new FetchInterval(24,72,48,3);
            fetchIntervalRepository.save(fetchInterval);
            return fetchInterval;
        });
    }

    public void updateSettings(long shortHours,long longHours , long scrapingHours, int scrapingHourOfDay) {
        FetchInterval fetchInterval = getInterval();
        fetchInterval.setShortIntervalHours(shortHours);
        fetchInterval.setLongIntervalHours(longHours);
        fetchInterval.setScrapingIntervalHours(scrapingHours);
        fetchInterval.setScrapingHourOfDay(scrapingHourOfDay);
        fetchIntervalRepository.save(fetchInterval);
    }

    public Duration getShortInterval() {
        return Duration.ofHours(getInterval().getShortIntervalHours());
    }

    public Duration getLongInterval() {
        return Duration.ofHours(getInterval().getLongIntervalHours());
    }

    public Duration getScrapingInterval() {return Duration.ofHours(getInterval().getScrapingIntervalHours());}

    public int getScrapingHourOfDay() {
        return getInterval().getScrapingHourOfDay();
    }
}
