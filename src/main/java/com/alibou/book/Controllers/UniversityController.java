package com.alibou.book.Controllers;

import com.alibou.book.Entity.University;
import com.alibou.book.Entity.UniversityType;
import com.alibou.book.Services.UniversityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins="*")
public class UniversityController {

    @Autowired
    private UniversityService universityService;


    @PostMapping("/add/university")
    public ResponseEntity<University> addUniversity(@RequestBody University university) {
        return ResponseEntity.ok(universityService.addUniversity(university));
    }

    @GetMapping("/all")
    public ResponseEntity<List<University>> getAllUniversities() {
        return ResponseEntity.ok(universityService.getAllUniversities());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<University>> getUniversitiesByType(@PathVariable UniversityType type) {
        return ResponseEntity.ok(universityService.getUniversitiesByType(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<University> getUniversityById(@PathVariable Long id) {
        return ResponseEntity.ok(universityService.getUniversityById(id));
    }





}
