package com.alibou.book.mappers;


import com.alibou.book.DTO.BiodataResponse;
import com.alibou.book.Entity.Biodata;

import com.alibou.book.mappers.BiodataMapper;

public class BiodataMapper {

    public static BiodataResponse toResponse(Biodata biodata) {
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
                biodata.getRecord() != null ? biodata.getRecord().getId() : null
        );
    }
}