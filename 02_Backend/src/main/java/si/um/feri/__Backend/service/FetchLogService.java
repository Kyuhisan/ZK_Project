package si.um.feri.__Backend.service;

import org.springframework.stereotype.Service;
import si.um.feri.__Backend.model.FetchLog;
import si.um.feri.__Backend.repository.FetchLogRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class FetchLogService {
    private final FetchLogRepository repository;

    public FetchLogService(FetchLogRepository repository) {
        this.repository = repository;
    }

    public void logFetch(String source, String type) {
        LocalDateTime localDateTime = ZonedDateTime.now(ZoneId.of("Europe/Ljubljana")).toLocalDateTime();
        FetchLog log = new FetchLog(source, type, localDateTime);
        repository.save(log);
    }
}