package com.alibou.book.Controllers;

import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Services.EligibilityRecordService;
import com.alibou.book.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/auth/eligibilityRecords")
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

}
