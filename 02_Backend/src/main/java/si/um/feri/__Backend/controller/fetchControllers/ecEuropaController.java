package si.um.feri.__Backend.controller.fetchControllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.__Backend.service.provider.ecEuropaProvider;
import java.io.IOException;

@RestController
@RequestMapping("/api/listings/ecEuropa")
@CrossOrigin
public class ecEuropaController {
    private final ecEuropaProvider listingService;

    public ecEuropaController(ecEuropaProvider service) {
        this.listingService = service;
    }

    @GetMapping("/fetch/forthcoming")
    public String fetchForthcomingListings() throws IOException {
        listingService.fetchListings("queryForthcoming.json");
        return "Forthcoming listings fetched and saved to MongoDB.";
    }

    @GetMapping("/fetch/open")
    public String fetchOpenListings() throws IOException {
        listingService.fetchListings("queryOpen.json");
        return "Open listings fetched and saved to MongoDB.";
    }

    @GetMapping("/fetch/closed")
    public String fetchClosedListings() throws IOException  {
        listingService.fetchListings("queryClosed.json");
        return "Closed listings fetched and saved to MongoDB.";
    }

    @GetMapping("/fetch/all")
    public String fetchAllListings() throws IOException {
        listingService.fetchListings("queryAll.json");
        return "All listings fetched and saved to MongoDB.";
    }
}
