package si.um.feri.__Backend.model.rawListings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class cascadeFundingRaw {
    private String id;
    private String status;
    private String source;
    private String url;
    private String title;
    private String summary;
    private String deadlineDate;
    private String maxFunding;
}
