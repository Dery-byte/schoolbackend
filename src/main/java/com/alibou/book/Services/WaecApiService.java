package com.alibou.book.Services;

import com.alibou.book.DTO.Candidate;
import com.alibou.book.DTO.CandidateSearchRequest;
import com.alibou.book.DTO.WaecResponse;
import com.alibou.book.DTO.WaecResultsRequest;

import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Repositories.WaecCandidateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class WaecApiService {

    private final RestTemplate waecApiRestTemplate;
    private final ObjectMapper objectMapper;
    private final WaecCandidateRepository waecCandidateRepository;

    @Value("${waec.api.url}")
    private String apiUrl;

    public WaecApiService(
            RestTemplate waecApiRestTemplate,
            ObjectMapper objectMapper,
            WaecCandidateRepository waecCandidateRepository) {
        this.waecApiRestTemplate = waecApiRestTemplate;
        this.objectMapper = objectMapper;
        this.waecCandidateRepository = waecCandidateRepository;
    }

    public ResponseEntity<String> verifyResult(WaecResultsRequest request) {
        // Generate unique reqref
        String reqRef = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        request.setReqref(reqRef);

        try {
            // Log request body for debugging
            System.out.println("Request Body (JSON): " +
                    objectMapper.writeValueAsString(request));

            // Build and send request (headers added by interceptor)
            HttpEntity<WaecResultsRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response =
                    waecApiRestTemplate.postForEntity(apiUrl, entity, String.class);

            System.out.println("WAEC API Response: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            // ✅ Save to database if successful
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WaecResponse waecResponse = objectMapper.readValue(response.getBody(), WaecResponse.class);

                // Map Candidate
                Candidate c = waecResponse.getCandidate();
                WaecCandidateEntity candidateEntity = new WaecCandidateEntity();
                candidateEntity.setCindex(c.getCindex());
                candidateEntity.setCname(c.getCname());
                candidateEntity.setDob(c.getDob());
                candidateEntity.setGender(c.getGender());
                candidateEntity.setExamtype(c.getExamtype());
                candidateEntity.setExamyear(request.getExamyear());

                // Map Result Details
                List<WaecResultDetailEntity> resultEntities = waecResponse.getResultdetails().stream().map(result -> {
                    WaecResultDetailEntity r = new WaecResultDetailEntity();
                    r.setSubjectcode(result.getSubjectcode());
                    r.setSubject(result.getSubject());
                    r.setGrade(result.getGrade());
                    r.setInterpretation(result.getInterpretation());
                    r.setCandidate(candidateEntity);
                    return r;
                }).toList();

                candidateEntity.setResultDetails(resultEntities);
//                candidateEntity.setExamyear(request.getExamyear());

                // Save candidate and results
                waecCandidateRepository.save(candidateEntity);
                System.out.println("✅ WAEC result saved successfully.");
            }

            return response;

        } catch (HttpStatusCodeException e) {
            System.out.println("Exception when calling WAEC API: " + e.getStatusCode());
            System.out.println("Response body: " + e.getResponseBodyAsString());
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());

        } catch (Exception e) {
            System.out.println("General Exception: " + e.getMessage());
            return new ResponseEntity<>("Something went wrong",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //get results based on examtype, examyear,cindex

    public ResponseEntity<WaecCandidateEntity> getCandidateWithResultsFromDb(@RequestBody CandidateSearchRequest request) {
        return waecCandidateRepository.findByCindexAndExamyearAndExamtype(
                request.getCindex(), request.getExamyear(), request.getExamtype()
        ).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
