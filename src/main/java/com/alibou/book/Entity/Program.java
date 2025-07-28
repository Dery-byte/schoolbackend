package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @ElementCollection
    @CollectionTable(name = "program_cutoff_points", joinColumns = @JoinColumn(name = "program_id"))
    @MapKeyColumn(name = "subject")
    @Column(name = "grade")
    private Map<String, String> cutoffPoints; // Example: {"Mathematics": "B2", "English": "C4"}



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
