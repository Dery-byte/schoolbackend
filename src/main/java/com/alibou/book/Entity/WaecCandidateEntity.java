package com.alibou.book.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Data
@Entity
public class WaecCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cindex;

    private String cname;
    private String dob;
    private int gender;

    @Column(nullable = false)
    private String examyear;

    @Column(nullable = false)
    private Long examtype;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//    @JsonIgnore
    @ToString.Exclude
    private List<WaecResultDetailEntity> resultDetails;
}
