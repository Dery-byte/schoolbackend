package com.alibou.book.Controllers;

import com.alibou.book.DTO.ProgramRequestDTO;
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
//
//    @PostMapping("/{universityId}/add")
//    public ResponseEntity<List<Program>> addProgramToUniversity(
//            @PathVariable Long universityId,
//            @RequestBody List<Program> programs) {
//        University university = universityService.getUniversityById(universityId);
//        for (Program program : programs) {
//            program.setUniversity(university);
//        }
//        List<Program> savedPrograms = programRepository.saveAll(programs);
//        return ResponseEntity.ok(savedPrograms);
//    }



    @PostMapping("/addProgram")
    public ResponseEntity<List<Program>> addProgramToUniversity(
            @RequestBody ProgramRequestDTO requestDTO) {

        University university = universityService.getUniversityById(requestDTO.getUniversityId());
        List<Program> programs = requestDTO.getPrograms();

        for (Program program : programs) {
            program.setUniversity(university);
        }

        List<Program> savedPrograms = programRepository.saveAll(programs);
        return ResponseEntity.ok(savedPrograms);
    }



    @GetMapping("/university/{universityId}")
    public ResponseEntity<List<Program>> getProgramsByUniversity(@PathVariable Long universityId) {
        University university = universityService.getUniversityById(universityId);
        return ResponseEntity.ok(programRepository.findByUniversity(university));
    }
}
