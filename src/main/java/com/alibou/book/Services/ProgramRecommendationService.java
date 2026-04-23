package com.alibou.book.Services;

import com.alibou.book.DTO.GeminiRequest;
import com.alibou.book.DTO.GeminiResponse;
import com.alibou.book.Entity.*;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ProgramRecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(ProgramRecommendationService.class);

    // Configuration constants
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000L;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;

    // Response length limits
    private static final Map<String, Integer> MAX_LENGTHS = Map.of(
            "careerPath", 2000,
            "jobOpportunities", 2000,
            "futureProspects", 2000,
            "improvementTips", 2000,
            "alternativeOptions", 2000
    );

    @Value("${google.gemini.api.url}")
    private String apiURL;

    @Value("${google.gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Autowired
    public ProgramRecommendationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @PostConstruct
    public void init() {
        // Connection pool configuration
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)  // maximum total connections
                .setMaxConnPerRoute(20) // maximum connections per route
                .build();

        // Timeout configuration
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .setResponseTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();

        // Build HTTP client
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .build();

        // Create request factory
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // Apply to RestTemplate
        restTemplate.setRequestFactory(requestFactory);
    }

    public Map<Program, AIRecommendation> generateRecommendations(
            Map<University, List<Program>> eligibleProgramsMap,
            Map<University, List<Program>> alternativeProgramsMap,
            WaecCandidateEntity candidate) {

        Map<Program, AIRecommendation> recommendations = new HashMap<>();

        processPrograms(eligibleProgramsMap, candidate, true, recommendations);
        processPrograms(alternativeProgramsMap, candidate, false, recommendations);

        return recommendations;
    }

    private void processPrograms(
            Map<University, List<Program>> programsMap,
            WaecCandidateEntity candidate,
            boolean isEligible,
            Map<Program, AIRecommendation> recommendations) {

        programsMap.values().parallelStream()
                .flatMap(List::stream)
                .forEach(program -> {
                    try {
                        AIRecommendation recommendation = buildRecommendation(program, candidate, isEligible);
                        recommendations.put(program, recommendation);
                    } catch (Exception e) {
                        logger.error("Failed to process program {}: {}", program.getName(), e.getMessage());
                        recommendations.put(program, createFallbackRecommendation(program.getName(), isEligible));
                    }
                });
    }

    private AIRecommendation buildRecommendation(Program program,
                                                 WaecCandidateEntity candidate, boolean isEligible) {

        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setProgramName(program.getName());

        recommendation.setCareerPath(
                getTruncatedResponse(() -> generateCareerPath(program, candidate), "careerPath"));

        recommendation.setJobOpportunities(
                getTruncatedResponse(() -> generateJobOpportunities(program), "jobOpportunities"));

        recommendation.setFutureProspects(
                getTruncatedResponse(() -> generateFutureProspects(program), "futureProspects"));

        if (!isEligible) {
//            recommendation.setImprovementTips(
//                    getTruncatedResponse(() -> generateImprovementTips(program, candidate), "improvementTips"));
//            recommendation.setAlternativeOptions(
//                    getTruncatedResponse(() -> generateAlternativeOptions(program), "alternativeOptions"));
        }

        return recommendation;
    }

    private String getTruncatedResponse(Supplier<String> supplier, String fieldName) {
        try {
            return truncateString(supplier.get(), MAX_LENGTHS.get(fieldName));
        } catch (Exception e) {
            logger.warn("Failed to generate {}: {}", fieldName, e.getMessage());
            return fieldName + " information currently unavailable";
        }
    }

    private AIRecommendation createFallbackRecommendation(String programName, boolean isEligible) {
        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setProgramName(programName);
        recommendation.setCareerPath("Career path unavailable");
        recommendation.setJobOpportunities("Job opportunities unavailable");
        recommendation.setFutureProspects("Future prospects unavailable");

        if (!isEligible) {
            recommendation.setImprovementTips("Improvement tips unavailable");
            recommendation.setAlternativeOptions("Alternative options unavailable");
        }
        return recommendation;
    }

    // Gemini API interaction methods
    private String generateCareerPath(Program program, WaecCandidateEntity candidate) {
        String prompt = String.format(
                "Create a 5-year career plan for %s graduates in Ghana with these WAEC results: %s. " +
                        "Include: 1) Yearly milestones 2) Certifications 3) Skills 4) Salary progression 5) Employers. " +
                        "Keep response under %d characters.",
                program.getName(),
                formatResults(candidate.getResultDetails()),
                MAX_LENGTHS.get("careerPath")
        );
        return callGeminiAPI(prompt);
    }



    private String generateFutureProspects(Program program) {
        String prompt = String.format(
                "Analyze the 10-year outlook for %s graduates in Ghana. Consider:\n" +
                        "1. Emerging technologies in this field\n" +
                        "2. Government policies affecting the sector\n" +
                        "3. Economic trends and job market projections\n" +
                        "4. Industry growth potential\n" +
                        "5. Skills that will be in demand\n\n" +
                        "Provide:\n" +
                        "- Optimistic scenario (best-case)\n" +
                        "- Moderate scenario (most likely)\n" +
                        "- Conservative scenario (worst-case)\n\n" +
                        "Format as concise bullet points under each scenario. " +
                        "Keep response under %d characters.",
                program.getName(),
                MAX_LENGTHS.get("futureProspects")
        );

        return callGeminiAPI(prompt);
    }

    private String generateJobOpportunities(Program program) {
        String prompt = String.format(
                "List job opportunities in Ghana for %s graduates. Include: " +
                        "1) Entry-level roles 2) Career progression 3) Key industries 4) Salary ranges. " +
                        "Keep response under %d characters.",
                program.getName(),
                MAX_LENGTHS.get("jobOpportunities")
        );
        return callGeminiAPI(prompt);
    }

    private String callGeminiAPI(String prompt) {
        String fullApiUrl = apiURL + "?key=" + apiKey;
        int attempts = 0;
        Exception lastError = null;

        while (attempts < MAX_RETRIES) {
            try {
                GeminiRequest request = new GeminiRequest(
                        List.of(new GeminiRequest.Content(
                                        List.of(new GeminiRequest.Part(prompt)))
                                )
                        );

                GeminiResponse response = restTemplate.postForObject(
                        fullApiUrl, request, GeminiResponse.class);

                return validateResponse(response);
            } catch (Exception e) {
                lastError = e;
                attempts++;
                logger.warn("Attempt {}/{} failed for prompt ({} chars): {}",
                        attempts, MAX_RETRIES, prompt.length(), e.getMessage());

                sleepBeforeRetry();
            }
        }
        throw new ApiException("Gemini API failed after " + MAX_RETRIES + " attempts", lastError);
    }

    private String validateResponse(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new ApiException("Empty API response");
        }

        return response.getCandidates().get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();
    }

    private void sleepBeforeRetry() {
        try {
            TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("API call interrupted");
        }
    }

    private String truncateString(String input, int maxLength) {
        if (input == null) return null;
        return input.length() > maxLength
                ? input.substring(0, maxLength - 3) + "..."
                : input;
    }

    private String formatResults(List<WaecResultDetailEntity> results) {
        return results.stream()
                .map(r -> r.getSubject() + ": " + r.getGrade())
                .collect(Collectors.joining(", "));
    }

    // Custom exception for better error handling
    private static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}