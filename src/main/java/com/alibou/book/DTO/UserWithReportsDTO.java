package com.alibou.book.DTO;

import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithReportsDTO {
    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private String phoneNumber;
    private List<EligibilityRecord> reports;

    public static UserWithReportsDTO fromUser(User user, List<EligibilityRecord> reports) {
        return UserWithReportsDTO.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .reports(reports)
                .build();
    }
}
