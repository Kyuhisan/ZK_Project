package si.um.feri.__Backend.controller.fetchControllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.um.feri.__Backend.service.provider.cascadeProvider;
import java.io.IOException;

@RestController
@RequestMapping("/api/listings/cascadeFunding")
@CrossOrigin
public class cascadeFundingController {
  private final cascadeProvider cascadeProvider;

  public cascadeFundingController(cascadeProvider cascadeProvider) {
    this.cascadeProvider = cascadeProvider;
  }

  @GetMapping("/scrape/all")
  public String scrapeAllListings() throws IOException {
    cascadeProvider.scrapeData();
    return "CascadeFunding data scraped and saved to MongoDB.";
  }
}