package com.alibou.book.Controllers;

import com.alibou.book.Entity.Program;
import com.alibou.book.Services.EligibilityCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins="*")
public class EligibilityController {

    @Autowired
    private EligibilityCheckerService eligibilityCheckerService;
//    @GetMapping("/check")
//    public ResponseEntity<Map<String, Object>> checkEligibility(Principal principal) {
//        return ResponseEntity.ok(eligibilityCheckerService.getEligibleAndAlternativePrograms(principal.getName()));
//    }

}
