package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;
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

    // Constructors, Getters, and Setters
}
