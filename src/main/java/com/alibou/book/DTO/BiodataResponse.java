package com.alibou.book.DTO;

import com.alibou.book.Entity.Biodata;

import java.time.LocalDate;

public record BiodataResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        LocalDate dob,
        String recordId  // This matches the record_id we're querying by
) {
    public static BiodataResponse fromEntity(Biodata biodata) {
        return new BiodataResponse(
                biodata.getId(),
                biodata.getFirstName(),
                biodata.getLastName(),
                biodata.getEmail(),
                biodata.getPhoneNumber(),
                biodata.getAddress(),
                biodata.getDob(),
                biodata.getRecord() != null ? biodata.getRecord().getId() : null
        );
    }
}