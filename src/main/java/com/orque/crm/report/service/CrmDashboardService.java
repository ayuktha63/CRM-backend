package com.orque.crm.report.service;

import com.orque.crm.report.entity.CrmDashboard;
import com.orque.crm.report.repository.CrmDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final CrmDashboardRepository repository;

    public List<CrmDashboard> getDashboards() {
        return repository.findAll();
    }

    public CrmDashboard getDashboard(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dashboard not found"));
    }

    @Transactional
    public CrmDashboard saveDashboard(CrmDashboard dashboard, String username) {
        dashboard.setCreatedBy(username);
        return repository.save(dashboard);
    }

    @Transactional
    public void deleteDashboard(Long id) {
        repository.deleteById(id);
    }
}
