package com.alibou.book.Controllers;

import com.alibou.book.DTO.EligibilityMonthlySummary;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Services.EligibilityRecordService;
import com.alibou.book.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/auth/eligibilityRecords")
@CrossOrigin(origins="*")
@RequiredArgsConstructor
public class EligibilityRecordController {

    private final EligibilityRecordService eligibilityRecordService;
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
        System.out.println(STR."Records for userId : \{userId}");
        System.out.println(STR." User has : \{records.size()}");
        return ResponseEntity.ok(records);
    }

//
//    @GetMapping("/my-eligibility-records")
//    public ResponseEntity<List<EligibilityRecord>> getEligibilityRecordsForUser(@AuthenticationPrincipal UserDetails userDetails) {
//        String username = userDetails.getUsername();
//        User user = (User) userDetailsService.loadUserByUsername(username);
//        String userId = String.valueOf(user.getId());
//        List<EligibilityRecord> records = eligibilityRecordService.getRecordsByUser(userId);
//        return ResponseEntity.ok(records);
//    }



//    @GetMapping("/my-eligibility-records")
//    public ResponseEntity<List<EligibilityRecord>> getEligibilityRecordsForUser(Authentication authentication) {
//        String username = authentication.getName(); // safer than Principal
//
//        System.out.println(username);
//        User user = (User) userDetailsService.loadUserByUsername(username);
//        String userId = String.valueOf(user.getId());
//
//        List<EligibilityRecord> records = eligibilityRecordService.getRecordsByUser(userId);
//        System.out.println("Records for userId: " + userId);
//        System.out.println("User has: " + records.size());
//
//        return ResponseEntity.ok(records);
//    }






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
    public List<EligibilityMonthlySummary> getMonthlyStats(
            @RequestParam int year) {
        return eligibilityRecordService.getMonthlyStats(year);
    }



}
