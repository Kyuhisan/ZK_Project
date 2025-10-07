package si.um.feri.__Backend.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Data
@Document("key_settings")
public class ApiKeySettings {
@Id
private String id = "singleton";
private String encryptedApiKey;

}
