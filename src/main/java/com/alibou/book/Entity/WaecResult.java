package com.alibou.book.Entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name = "waec_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"indexNumber", "examYear", "examType"})
})
public class WaecResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String examYear;
    private String examType; // E.g., WASSCE, SSCE
    private String indexNumber; // Unique Per result

    @ElementCollection
    @CollectionTable(name = "waec_subjects", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "subject")
    @Column(name = "grade")
    private Map<String, String> subjectsGrades; // e.g., {"Mathematics": "B2", "English": "C4"}
    private String uploadedBy; // username of the uploader (self or proxy)

    @CreationTimestamp
    private LocalDateTime createdAt;
    private boolean verified = false; // If verified by WAEC API


}
