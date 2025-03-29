//package com.alibou.book.Entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//@Setter
//@Getter
//@Entity
//public class ResultDetail {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String subjectcode;
//    private String subject;
//    private String grade;
//    private String interpretation;
//
//    @ManyToOne
//    @JoinColumn(name = "candidate_result_id")
//    private CandidateResult candidateResult;
//
//    // getters and setters
//}
