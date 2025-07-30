package com.alibou.book.DTO;

import com.alibou.book.Entity.Biodata;

import java.time.LocalDate;

public record BiodataDTO(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        LocalDate dob,
        String recordId  // Only include ID instead of whole object
) {
    public static BiodataDTO fromEntity(Biodata biodata) {
        return new BiodataDTO(
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