package com.cinema.dto.auth;

import com.cinema.entity.Member;
import com.cinema.entity.User;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        String memberCode) {

    public static UserResponse from(User user, Member member) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getCode(),
                user.getStatus().name(),
                member == null ? null : member.getMemberCode());
    }
}
