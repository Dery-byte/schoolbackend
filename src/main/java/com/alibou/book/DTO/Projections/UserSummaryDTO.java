package com.alibou.book.DTO.Projections;

import com.alibou.book.user.User;

import java.time.LocalDateTime;

public record UserSummaryDTO(
        Integer id,
        String fullName,
        LocalDateTime signupDate,
        String username
) {
    public static UserSummaryDTO fromUser(User user) {
        return new UserSummaryDTO(
                user.getId(),
                user.getFullName(),

                user.getCreatedDate(),
                user.getUsername()
        );
    }
}