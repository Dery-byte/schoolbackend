package com.alibou.book.Entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativeProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @ElementCollection
    private Map<String, String> cutoffPoints;

    @ElementCollection
    private List<String> explanations;

    private double percentage;
    private double admissionProbability;

    @ManyToOne
    @JoinColumn(name = "university_eligibility_id")
    @JsonBackReference
    private UniversityEligibility universityEligibility;
}
