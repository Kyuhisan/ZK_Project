package si.um.feri.__Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final ApiKeyService apiKeyService;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", "mistralai/mistral-7b-instruct:free",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant for keyword classification."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );
    }

    private String loadAllKeywordsFromFiles(List<String> filePaths) throws IOException {
        Set<String> keywords = new LinkedHashSet<>();

        for (String path : filePaths) {
            List<String> lines = Files.readAllLines(Paths.get(path));
            for (String line : lines) {
                String cleaned = line.trim();
                if (!cleaned.isEmpty()) {
                    keywords.add(cleaned);
                }
            }
        }
        return keywords.stream()
                .map(k -> "- " + k)
                .collect(Collectors.joining("\n"));
    }

    @Value("${app.output.keywords-path}")
    private String keywordsPath;

    public Mono<String> extractKeywords(String userInput) {
        String formattedKeywordList;
        try {
            String baseDir = System.getProperty("user.dir") + keywordsPath;
            formattedKeywordList = loadAllKeywordsFromFiles(List.of(
                    baseDir + "cascadeFunding/keywords.txt",
                    baseDir + "getOnePass/keywords.txt",
                    baseDir + "generic/keywords.txt",
                    baseDir + "ecEuropa/keywords.txt"
            ));
        } catch (IOException e) {
            return Mono.error(new RuntimeException("Failed to load keywords from files", e));
        }

        String prompt = """
        You are a keyword extractor.

        Given a user input and a list of available keywords, select ONLY between 5 and 10 relevant keywords based on thematic or semantic similarity.

        Rules:
        - You may invent up to 2 keywords if necessary.
        - No duplicates.
        - All keywords must be in english language!
        - Return only a JSON array of strings.

        Format:
        ["keyword1", "keyword2", ..., "keywordN"]

        Available Keywords:
        """ + formattedKeywordList + "\n\nUser Input:\n" + userInput;

        Map<String, Object> requestBody = buildRequestBody(prompt);

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKeyService.getCurrentApiKey())
                .header("HTTP-Referer", "http://localhost:5173")
                .header("X-Title", "HECF-SmartSearch")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    System.out.println("Raw response: " + response);

                    Object choicesObj = response.get("choices");
                    if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                        Object messageObj = ((Map<?, ?>) choices.get(0)).get("message");
                        if (messageObj instanceof Map<?, ?> messageMap) {
                            Object content = messageMap.get("content");
                            if (content != null) return content.toString().trim();
                        }
                    }
                    return "[]";
                })
                .onErrorResume(e -> {
                    System.err.println("OpenRouter call failed: " + e.getMessage());
                    return Mono.just("[]");
                });
    }
}