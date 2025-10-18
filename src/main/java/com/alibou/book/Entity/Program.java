package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter@Getter
@Entity
@Table(name = "programs")
public class Program {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;


    @ManyToOne
    @JoinColumn(name = "university_id", nullable = false)
    @JsonIgnore // Prevents infinite recursion
    private University university;

//    @ElementCollection
//    @CollectionTable(name = "program_cutoff_points", joinColumns = @JoinColumn(name = "program_id"))
//    @MapKeyColumn(name = "subject")
//    @Column(name = "grade")
////    private Map<String, String> cutoffPoints; // Example: {"Mathematics": "B2", "English": "C4"}





    // ✅ Core subjects and grades (e.g., {"Mathematics": "A1", "English": "B2"})
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "program_core_subjects", joinColumns = @JoinColumn(name = "program_id", nullable = false))
    @MapKeyColumn(name = "subject")
    @Column(name = "grade")
    private Map<String, String> coreSubjects = new HashMap<>();

    // ✅ Alternative subjects and grades (e.g., {"Chemistry": "B3", "Biology": "C4"})
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "program_alternative_subjects", joinColumns = @JoinColumn(name = "program_id", nullable = false))
    @MapKeyColumn(name = "subject")
    @Column(name = "grade")
    private Map<String, String> alternativeSubjects = new HashMap<>();




    // ✅ New flexible format supporting AND/OR logic
    @ElementCollection
    @CollectionTable(name = "program_alt_groups", joinColumns = @JoinColumn(name = "program_id"))
    private List<SubjectRequirement> alternativeGroups = new ArrayList<>();



    // Many-to-Many relationship with Category
    @ManyToMany
    @JoinTable(
            name = "program_category",
            joinColumns = @JoinColumn(name = "program_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // Constructors, Getters, and Setters
}
