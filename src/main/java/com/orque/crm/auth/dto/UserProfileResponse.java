package com.orque.crm.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String username;

    private String email;

    private String role;

    private Boolean enabled;
}