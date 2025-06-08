package com.alibou.book.DTO;

import lombok.Data;

import java.util.List;

@Data
public class WaecCandidateDTO {
    private String cindex;
    private String cname;
    private String dob;
    private String gender;
    private String examtype;
    private Integer examyear;
    private List<WaecResultDetailDTO> resultDetails;

    // Getters and setters
}
