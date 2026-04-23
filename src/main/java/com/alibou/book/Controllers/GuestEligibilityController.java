package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.DTO.GuestEligibilityCheckRequest;
import com.alibou.book.DTO.GuestSaveTempRequest;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Services.GuestEligibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guest/eligibility")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GuestEligibilityController {

    private final GuestEligibilityService guestEligibilityService;

    @PostMapping("/check")
    public ResponseEntity<EligibilityApiResponse> checkGuestEligibility(
            @Valid @RequestBody GuestEligibilityCheckRequest request) {
        return ResponseEntity.ok(guestEligibilityService.checkEligibility(request));
    }

    @PostMapping("/save-temp")
    public ResponseEntity<EligibilityRecord> saveTempRecord(
            @Valid @RequestBody GuestSaveTempRequest request) {
        return ResponseEntity.ok(guestEligibilityService.saveTempRecord(request));
    }
}
