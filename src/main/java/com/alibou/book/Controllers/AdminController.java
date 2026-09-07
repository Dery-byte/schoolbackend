package com.alibou.book.Controllers;

import com.alibou.book.DTO.AdminEmailRequest;
import com.alibou.book.DTO.BiodataWithStatusDTO;
import com.alibou.book.Entity.Biodata;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Repositories.BiodataRepository;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.alibou.book.Services.EligibilityReportService;
import com.alibou.book.Services.SystemSettingService;
import com.alibou.book.config.PaystackConfig;
import com.alibou.book.email.EmailService;
import com.alibou.book.email.EmailTemplateName;
import com.alibou.book.Services.MNotifyV2SmsService;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private final SystemSettingService systemSettingService;
    private final UserRepository userRepository;
    private final MNotifyV2SmsService smsService;
    private final PaystackConfig paystackConfig;

    @org.springframework.beans.factory.annotation.Value("${application.mailing.frontend.baseUrl}")
    private String frontendUrl;

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

    /**
     * Admin sets the global eligibility check settings.
     */
    @PostMapping("/settings/threshold")
    public ResponseEntity<Map<String, String>> setThreshold(@RequestBody Map<String, Object> request) {
        if (request.containsKey("threshold")) {
            systemSettingService.updateSetting("ELIGIBILITY_CHECK_THRESHOLD", String.valueOf(request.get("threshold")));
        }
        if (request.containsKey("discountMode")) {
            systemSettingService.updateSetting("DISCOUNT_MODE", String.valueOf(request.get("discountMode")));
        }
        return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
    }

    /**
     * Admin gets the current global eligibility check settings.
     */
    @GetMapping("/settings/threshold")
    public ResponseEntity<Map<String, Object>> getThreshold() {
        String thresholdStr = systemSettingService.getSetting("ELIGIBILITY_CHECK_THRESHOLD", "3");
        String discountMode = systemSettingService.getSetting("DISCOUNT_MODE", "MANUAL");
        
        Map<String, Object> response = new HashMap<>();
        response.put("threshold", Integer.parseInt(thresholdStr));
        response.put("discountMode", discountMode);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Admin assigns a discount code to a user manually.
     */
    @PostMapping("/users/{userId}/discount")
    public ResponseEntity<Map<String, String>> assignDiscountCode(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String code = request.get("discountCode");
            String pkg = request.get("discountPackage");
            String priceStr = request.get("discountPrice");
            
            String mode = request.get("discountMode");
            String thresholdStr = request.get("discountThreshold");
            
            if (pkg == null || pkg.trim().isEmpty()) {
                pkg = "PREMIUM"; // Default
            }
            double price = 5.00;
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    // ignore and use default 5.00
                }
            }
            
            boolean sendSms = false;
            if (request.containsKey("sendSms")) {
                sendSms = Boolean.parseBoolean(String.valueOf(request.get("sendSms")));
            }
            
            if ("AUTOMATIC".equalsIgnoreCase(mode)) {
                user.setDiscountGenerationMode("AUTOMATIC");
                if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
                    try {
                        user.setDiscountCheckThreshold(Integer.parseInt(thresholdStr));
                    } catch (NumberFormatException e) {
                        user.setDiscountCheckThreshold(3); // Default
                    }
                } else {
                    user.setDiscountCheckThreshold(3);
                }
                user.setDiscountCode(null); // Clear code until threshold is met
                user.setDiscountPackage(pkg);
                user.setDiscountPrice(price);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Automatic discount generation enabled for user", "discountMode", "AUTOMATIC", "discountPackage", pkg, "discountPrice", String.valueOf(price)));
            } else {
                if (code == null || code.trim().isEmpty()) {
                    code = "DISC-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                }
                user.setDiscountGenerationMode("MANUAL");
                user.setDiscountCode(code);
                user.setDiscountPackage(pkg);
                user.setDiscountPrice(price);
                
                // Track history
                user.getHistoricalDiscountAmounts().add(price);
                
                userRepository.save(user);
                
                // Send Email Notification
                boolean sendEmail = true; // default true for backwards compatibility
                if (request.containsKey("sendEmail")) {
                    sendEmail = Boolean.parseBoolean(String.valueOf(request.get("sendEmail")));
                }
                
                if (sendEmail) {
                    try {
                        Map<String, Object> vars = new HashMap<>();
                        vars.put("homepageUrl", frontendUrl);
                        vars.put("discountCode", code);
                        vars.put("discountPackage", pkg);
                        vars.put("discountPrice", String.valueOf(price));
                        vars.put("username", user.getFirstname());
                        
                        emailService.sendEmail(
                                user.getUsername(),
                                EmailTemplateName.DISCOUNT_ASSIGNED,
                                vars,
                                "You've received a special discount!"
                        );
                    } catch (Exception e) {
                        log.error("Failed to send discount email to {}: {}", user.getUsername(), e.getMessage());
                    }
                }
                
                // Send SMS Notification
                if (sendSms && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                    String message = "Hello " + user.getFirstname() + ", you've received a discount code: " + code + ". Visit " + frontendUrl + " to use it!";
                    smsService.sendSms(List.of(user.getPhoneNumber()), message);
                }
                
                return ResponseEntity.ok(Map.of("message", "Discount code assigned successfully", "discountCode", code, "discountPackage", pkg, "discountPrice", String.valueOf(price)));
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    /**
     * Admin revokes a discount code from a user.
     */
    @DeleteMapping("/users/{userId}/discount")
    public ResponseEntity<Map<String, String>> revokeDiscountCode(@PathVariable Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setDiscountCode(null);
            user.setDiscountPackage(null);
            user.setDiscountPrice(null);
            user.setDiscountGenerationMode("MANUAL");
            user.setDiscountCheckThreshold(null);
            user.setChecksSinceLastDiscount(0);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Discount settings and code revoked successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    // ──────────────────────────────────────────────────────────
    //  GLOBAL DISCOUNT BANNER  (stored in system_settings table)
    // ──────────────────────────────────────────────────────────

    /**
     * Public endpoint — the login/register page fetches this
     * to decide whether to show the animated promotion banner.
     * GET /auth/admin/settings/global-discount
     */
    @GetMapping("/settings/global-discount")
    public ResponseEntity<Map<String, Object>> getGlobalDiscount() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled",     Boolean.parseBoolean(
                systemSettingService.getSetting("GLOBAL_DISCOUNT_ENABLED", "false")));
        result.put("percentage",  Integer.parseInt(
                systemSettingService.getSetting("GLOBAL_DISCOUNT_PERCENTAGE", "0")));
        result.put("message",     systemSettingService.getSetting("GLOBAL_DISCOUNT_MESSAGE", ""));
        result.put("expiryDate",  systemSettingService.getSetting("GLOBAL_DISCOUNT_EXPIRY", null));
        result.put("setAt",       systemSettingService.getSetting("GLOBAL_DISCOUNT_SET_AT", null));
        
        // Global Promo fields
        result.put("promoCode", systemSettingService.getSetting("GLOBAL_PROMO_CODE", ""));
        result.put("promoPackage", systemSettingService.getSetting("GLOBAL_PROMO_PACKAGE", "ALL"));
        String priceStr = systemSettingService.getSetting("GLOBAL_PROMO_DISCOUNTED_PRICE", "0");
        try {
            result.put("promoDiscountedPrice", Double.parseDouble(priceStr));
        } catch (NumberFormatException e) {
            result.put("promoDiscountedPrice", 0.0);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Admin saves (enables) the global discount banner.
     * POST /auth/admin/settings/global-discount
     * Body: { enabled, percentage, message, expiryDate }
     */
    @PostMapping("/settings/global-discount")
    public ResponseEntity<Map<String, String>> saveGlobalDiscount(
            @RequestBody Map<String, Object> request) {
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_ENABLED",
                String.valueOf(request.getOrDefault("enabled", "false")));
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_PERCENTAGE",
                String.valueOf(request.getOrDefault("percentage", "0")));
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_MESSAGE",
                String.valueOf(request.getOrDefault("message", "")));
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_EXPIRY",
                String.valueOf(request.getOrDefault("expiryDate", "")));
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_SET_AT",
                Instant.now().toString());

        systemSettingService.updateSetting("GLOBAL_PROMO_CODE", String.valueOf(request.getOrDefault("promoCode", "")));
        systemSettingService.updateSetting("GLOBAL_PROMO_PACKAGE", String.valueOf(request.getOrDefault("promoPackage", "ALL")));
        systemSettingService.updateSetting("GLOBAL_PROMO_DISCOUNTED_PRICE", String.valueOf(request.getOrDefault("promoDiscountedPrice", "0")));

        log.info("Global discount banner saved: enabled={}, percentage={}",
                request.get("enabled"), request.get("percentage"));
        return ResponseEntity.ok(Map.of("message", "Global discount banner saved successfully"));
    }

    /**
     * Admin clears / disables the global discount banner.
     * DELETE /auth/admin/settings/global-discount
     */
    @DeleteMapping("/settings/global-discount")
    public ResponseEntity<Map<String, String>> clearGlobalDiscount() {
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_ENABLED", "false");
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_PERCENTAGE", "0");
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_MESSAGE", "");
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_EXPIRY", "");
        systemSettingService.updateSetting("GLOBAL_DISCOUNT_SET_AT", "");
        
        systemSettingService.updateSetting("GLOBAL_PROMO_CODE", "");
        systemSettingService.updateSetting("GLOBAL_PROMO_PACKAGE", "ALL");
        systemSettingService.updateSetting("GLOBAL_PROMO_DISCOUNTED_PRICE", "0");

        return ResponseEntity.ok(Map.of("message", "Global discount cleared successfully"));
    }

    // ─────────────────────────────────────────────────────────
    //  PAYMENT GATEWAY TOGGLE
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the name of the currently active payment gateway.
     * GET /auth/admin/settings/payment-gateway
     */
    @GetMapping("/settings/payment-gateway")
    public ResponseEntity<Map<String, String>> getActivePaymentGateway() {
        String gateway = systemSettingService.getSetting("ACTIVE_PAYMENT_GATEWAY", "MOOLRE");
        return ResponseEntity.ok(Map.of("gateway", gateway.toUpperCase()));
    }

    /**
     * Switches the active payment gateway.
     * POST /auth/admin/settings/payment-gateway
     * Body: { "gateway": "PAYSTACK" }  or  { "gateway": "MOOLRE" }
     */
    @PostMapping("/settings/payment-gateway")
    public ResponseEntity<Map<String, String>> setActivePaymentGateway(
            @RequestBody Map<String, String> request) {
        String gateway = request.getOrDefault("gateway", "MOOLRE").toUpperCase();
        if (!"MOOLRE".equals(gateway) && !"PAYSTACK".equals(gateway)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid gateway. Allowed values: MOOLRE, PAYSTACK"));
        }
        systemSettingService.updateSetting("ACTIVE_PAYMENT_GATEWAY", gateway);
        log.info("Admin switched active payment gateway to: {}", gateway);
        return ResponseEntity.ok(Map.of("message", "Active payment gateway set to " + gateway, "gateway", gateway));
    }

    /**
     * Returns the Paystack public key so the Angular frontend can initialise the Paystack popup.
     * This does NOT expose the secret key.
     * GET /auth/admin/settings/paystack-public-key
     */
    @GetMapping("/settings/paystack-public-key")
    public ResponseEntity<Map<String, String>> getPaystackPublicKey() {
        String pubKey = paystackConfig.getPublicKey() != null ? paystackConfig.getPublicKey() : "";
        return ResponseEntity.ok(Map.of("publicKey", pubKey));
    }

    // ── helpers ───────────────────────────────────────────────
    private int parseIntSafe(String value, int defaultVal) {
        try { return Integer.parseInt(value); } catch (Exception e) { return defaultVal; }
    }

    /**
     * Admin activates or deactivates a user.
     */
    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<Map<String, String>> toggleUserStatus(@PathVariable Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            String status = user.isEnabled() ? "Activated" : "Deactivated";
            return ResponseEntity.ok(Map.of("message", "User successfully " + status, "enabled", String.valueOf(user.isEnabled())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }
}
