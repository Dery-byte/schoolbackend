package com.alibou.book.Entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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



    @OneToOne(cascade = CascadeType.ALL)
    private AIRecommendation aiRecommendation;

    @ManyToOne
    @JoinColumn(name = "university_eligibility_id")
    @JsonBackReference
    private UniversityEligibility universityEligibility;





    @ElementCollection
    @CollectionTable(name = "eligible_program_categories", joinColumns = @JoinColumn(name = "eligible_program_id"))
    @Column(name = "category")
    private List<String> categories = new ArrayList<>();
}
