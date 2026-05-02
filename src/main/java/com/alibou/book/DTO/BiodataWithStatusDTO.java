package com.alibou.book.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiodataWithStatusDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String gender;
    private String region;
    private LocalDate dob;
    private String address;

    /** The ExamCheckRecord record_id (UUID string) linked to this biodata, if any */
    private String recordId;

    /** Whether an EligibilityRecord exists for this biodata's record */
    private boolean hasReport;

    /** The eligibility record id, if found (for download link) */
    private String eligibilityRecordId;
}
