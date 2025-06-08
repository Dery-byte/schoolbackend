package com.alibou.book.Entity;


import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDetailssss {
    private String candidateName;
    private String examType;
}
