package si.um.feri.__Backend.model.rawListings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class getOnePassRaw {
    private String id;
    private String status;
    private String source;
    private String url;
    private String title;
    private String summary;
    private String deadlineDate;
    private List<String> industry;
    private String description;
    private List<String> technology;
}
