package com.alibou.book.Controllers;

import com.alibou.book.DTO.AttachTempReportRequest;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Services.GuestAttachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/eligibility")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EligibilityAttachController {

    private final GuestAttachService guestAttachService;

    @PostMapping("/attach-temp-report-to-user")
    public ResponseEntity<EligibilityRecord> attachTempReport(
            @Valid @RequestBody AttachTempReportRequest request,
            Principal principal) {
        EligibilityRecord attached = guestAttachService.attachToUser(request.getSessionId(), principal);
        return ResponseEntity.ok(attached);
    }
}
