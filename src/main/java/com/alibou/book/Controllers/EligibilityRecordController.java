package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityMonthlySummary;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Services.EligibilityRecordService;
import com.alibou.book.Services.EligibilityReportService;
import com.alibou.book.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/auth/eligibilityRecords")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EligibilityRecordController {

    private final EligibilityRecordService eligibilityRecordService;
    private final EligibilityReportService eligibilityReportService;
    private final UserDetailsService userDetailsService;

    @GetMapping("/all")
    public ResponseEntity<List<EligibilityRecord>> getAllEligibilityRecords() {
        return ResponseEntity.ok(eligibilityRecordService.getAllRecords());
    }

    @GetMapping("/my-eligibility-records")
    public ResponseEntity<List<EligibilityRecord>> getEligibilityRecordsForUser(Principal principal) {
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        String userId = String.valueOf(user.getId());
        List<EligibilityRecord> records = eligibilityRecordService.getRecordsByUser(userId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalRecordsCount() {
        return ResponseEntity.ok(eligibilityRecordService.getTotalEligibilityRecords());
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getRecordsCountByUser(@PathVariable String userId) {
        return ResponseEntity.ok(eligibilityRecordService.getEligibilityRecordsCountByUser(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EligibilityRecord>> getRecordsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(eligibilityRecordService.getEligibilityRecordsByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<EligibilityRecord>> getAllRecords() {
        return ResponseEntity.ok(eligibilityRecordService.getAllEligibilityRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EligibilityRecord> getRecordById(@PathVariable String id) {
        return ResponseEntity.ok(eligibilityRecordService.getEligibilityRecordById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable String id) {
        eligibilityRecordService.deleteEligibilityRecord(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/monthlyStats")
    public List<EligibilityMonthlySummary> getMonthlyStats(@RequestParam int year) {
        return eligibilityRecordService.getMonthlyStats(year);
    }

    /**
     * GET /auth/eligibilityRecords/{id}/report
     * Generates and returns a PDF eligibility report for the given record.
     * Only the owning user may download their own report.
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String id,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        EligibilityRecord record = eligibilityRecordService.getEligibilityRecordById(id);

        if (!record.getUserId().equals(String.valueOf(user.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdf = eligibilityReportService.generateReport(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "Eligibility_Report_" + id + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
