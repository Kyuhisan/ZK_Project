package si.um.feri.__Backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import si.um.feri.__Backend.model.FetchLog;

import java.util.List;

public interface FetchLogRepository extends MongoRepository<FetchLog, String> {
    List<FetchLog> findTop20ByOrderByTimeOfFetchDesc();
    FetchLog findTop1BySourceOrderByTimeOfFetchDesc(String source);
}
