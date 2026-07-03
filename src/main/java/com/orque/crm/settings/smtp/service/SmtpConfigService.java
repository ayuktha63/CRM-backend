package com.orque.crm.settings.smtp.service;

import com.orque.crm.settings.smtp.dto.SmtpConfigRequest;
import com.orque.crm.settings.smtp.dto.SmtpConfigResponse;
import com.orque.crm.settings.smtp.dto.SmtpTestResult;

import java.util.List;

public interface SmtpConfigService {

    List<SmtpConfigResponse> listForCurrentUser();

    SmtpConfigResponse create(SmtpConfigRequest request);

    SmtpConfigResponse update(Long id, SmtpConfigRequest request);

    void delete(Long id);

    SmtpTestResult testConnection(Long id);
}
