package com.alibou.book.Services;

import com.alibou.book.DTO.BiodataResponse;
import com.alibou.book.Entity.Biodata;
import com.alibou.book.Repositories.BiodataRepository;
import com.alibou.book.exception.BiodataNotFoundException;
import com.alibou.book.exception.DuplicateEmailException;
import com.alibou.book.exception.InvalidAgeException;
import com.alibou.book.mappers.BiodataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BiodataService {

    private final BiodataRepository biodataRepository;
    private static final int MINIMUM_AGE = 18;

    // ========== PUBLIC CRUD METHODS ========== //

    /**
     * Creates new biodata after validation
     */
    public Biodata createBiodata(Biodata biodata) {
        validateBiodataForCreation(biodata);
        return biodataRepository.save(biodata);
    }

    /**
     * Retrieves biodata by ID or throws exception if not found
     */
    public Biodata getBiodataById(Integer id) {
        return biodataRepository.findById(id)
                .orElseThrow(() -> new BiodataNotFoundException("Biodata not found with id: " + id));
    }

    /**
     * Retrieves all biodata records
     */
    public List<Biodata> getAllBiodata() {
        return biodataRepository.findAll();
    }

    /**
     * Updates existing biodata with partial updates
     */
    public Biodata updateBiodata(Integer id, Biodata updatedBiodata) {
        Biodata existingBiodata = getExistingBiodata(id);
        applyUpdates(existingBiodata, updatedBiodata);
        validateBiodataForUpdate(existingBiodata);
        return biodataRepository.save(existingBiodata);
    }

    /**
     * Deletes biodata by ID
     */
    public void deleteBiodata(Integer id) {
        if (!biodataRepository.existsById(id)) {
            throw new BiodataNotFoundException("Biodata not found with id: " + id);
        }
        biodataRepository.deleteById(id);
    }

    // ========== PUBLIC QUERY METHODS ========== //

    /**
     * Finds biodata by email or throws exception if not found
     */
    public Biodata findByEmail(String email) {
        return biodataRepository.findByEmail(email)
                .orElseThrow(() -> new BiodataNotFoundException("Biodata not found with email: " + email));
    }

    /**
     * Checks if email exists in the system
     */
    public boolean emailExists(String email) {
        return biodataRepository.existsByEmail(email);
    }

    // ========== PRIVATE VALIDATION METHODS ========== //

    private void validateBiodataForCreation(Biodata biodata) {
        validateAge(biodata.getDob());
        validateEmailUniqueness(biodata.getEmail());
    }

    private void validateBiodataForUpdate(Biodata biodata) {
        validateAge(biodata.getDob());
        // Additional update-specific validation can go here
    }

    private void validateAge(LocalDate dob) {
        if (dob != null && dob.isAfter(LocalDate.now().minusYears(MINIMUM_AGE))) {
            throw new InvalidAgeException("Person must be at least " + MINIMUM_AGE + " years old");
        }
    }

    private void validateEmailUniqueness(String email) {
        if (emailExists(email)) {
            throw new DuplicateEmailException("Email already exists: " + email);
        }
    }

    // ========== PRIVATE HELPER METHODS ========== //

    private Biodata getExistingBiodata(Integer id) {
        return biodataRepository.findById(id)
                .orElseThrow(() -> new BiodataNotFoundException("Biodata not found with id: " + id));
    }

    private void applyUpdates(Biodata target, Biodata source) {
        if (source.getFirstName() != null) target.setFirstName(source.getFirstName());
        if (source.getLastName() != null) target.setLastName(source.getLastName());
        if (source.getEmail() != null) target.setEmail(source.getEmail());
        if (source.getPhoneNumber() != null) target.setPhoneNumber(source.getPhoneNumber());
        if (source.getAddress() != null) target.setAddress(source.getAddress());
        if (source.getDob() != null) target.setDob(source.getDob());

        // Add record update if needed
        if (source.getRecord() != null) target.setRecord(source.getRecord());
    }












    @Transactional(readOnly = true)
    public BiodataResponse getBiodataByRecordId(String recordId) {
        Biodata biodata = biodataRepository.findByRecordId(recordId)
                .orElseThrow(() -> new BiodataNotFoundException("Record ID: " + recordId));
        return BiodataMapper.toResponse(biodata);
    }
}