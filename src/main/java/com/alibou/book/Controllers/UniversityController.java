package com.alibou.book.Controllers;

import com.alibou.book.Entity.University;
import com.alibou.book.Entity.UniversityType;
import com.alibou.book.Repositories.UniversityRepository;
import com.alibou.book.Services.UniversityService;
import com.alibou.book.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    @Autowired
    private UniversityRepository universityRepository;



    @PostMapping("/add/university")
    public ResponseEntity<List<University>> addUniversity(@RequestBody List<University> universityList) {
        List<University> saved = universityService.addUniversity(universityList);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/all")
    public ResponseEntity<List<University>> getAllUniversities() {
        return ResponseEntity.ok(universityService.getAllUniversities());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<University>> getUniversitiesByType(@PathVariable UniversityType type) {
        return ResponseEntity.ok(universityService.getUniversitiesByType(type));
    }

    @GetMapping("/getUniversityById/{id}")
    public ResponseEntity<University> getUniversityById(@PathVariable Long id) {
        return ResponseEntity.ok(universityService.getUniversityById(id));
    }


    @DeleteMapping("/deleteUniversityById/{id}")
    public ResponseEntity<University> deleteUniversityById(@PathVariable Long id) {
        University deletedUniversity = universityService.deleteUniversityById(id);
        return ResponseEntity.ok(deletedUniversity);
    }



    @PutMapping("/updateUniverity")
    @Transactional
    public ResponseEntity<University> updateUniversity(
            @Valid @RequestBody com.alibou.book.dto.UpdateUniversityDTO updateDTO) {
        University university = universityRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "University not found with id: " + updateDTO.getId()));

        // Update fields
        university.setName(updateDTO.getName());
        university.setLocation(updateDTO.getLocation());
        university.setType(updateDTO.getType());

        University updatedUniversity = universityRepository.save(university);
        return ResponseEntity.ok(updatedUniversity);
    }





}
