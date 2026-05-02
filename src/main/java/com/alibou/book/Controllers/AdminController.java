package com.alibou.book.Controllers;

import com.alibou.book.DTO.AdminEmailRequest;
import com.alibou.book.DTO.BiodataWithStatusDTO;
import com.alibou.book.Entity.Biodata;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Repositories.BiodataRepository;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.alibou.book.Services.EligibilityReportService;
import com.alibou.book.email.EmailService;
import com.alibou.book.email.EmailTemplateName;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin-specific operations:
 *  - Download any eligibility report (no ownership check)
 *  - Send a custom email to any user
 *  - Get all biodata with report status
 */
@Slf4j
@RestController
@RequestMapping("/auth/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final EligibilityReportService eligibilityReportService;
    private final EmailService emailService;
    private final BiodataRepository biodataRepository;
    private final EligibilityRecordRepository eligibilityRecordRepository;

    /**
     * Admin report download — no ownership check.
     * GET /auth/admin/reports/{recordId}/download
     */
    @GetMapping("/reports/{recordId}/download")
    public ResponseEntity<byte[]> downloadReportAsAdmin(@PathVariable String recordId) {
        try {
            byte[] pdf = eligibilityReportService.generateReport(recordId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "Eligibility_Report_" + recordId + ".pdf");
            headers.setContentLength(pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Admin report download failed for recordId={}: {}", recordId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Admin custom email sender.
     * POST /auth/admin/send-email
     */
    @PostMapping("/send-email")
    public ResponseEntity<Map<String, String>> sendAdminEmail(
            @RequestBody AdminEmailRequest request) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("subject", request.getSubject());
            vars.put("recipientName", request.getRecipientName() != null ? request.getRecipientName() : "Student");
            vars.put("messageBody", request.getMessageBody());

            emailService.sendEmail(
                    request.getToEmail(),
                    EmailTemplateName.ADMIN_MESSAGE,
                    vars,
                    request.getSubject()
            );
            log.info("Admin email sent to {} with subject '{}'", request.getToEmail(), request.getSubject());
            return ResponseEntity.ok(Map.of("status", "Email sent successfully to " + request.getToEmail()));
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send admin email to {}: {}", request.getToEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Returns all biodata records enriched with whether the student has generated an eligibility report.
     * GET /auth/admin/biodata-with-status
     */
    @GetMapping("/biodata-with-status")
    public ResponseEntity<List<BiodataWithStatusDTO>> getAllBiodataWithStatus() {
        List<Biodata> allBiodata = biodataRepository.findAll();
        List<BiodataWithStatusDTO> result = new ArrayList<>();

        for (Biodata bio : allBiodata) {
            String recordId = bio.getRecord() != null ? bio.getRecord().getId() : null;
            boolean hasReport = false;
            String eligibilityRecordId = null;

            if (recordId != null) {
                Optional<EligibilityRecord> eligibility = eligibilityRecordRepository
                        .findByExamCheckRecord(bio.getRecord());
                if (eligibility.isPresent()) {
                    hasReport = true;
                    eligibilityRecordId = eligibility.get().getId();
                }
            }

            BiodataWithStatusDTO dto = BiodataWithStatusDTO.builder()
                    .id(bio.getId())
                    .firstName(bio.getFirstName())
                    .lastName(bio.getLastName())
                    .email(bio.getEmail())
                    .phoneNumber(bio.getPhoneNumber())
                    .gender(bio.getGender() != null ? bio.getGender().name() : null)
                    .region(bio.getRegion() != null ? bio.getRegion().name() : null)
                    .dob(bio.getDob())
                    .address(bio.getAddress())
                    .recordId(recordId)
                    .hasReport(hasReport)
                    .eligibilityRecordId(eligibilityRecordId)
                    .build();
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }
}
