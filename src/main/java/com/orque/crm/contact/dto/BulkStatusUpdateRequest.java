package com.orque.crm.contact.dto;

import com.orque.crm.enums.ContactStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkStatusUpdateRequest {

    @NotEmpty
    private List<Long> contactIds;

    @NotNull
    private ContactStatus status;
}