package com.alibou.book.DTO;

import com.alibou.book.Entity.Biodata;
import com.alibou.book.Entity.Gender;
import com.alibou.book.Entity.GhanaRegion;

import java.time.LocalDate;

public record BiodataResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        String middleName,
        Gender gender,
        LocalDate dob,
        String recordId, // This matches the record_id we're querying by
        GhanaRegion region
) {
    public static BiodataResponse fromEntity(Biodata biodata) {
        return new BiodataResponse(
                biodata.getId(),
                biodata.getFirstName(),
                biodata.getLastName(),
                biodata.getEmail(),
                biodata.getPhoneNumber(),
                biodata.getAddress(),
                biodata.getMiddleName(),
                biodata.getGender(),
                biodata.getDob(),
                biodata.getRecord() != null ? biodata.getRecord().getId() : null,
                biodata.getRegion()
        );
    }
}