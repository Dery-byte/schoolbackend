package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    private String universityName;
    private String location;
    private String type; // e.g., PUBLIC, PRIVATE

    @ManyToOne
    @JoinColumn(name = "eligibility_record_id")
    @JsonBackReference // Prevents infinite recursion during JSON serialization
    private EligibilityRecord eligibilityRecord;

    @OneToMany(mappedBy = "universityEligibility", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<EligibleProgram> eligiblePrograms;

    @OneToMany(mappedBy = "universityEligibility", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<AlternativeProgram> alternativePrograms;
}
