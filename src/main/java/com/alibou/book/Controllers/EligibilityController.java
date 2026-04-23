package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityCheckRequest;
import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Services.EligibilityService;
import com.alibou.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EligibilityController {

    private final UserDetailsService userDetailsService;
    private final EligibilityService eligibilityService;
    private final ExamCheckRecordRepository examCheckRecordRepository;

    /**
     * POST /auth/check-eligibility
     * Checks candidate eligibility for university programs and returns a detailed response.
     */



//    CHECK ELEGIBILITY ALL (PUBLIC/PRIVATE SCHOOLS)
    @PostMapping("/check-eligibility")
    public ResponseEntity<EligibilityApiResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {

        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        ExamCheckRecord examRecord = examCheckRecordRepository.findById(request.getCheckRecordId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "ExamCheckRecord not found: " + request.getCheckRecordId()));

        WaecCandidateEntity candidate = buildCandidateFromRequest(request, examRecord);

        EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                candidate,
                request.getUniversityType(),
                String.valueOf(user.getId()),
                request.getCheckRecordId(),
                request.getCategoryIds()
        );

        return ResponseEntity.ok(response);
    }



    // CHECK ELEGIBILITY /PRIVATE SCHOOLS
    @PostMapping("/check-eligibility-private")
    public ResponseEntity<EligibilityApiResponse> checkPrivateEligibility(
            @Valid @RequestBody EligibilityCheckRequest request,
            Principal principal) {

        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        ExamCheckRecord examRecord = examCheckRecordRepository.findById(request.getCheckRecordId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "ExamCheckRecord not found: " + request.getCheckRecordId()));

        WaecCandidateEntity candidate = buildCandidateFromRequest(request, examRecord);

        EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                candidate,
"PRIVATE",
//                request.getUniversityType(),
                String.valueOf(user.getId()),
                request.getCheckRecordId(),
                request.getCategoryIds()
        );

        return ResponseEntity.ok(response);
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
