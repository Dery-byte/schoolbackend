package com.alibou.book.Controllers;

import com.alibou.book.DTO.CandidateSearchRequest;
import com.alibou.book.DTO.WaecResultsRequest;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Services.WaecApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/waecs")
public class WaecControllers {

    private final WaecApiService waecApiService;
    public WaecControllers(WaecApiService waecApiService) {
        this.waecApiService = waecApiService;
    }
    // VERIFY RETURNS THE DATA FROM THE DATABASE IF EXIST AND FETCH FROM THE WAEC API OTHERWISE
    @PostMapping("/verify")
    public ResponseEntity<?> verifyWaecResult(@RequestBody WaecResultsRequest request) {
        return waecApiService.verifyResult(request);
    }



//    @GetMapping("/databaseresult")
//    public ResponseEntity<WaecCandidateEntity> fetchResult(@RequestBody CandidateSearchRequest request) {
//        return waecApiService.getCandidateWithResultsFromDb(request);
//    }

}