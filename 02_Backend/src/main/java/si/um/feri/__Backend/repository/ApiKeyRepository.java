package si.um.feri.__Backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import si.um.feri.__Backend.model.ApiKeySettings;

public interface ApiKeyRepository extends MongoRepository<ApiKeySettings, String> {

}
