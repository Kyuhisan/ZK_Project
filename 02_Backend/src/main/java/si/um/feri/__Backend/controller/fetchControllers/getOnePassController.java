package si.um.feri.__Backend.controller.fetchControllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.__Backend.service.provider.getOnePassProvider;
import java.io.IOException;

@RestController
@RequestMapping("/api/listings/getOnePass")
@CrossOrigin
public class getOnePassController {
    private final getOnePassProvider getOnePassProvider;

    public getOnePassController(getOnePassProvider getOnePassProvider) {
        this.getOnePassProvider = getOnePassProvider;
    }

    @GetMapping("/scrape/all")
    public String scrapeAllListings() throws IOException {
        getOnePassProvider.scrapeData();
        return "GetOnePass data scraped and saved to MongoDB.";
    }
}