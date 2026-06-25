package com.orque.crm.email.dto;

import com.orque.crm.enums.EmailProvider;
import lombok.Data;

@Data
public class ConnectMailboxRequest {

    private String emailAddress;

    private EmailProvider provider;
}