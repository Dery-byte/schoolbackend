package com.alibou.book.user;
import com.alibou.book.Entity.Providers;
import com.alibou.book.role.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static jakarta.persistence.FetchType.EAGER;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
//@RequiredArgsConstructor
@Entity
@Table(name = "_user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Ignore Hibernate proxy
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, Principal {

    @Id
    @GeneratedValue
    private Integer id;
    private String firstname;
    private String lastname;
    @Column(unique = true)
    private String username;
    private String password;
    private String phoneNumber;
    private String emailVerifed;
    private String email;

    private boolean accountLocked;
    private boolean enabled;

    @Column(columnDefinition = "integer default 0")
    @Builder.Default
    private Integer eligibilityCheckCount = 0;
    
    @Column(columnDefinition = "integer default 0")
    @Builder.Default
    private Integer checksSinceLastDiscount = 0;
    
    private String discountCode;
    
    @Column(name = "discount_package")
    private String discountPackage;
    
    @Column(name = "discount_price")
    private Double discountPrice;
    
    @Column(name = "discount_generation_mode")
    private String discountGenerationMode; // "MANUAL" or "AUTOMATIC"
    
    @Column(name = "discount_check_threshold")
    private Integer discountCheckThreshold;
    
    @ManyToMany(fetch = EAGER)
    @Builder.Default
    private List<Role> roles = new ArrayList<>(); // ✅ always initialized
//    private List<Role> roles;




    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    // SELF, GOOGLE, FACEBOOK, TWITTER, LINKEDIN, GITHUB
    private Providers provider = Providers.SELF;
    private String providerUserId;


    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    public <T> User(String username, String s, List<T> ts) {
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String fullName() {
        return getFirstname() + " " + getLastname();
    }

    @Override
    public String getName() {
        return username;
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }

    public void setEmailVerified(boolean b) {
    }
}
