package com.alibou.book.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recommendations")
public class AIRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String programName;

    @Column(length = 4000)
    private String careerPath;

    @Column(length = 4000)
    private String jobOpportunities;

    @Column(length = 4000)
    private String futureProspects;

    @Column(length = 4000)
    private String alternativeOptions;

    @Column(length = 4000)
    private String improvementTips;

    @OneToOne
    @JoinColumn(name = "eligible_program_id")
    private EligibleProgram eligibleProgram;

    @OneToOne
    @JoinColumn(name = "alternative_program_id")
    private AlternativeProgram alternativeProgram;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendationText;

    private double confidenceScore;

    // constructors, getters, setters
}