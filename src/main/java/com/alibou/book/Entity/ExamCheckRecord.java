package com.alibou.book.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamCheckRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Java 17+ UUID generation
    private String id;

    private String userId;

    private String candidateName;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;     // e.g., "paid", "pending"
    private String checkStatus;       // e.g., "completed", "not_started"

    private Instant createdAt;
    private Instant lastUpdated;
    private int checkLimit = 0;



//    @Embedded
//    private ExamDetails examDetails;
@Column(unique = true)  // Ensures one payment reference per record
private String externalRef;  // Matches PaymentStatuss.externalRef

    @OneToOne(cascade = CascadeType.ALL)
    private WaecCandidateEntity waecCandidateEntity;
}
