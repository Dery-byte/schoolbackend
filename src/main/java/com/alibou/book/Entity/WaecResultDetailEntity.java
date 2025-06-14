package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
public class WaecResultDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rId;
    private String subjectcode;
    private String subject;
    private String grade;
    private String interpretation;
    @ManyToOne
    @JoinColumn(name = "candidate_id")
    @JsonIgnore
    @ToString.Exclude
    private WaecCandidateEntity candidate;
}
