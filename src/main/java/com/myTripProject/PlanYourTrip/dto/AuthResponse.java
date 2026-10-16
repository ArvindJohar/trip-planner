package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.entity.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long id;
    private String name;
    private String email;
    private Role role;
}