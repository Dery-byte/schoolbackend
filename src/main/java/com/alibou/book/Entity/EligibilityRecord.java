package com.alibou.book.Entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    ///private String recordId;
    private LocalDateTime createdAt;

    @OneToOne
    private ExamCheckRecord examCheckRecord;
    @OneToMany(mappedBy = "eligibilityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    //@JsonManagedReference
    @JsonManagedReference
    private List<UniversityEligibility> universities;


    @ElementCollection
    @CollectionTable(name = "eligibility_record_categories", joinColumns = @JoinColumn(name = "eligibility_record_id"))
    @Column(name = "category_name")
    private List<String> selectedCategories;


}
