package com.alibou.book.Controllers;


import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Services.ExamCheckRecordService;
import com.alibou.book.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/records")
@RequiredArgsConstructor
public class ExamCheckRecordController {
    private final ExamCheckRecordService examCheckRecordService;
    private final UserDetailsService userDetailsService;


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















    // Get all records for a user
    @GetMapping("/RecordsByUserId")
    public ResponseEntity<List<ExamCheckRecord>> getByUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Load user and validate
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        Long UserId = Long.valueOf(user.getId());
//        return examCheckRecordRepository.findAllByUserId(String.valueOf(UserId));
        return ResponseEntity.ok(examCheckRecordService.getAllByUserId(String.valueOf(UserId)));
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

}
