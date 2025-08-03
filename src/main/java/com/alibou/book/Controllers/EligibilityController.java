package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityRequest;
import com.alibou.book.DTO.UniversityEligibilityDTO;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.WaecCandidateEntity;
//import com.alibou.book.Services.EligibilityCheckerService;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Services.WaecApiService;
import com.alibou.book.user.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins="*")
public class EligibilityController {

    @Autowired
    private WaecApiService waecApiService;
    private final UserDetailsService userDetailsService;

    public EligibilityController(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
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


    @PostMapping("/check-eligibility")
    public ResponseEntity<EligibilityRecord> checkEligibility(
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

}
