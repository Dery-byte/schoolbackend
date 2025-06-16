package com.alibou.book.Entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @ElementCollection
    private Map<String, String> cutoffPoints; // subject => required grade

    private double percentage;
    private double admissionProbability;

    @ManyToOne
    @JoinColumn(name = "university_eligibility_id")
    @JsonBackReference
    private UniversityEligibility universityEligibility;
}
