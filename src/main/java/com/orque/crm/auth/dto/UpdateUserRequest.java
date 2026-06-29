package com.orque.crm.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {

    private String firstName;
    private String lastName;

    @Email
    private String email;

    private String phone;
    private String role;   // SALES_ADMIN | SALES
    private String status; // ACTIVE | INACTIVE
}
