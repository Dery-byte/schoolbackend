package com.alibou.book.Controllers;

import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Services.UniversityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/programs")
public class ProgramController {


    @Autowired
    private UniversityService universityService;

    @Autowired
    private ProgramRepository programRepository;

    @PostMapping("/{universityId}/add")
    public ResponseEntity<Program> addProgramToUniversity(
            @PathVariable Long universityId, @RequestBody Program program) {

        University university = universityService.getUniversityById(universityId);
        program.setUniversity(university);
        return ResponseEntity.ok(programRepository.save(program));
    }

    @GetMapping("/university/{universityId}")
    public ResponseEntity<List<Program>> getProgramsByUniversity(@PathVariable Long universityId) {
        University university = universityService.getUniversityById(universityId);
        return ResponseEntity.ok(programRepository.findByUniversity(university));
    }
}
