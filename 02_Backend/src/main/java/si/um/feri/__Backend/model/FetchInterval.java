package si.um.feri.__Backend.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Getter
@Setter
@Data
@Document("fetch_interval")
public class FetchInterval {
    @Id
    private String id = "singelton";
    private long shortHours;
    private long longHours;
    private long scrapeHours;
    private int timeOfScrape;

    public FetchInterval() {}

    public FetchInterval(long shortHours,long longHours , long scrapeHours, int timeOfScrape) {
        this.shortHours = shortHours;
        this.longHours = longHours;
        this.scrapeHours = scrapeHours;
        this.timeOfScrape = timeOfScrape;
    }

}
