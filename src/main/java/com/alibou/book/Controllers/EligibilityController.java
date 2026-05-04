package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityCheckRequest;
import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Services.EligibilityService;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class EligibilityController {

    private final UserDetailsService userDetailsService;
    private final EligibilityService eligibilityService;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final UserRepository userRepository;

    /**
     * POST /auth/check-eligibility
     * Checks candidate eligibility for university programs and returns a detailed response.
     */



//    CHECK ELEGIBILITY ALL (PUBLIC/PRIVATE SCHOOLS)
    @PostMapping("/check-eligibility")
    public ResponseEntity<EligibilityApiResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {

        log.info("🚀 Received eligibility check request | userId={} | recordId={} | categories={}",
                principal.getName(), request.getCheckRecordId(), request.getCategoryIds());

        try {
            User user = (User) userDetailsService.loadUserByUsername(principal.getName());

            ExamCheckRecord examRecord = examCheckRecordRepository.findById(request.getCheckRecordId())
                    .orElseThrow(() -> {
                        log.error("❌ ExamCheckRecord not found: {}", request.getCheckRecordId());
                        return new EntityNotFoundException("ExamCheckRecord not found: " + request.getCheckRecordId());
                    });

            WaecCandidateEntity candidate = buildCandidateFromRequest(request, examRecord);

            log.debug("🏃 Calling EligibilityService.checkEligibilityWithDetails...");
            EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                    candidate,
                    request.getUniversityType(),
                    String.valueOf(user.getId()),
                    request.getCheckRecordId(),
                    request.getCategoryIds()
            );

            log.info("✅ Eligibility check successful for user: {}", principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("💥 CRITICAL ERROR in checkEligibility: {}", e.getMessage(), e);
            throw e;
        }
    }



    // CHECK ELEGIBILITY /PRIVATE SCHOOLS
    @PostMapping("/check-eligibility-private")
    public ResponseEntity<EligibilityApiResponse> checkPrivateEligibility(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {

        log.info("🚀 Received PRIVATE eligibility check request | userId={} | recordId={} | categories={}",
                principal.getName(), request.getCheckRecordId(), request.getCategoryIds());

        try {
            User user = (User) userDetailsService.loadUserByUsername(principal.getName());

            ExamCheckRecord examRecord = examCheckRecordRepository.findById(request.getCheckRecordId())
                    .orElseThrow(() -> {
                        log.error("❌ ExamCheckRecord not found: {}", request.getCheckRecordId());
                        return new EntityNotFoundException("ExamCheckRecord not found: " + request.getCheckRecordId());
                    });

            WaecCandidateEntity candidate = buildCandidateFromRequest(request, examRecord);

            log.debug("🏃 Calling EligibilityService.checkEligibilityWithDetails for PRIVATE...");
            EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                    candidate,
                    "PRIVATE",
                    String.valueOf(user.getId()),
                    request.getCheckRecordId(),
                    request.getCategoryIds()
            );

            log.info("✅ PRIVATE Eligibility check successful for user: {}", principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("💥 CRITICAL ERROR in checkPrivateEligibility: {}", e.getMessage(), e);
            throw e;
        }
    }




















    @PostMapping("/discount/validate")
    public ResponseEntity<Map<String, Object>> validateDiscount(
            @RequestBody Map<String, String> request,
            Principal principal) {
        
        String code = request.get("discountCode");
        String subscriptionType = request.get("subscriptionType");
        
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Discount code is required"));
        }
        
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "User not authenticated"));
        }
        
        User currentUser = (User) userDetailsService.loadUserByUsername(principal.getName());
        
        Optional<User> userWithCodeOpt = userRepository.findAll().stream()
                .filter(u -> u.getDiscountCode() != null && code.equalsIgnoreCase(u.getDiscountCode().trim()))
                .findFirst();
                
        if (userWithCodeOpt.isEmpty() || !userWithCodeOpt.get().getId().equals(currentUser.getId())) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid discount code."));
        }
        
        String requiredPackage = currentUser.getDiscountPackage();
        if (requiredPackage != null && !requiredPackage.equalsIgnoreCase(subscriptionType) && !requiredPackage.equalsIgnoreCase("ALL")) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Discount code is not applicable for this package"));
        }
        
        double discountPrice = currentUser.getDiscountPrice() != null ? currentUser.getDiscountPrice() : 5.00;
        
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "discountPrice", discountPrice,
            "message", "Discount applied successfully!"
        ));
    }

    /**
     * Converts the request DTO to a WaecCandidateEntity, using the ExamCheckRecord for the candidate name.
     */
    private WaecCandidateEntity buildCandidateFromRequest(
            EligibilityCheckRequest request,
            ExamCheckRecord examRecord) {

        WaecCandidateEntity candidate = new WaecCandidateEntity();
        candidate.setCname(examRecord.getCandidateName() != null
                ? examRecord.getCandidateName()
                : "Candidate");

        List<WaecResultDetailEntity> resultDetails = request.getResultDetails().stream()
                .map(dto -> {
                    WaecResultDetailEntity detail = new WaecResultDetailEntity();
                    detail.setSubject(dto.getSubject());
                    detail.setGrade(dto.getGrade());
                    detail.setCandidate(candidate);
                    return detail;
                })
                .collect(Collectors.toList());

        candidate.setResultDetails(resultDetails);
        return candidate;
    }
}
