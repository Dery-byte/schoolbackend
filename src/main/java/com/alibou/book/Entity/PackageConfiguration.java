package com.alibou.book.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private SubscriptionType subscriptionType;

    private Double price;

    private Integer privateSchoolSlots;
    private Integer publicSchoolSlots;
    private Integer programsPerPrivateUniversity;
    private Integer programsPerPublicUniversity;

    private Integer maxCategorySelection;

    @Enumerated(EnumType.STRING)
    private InstitutionTypeVisibility visibility;
}
