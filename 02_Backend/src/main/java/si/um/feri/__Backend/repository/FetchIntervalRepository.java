package si.um.feri.__Backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import si.um.feri.__Backend.model.FetchInterval;

public interface FetchIntervalRepository  extends MongoRepository<FetchInterval, String> {
}
