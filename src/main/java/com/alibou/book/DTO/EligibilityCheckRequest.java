package com.alibou.book.DTO;

import com.alibou.book.Entity.WaecCandidateEntity;
import lombok.Data;

@Data
public class EligibilityCheckRequest {
    private String recordId;
    private WaecCandidateEntity candidate;
}
