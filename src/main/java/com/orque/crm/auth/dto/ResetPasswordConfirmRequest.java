package com.orque.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordConfirmRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}
