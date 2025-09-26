package com.alibou.book.Controllers;


import com.alibou.book.DTO.ExamCheckRecordDTO;
import com.alibou.book.DTO.ExamRecordStatsDTO;
import com.alibou.book.DTO.Projections.ExamCheckMonthlySummary;
import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Services.ExamCheckRecordService;
import com.alibou.book.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/records")
@CrossOrigin(origins="*")
@RequiredArgsConstructor
public class ExamCheckRecordController {
    private final ExamCheckRecordService examCheckRecordService;
    private final UserDetailsService userDetailsService;
    private final ExamCheckRecordRepository examCheckRecordRepository;


    // Step 1: Create initial record (minimal data)

    @PostMapping("/createCheckRecords")
    public ResponseEntity<ExamCheckRecord> create(@RequestBody ExamCheckRecord record, Principal principal) {
        ExamCheckRecord saved = examCheckRecordService.startCheck(record, principal);
        return ResponseEntity.ok(saved);
    }

    // Step 2: Update payment status
    @PatchMapping("/{id}/paymentStatus")
    public ResponseEntity<ExamCheckRecord> updatePaymentStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> paymentStatusUpdate) {
        String newPaymentStatus = paymentStatusUpdate.get("paymentStatus");
        ExamCheckRecord updated = examCheckRecordService.updatePaymentStatus(id, newPaymentStatus);
        return ResponseEntity.ok(updated);
    }

    // Step 3: Update candidate info with results
    @PatchMapping("/{id}/candidate")
    public ResponseEntity<ExamCheckRecord> updateCandidate(
            @PathVariable String id,
            @RequestBody WaecCandidateEntity candidate) {
        ExamCheckRecord updated = examCheckRecordService.updateCandidate(id, candidate);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/monthlyStats")
    public ResponseEntity<List<ExamCheckMonthlySummary>> getMonthlyStats(
            @RequestParam int year) {
        List<ExamCheckMonthlySummary> result = examCheckRecordService.getMonthlyStats(year);
        return ResponseEntity.ok(result);
    }














    // Get all records for a user
//    @GetMapping("/RecordsByUserId")
//    public ResponseEntity<List<ExamCheckRecord>> getByUser(Principal principal) {
//        if (principal == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//        // Load user and validate
//        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
//        Long UserId = Long.valueOf(user.getId());
////        return examCheckRecordRepository.findAllByUserId(String.valueOf(UserId));
//        return ResponseEntity.ok(examCheckRecordService.getAllByUserId(String.valueOf(UserId)));
//    }


    @GetMapping("/RecordsByUserId")
    public ResponseEntity<List<ExamCheckRecordDTO>> getByUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        List<ExamCheckRecord> records = examCheckRecordService.getAllByUserId(user.getId());
        List<ExamCheckRecordDTO> dtos = records.stream()
                .map(ExamCheckRecordDTO::fromEntity)
                .toList();

//        System.out.println(records);
        return ResponseEntity.ok(dtos);
    }

    // Get a specific record
    @GetMapping("/{id}")
    public ResponseEntity<ExamCheckRecord> getById(@PathVariable String id) {
        return examCheckRecordService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // Update record
    @PutMapping("/{id}")
    public ResponseEntity<ExamCheckRecord> update(@PathVariable String id, @RequestBody ExamCheckRecord record) {
        record.setId(id);
        return ResponseEntity.ok(examCheckRecordService.update(record));
    }

    // Delete record
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        examCheckRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/stats")
    public ResponseEntity<ExamRecordStatsDTO> getExamRecordStats() {
        return ResponseEntity.ok(
                ExamRecordStatsDTO.builder()
                        .totalRecords(examCheckRecordService.getTotalRecords())
                        .paidRecords(examCheckRecordService.getPaidRecords())
                        .pendingRecords(examCheckRecordService.getPendingRecords())
                        .build()
        );
    }

    @GetMapping("/stats/filtered")
    public ResponseEntity<ExamRecordStatsDTO> getFilteredStats(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {

        return ResponseEntity.ok(
                ExamRecordStatsDTO.builder()
                        .totalRecords(examCheckRecordService.getTotalRecordsBetweenDates(startDate, endDate))
                        .paidRecords(examCheckRecordService.getPaidRecordsBetweenDates(startDate, endDate))
                        .pendingRecords(examCheckRecordService.getPendingRecordsBetweenDates(startDate, endDate))
                        .build()
        );
    }

}
