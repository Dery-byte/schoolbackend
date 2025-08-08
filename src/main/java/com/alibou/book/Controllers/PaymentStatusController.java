package com.alibou.book.Controllers;
import com.alibou.book.DTO.PaymentStatusResponseDTO;
import com.alibou.book.DTO.Projections.DailyPaymentSummary;
import com.alibou.book.DTO.Projections.MonthlyRevenueSummary;
import com.alibou.book.DTO.Projections.WeeklyRevenueSummary;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Services.PaymentStatusService;
import com.alibou.book.user.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auth/payment-status")
public class PaymentStatusController {
    private PaymentStatusService paymentStatusService;
    private final UserDetailsService userDetailsService;

    public PaymentStatusController(PaymentStatusService paymentStatusService, UserDetailsService userDetailsService) {
        this.paymentStatusService = paymentStatusService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping
    public ResponseEntity<PaymentStatuss> createPaymentStatus(@RequestBody PaymentStatuss status) {
        return ResponseEntity.ok(paymentStatusService.save(status));
    }

    @GetMapping("/getAllPayments")
    public ResponseEntity<List<PaymentStatuss>> getAllPaymentStatuses() {
        return ResponseEntity.ok(paymentStatusService.findAll());
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<PaymentStatuss> getById(@PathVariable Long id) {
        return paymentStatusService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/external/{externalRef}")
    public ResponseEntity<PaymentStatuss> getByExternalRef(@PathVariable String externalRef) {
        return paymentStatusService.findByExternalRef(externalRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        paymentStatusService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/total-revenue")
    public ResponseEntity<Double> getTotalRevenue() {
        return ResponseEntity.ok(paymentStatusService.getTotalRevenue());
    }

    @GetMapping("/user")
    public ResponseEntity<Double> getRevenueByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(paymentStatusService.getRevenueByUser(userId));
    }

    @GetMapping("/period")
    public ResponseEntity<Double> getRevenueBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(paymentStatusService.getRevenueBetweenDates(start, end));
    }















    @GetMapping("/forFarmer")
    public ResponseEntity<List<PaymentStatusResponseDTO>> paymentForFarmer(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to fetch orders.");
        }
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        // Assuming service returns a List<PaymentStatuss>
        List<PaymentStatusResponseDTO> payments = paymentStatusService.paymentForFarmer(Long.valueOf(user.getId()));
        return ResponseEntity.ok(payments);
    }









    @GetMapping("/revenueDaily")
    public ResponseEntity<List<DailyPaymentSummary>> getDailyPaymentStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int status) {

        return ResponseEntity.ok(
                paymentStatusService.getDailyPaymentSummary(startDate, endDate, status));
    }

    @GetMapping("/revenueWeekly")
    public ResponseEntity<List<WeeklyRevenueSummary>> getWeeklyRevenue(
            @RequestParam @Min(2000) @Max(9999) int year,
            @RequestParam @Min(1) @Max(12) int month) {

        return ResponseEntity.ok(paymentStatusService.getWeeklyRevenue(year, month));
    }

    @GetMapping("/revenuesDaily")
    public List<DailyPaymentSummary> getDailyPaymentStats(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "1") int statusCode
    ) {
        return paymentStatusService.getDailyPaymentsSummary(
                year,
                month,
                statusCode
        );
    }

    @GetMapping("/revenueMonthly")
    public List<MonthlyRevenueSummary> getMonthlyRevenue(
            @RequestParam int year) {
        return paymentStatusService.getMonthlyRevenue(year);
    }

//    @GetMapping("/revenueMonthly")
//    public ResponseEntity<List<UserMonthlyPaymentSummary>> getMonthlyUserPaymentStats(
//            @RequestParam int year,
//            @RequestParam(defaultValue = "1") int status) {
//
//        return ResponseEntity.ok(
//                paymentStatusService.getUserMonthlyPaymentSummary(year, status));
//    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentStatuss> getPaymentByTransaction(
            @PathVariable long transactionId,
            @RequestParam Long userId) {

        return ResponseEntity.ok(
                paymentStatusService.getPaymentByTransactionAndUser(transactionId, userId));
    }
}
