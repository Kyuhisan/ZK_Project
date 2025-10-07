package si.um.feri.__Backend.model.rawListings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ecEuropaRaw {
    private Metadata metadata;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private List<String> identifier;
        private List<String> title;
        private List<String> deadlineDate;
        private List<String> status;
        private List<String> budget;
        private List<String> budgetOverview;
        private List<String> startDate;
        private List<String> deadlineModel;
        private List<String> descriptionByte;           //  summary
        private List<String> crossCuttingPriorities;    //  technologies
        private List<String> tags;                      //  keywords
        private List<String> typesOfAction;             //  industries
    }
}

