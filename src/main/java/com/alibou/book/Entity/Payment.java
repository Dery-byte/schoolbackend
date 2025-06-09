package com.alibou.book.Entity;

import com.alibou.book.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private Double amount;
    private String otpCode;
    private String transactionId;
    private String status;
    private LocalDateTime createdAt;
    @Column(unique = true, nullable = false)
    private String externalRef;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Linking payment to a user

//    @OneToOne
//    @JoinColumn(name = "order_id")
//    private Orders orders; // Linking payment to an order

//    public Payment(String phoneNumber, double amount, String transactionId, String pending) {
//    }
//
//    public Payment(String phoneNumber, double amount, String status) {
//    }
}
