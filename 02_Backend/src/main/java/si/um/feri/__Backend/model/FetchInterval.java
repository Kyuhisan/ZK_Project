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
    private long shortIntervalHours;
    private long longIntervalHours;
    private long scrapingIntervalHours;
    private int scrapingHourOfDay;

    public FetchInterval() {}

    public FetchInterval(long shortIntervalHours,long longIntervalHours , long scrapingIntervalHours, int scrapingHourOfDay) {
        this.shortIntervalHours = shortIntervalHours;
        this.longIntervalHours = longIntervalHours;
        this.scrapingIntervalHours = scrapingIntervalHours;
        this.scrapingHourOfDay = scrapingHourOfDay;
    }

}
