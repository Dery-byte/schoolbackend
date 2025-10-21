package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityCheckRequest;
import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.DTO.EligibilityRequest;
import com.alibou.book.DTO.ResultDetail;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Entity.WaecCandidateEntity;
//import com.alibou.book.Services.EligibilityCheckerService;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Services.EligibilityService;
import com.alibou.book.Services.WaecApiService;
import com.alibou.book.user.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins="*")
public class EligibilityController {

    @Autowired
    private WaecApiService waecApiService;
    private final UserDetailsService userDetailsService;

    private final EligibilityService eligibilityService;

    public EligibilityController(UserDetailsService userDetailsService, EligibilityService eligibilityService) {
        this.userDetailsService = userDetailsService;
        this.eligibilityService = eligibilityService;
    }


//    @PostMapping("/check-eligibilityAll")
//    public ResponseEntity<List<UniversityEligibilityDTO>> checkEligibility(@RequestBody WaecCandidateEntity candidate) {
//        List<UniversityEligibilityDTO> eligibility = waecApiService.checkEligibility(candidate, null);
//        return ResponseEntity.ok(eligibility);
//    }


// @PostMapping("/check-eligibilityAll")
//    public ResponseEntity<EligibilityRecord> checkEligibility(@RequestBody WaecCandidateEntity candidate,
//                                                              Principal principal) {
//     User user = (User) userDetailsService.loadUserByUsername(principal.getName());
//     String userId = String.valueOf(user.getId());
//     EligibilityRecord eligibility = waecApiService.checkEligibility(candidate, null, userId);
//        return ResponseEntity.ok(eligibility);
//    }


//    @PostMapping("/check-eligibility/{universityType}")
//    public ResponseEntity<List<UniversityEligibilityDTO>> checkEligibility(
//            @PathVariable String universityType,
//            @RequestBody WaecCandidateEntity candidate) {
//
//        return ResponseEntity.ok(waecApiService.checkEligibility(candidate, universityType));
//    }


    @PostMapping("/checks-eligibility")
    public ResponseEntity<EligibilityRecord> checkEligibilityBasic(
            @RequestBody @Valid EligibilityRequest request,
            Principal principal) {

        // 1. Get authenticated user
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        // 2. Create candidate entity from request
        WaecCandidateEntity candidate = new WaecCandidateEntity();
        candidate.setResultDetails(request.getResultDetails().stream()
                .map(dto -> {
                    WaecResultDetailEntity detail = new WaecResultDetailEntity();
                    detail.setSubject(dto.getSubject());
                    detail.setGrade(dto.getGrade());
                    return detail;
                })
                .collect(Collectors.toList()));

        // 3. Call service
        EligibilityRecord record = waecApiService.checkEligibility(
                candidate,
                request.getUniversityType(),
                String.valueOf(user.getId()),
                request.getCheckRecordId(),
                request.getCategoryIds()
        );

        return ResponseEntity.ok(record);
    }


    /**
     * POST /api/v1/eligibility/check
     * Checks candidate eligibility for university programs
     */


    @PostMapping("/check-eligibility")
    public ResponseEntity<EligibilityApiResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {

        System.out.println("📋 Eligibili" + request.getCheckRecordId());
        // Convert request DTO to WaecCandidateEntity
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        WaecCandidateEntity candidate = buildCandidateFromRequest(request);

        // Call service
        EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                candidate,
                request.getUniversityType(),
                String.valueOf(user.getId()),
                request.getCheckRecordId(),
                request.getCategoryIds()
        );

        System.out.println("✅ Eligibility check completed. Found {} universities with programs  " + response.getSummary().getTotalUniversities());

        return ResponseEntity.ok(response);
    }

    /**
     * Converts the request DTO to WaecCandidateEntity
     */
    private WaecCandidateEntity buildCandidateFromRequest(EligibilityCheckRequest request) {
        WaecCandidateEntity candidate = new WaecCandidateEntity();

        // Generate a temporary ID if not provided
//        candidate.setId(Long.valueOf(UUID.randomUUID().toString()));

        // Set candidate name (you might want to get this from authenticated user)
        candidate.setCname("Candidate");

        // Convert ResultDetailDto list to WaecResultDetail entities
        // ✅ CORRECT:
        List<WaecResultDetailEntity> resultDetails = request.getResultDetails().stream()
                .map(dto -> {
                    WaecResultDetailEntity detail = new WaecResultDetailEntity();
                    detail.setSubject(dto.getSubject());
                    detail.setGrade(dto.getGrade());
                    detail.setCandidate(candidate);
                    return detail;
                })
                .collect(Collectors.toList());

        candidate.setResultDetails(resultDetails); // No casting, no wrapping!


        return candidate;
    }


    @PostMapping("/checks")
    public ResponseEntity<EligibilityRecord> checkEligibilityBasic(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        WaecCandidateEntity candidate = buildCandidateFromRequest(request);
        // Call service
        EligibilityRecord response = eligibilityService.checkEligibility(
                candidate,
                request.getUniversityType(),
                String.valueOf(user.getId()),
                request.getCheckRecordId(),
                request.getCategoryIds()
        );
        return ResponseEntity.ok(response);
    }







// Get a single record by ID
@GetMapping("/{id}")
public ResponseEntity<EligibilityRecord> getEligibilityRecordById(@PathVariable String id) {
    try {
        EligibilityRecord record = eligibilityService.getEligibilityRecordById(id);
        return ResponseEntity.ok(record);
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build();
    }

}



}








//    @PostMapping("/check")
//    public ResponseEntity<EligibilityRecord> checkEligibilityDetailed(
//            @RequestBody EligibilityCheckRequest request,
//            @RequestHeader("User-Id") String userId) {
//
////        log.info("Eligibility check requested by user: {}", userId);
//
//        EligibilityRecord response = eligibilityService.checkEligibility(
//                request.getCandidate(),
//                request.getUniversityType(),
//                userId,
//                request.getCheckExamRecordId(),
//                request.getCategoryIds()
//        );
//
//        return ResponseEntity.ok(response);
//    }
//
//






