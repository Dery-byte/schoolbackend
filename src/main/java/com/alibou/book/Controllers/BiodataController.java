package com.alibou.book.Controllers;

import com.alibou.book.DTO.BiodataDTO;
import com.alibou.book.DTO.BiodataResponse;
import com.alibou.book.Entity.Biodata;
import com.alibou.book.Services.BiodataService;
import com.alibou.book.exception.DuplicateEmailException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/biodata")
@RequiredArgsConstructor
public class BiodataController {

    private final BiodataService biodataService;

    @PostMapping("/addBiodata")
    public ResponseEntity<Biodata> createBiodata(@Valid @RequestBody Biodata biodata) {
        try {
            Biodata savedBiodata = biodataService.createBiodata(biodata);
            return new ResponseEntity<>(savedBiodata, HttpStatus.CREATED);
        } catch (DuplicateEmailException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BiodataDTO> getBiodataById(@PathVariable Integer id) {
        Biodata biodata = biodataService.getBiodataById(id);
        return ResponseEntity.ok(BiodataDTO.fromEntity(biodata));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<BiodataDTO>> getAllBiodata() {
        List<BiodataDTO> dtos = biodataService.getAllBiodata()
                .stream()
                .map(BiodataDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Biodata> updateBiodata(
            @PathVariable Integer id,
            @Valid @RequestBody Biodata updatedBiodata
    ) {
        return ResponseEntity.ok(biodataService.updateBiodata(id, updatedBiodata));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBiodata(@PathVariable Integer id) {
        biodataService.deleteBiodata(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Biodata> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(biodataService.findByEmail(email));
    }



    @GetMapping("/byrecordId/{recordId}")
    public ResponseEntity<BiodataResponse> getBiodataByRecordId(
            @PathVariable String recordId) {
        BiodataResponse response = biodataService.getBiodataByRecordId(recordId);
        return ResponseEntity.ok(response);
    }






    @PutMapping("/updateBiodata")
    public ResponseEntity<Biodata> updateBiodata(
            @Valid @RequestBody Biodata updatedBiodata
    ) {
        if (updatedBiodata.getId() == null) {
            throw new IllegalArgumentException("Biodata ID must be provided in the request body");
        }
        Biodata updated = biodataService.updateBiodata(updatedBiodata.getId(), updatedBiodata);
        return ResponseEntity.ok(updated);
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return errorResponse;
    }
}