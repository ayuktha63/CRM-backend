package com.orque.crm.report.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.report.entity.CrmDashboard;
import com.orque.crm.report.repository.CrmDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final CrmDashboardRepository repository;

    /**
     * A user sees: their own dashboards of any shareType, plus any TEAM/PUBLIC dashboard
     * from elsewhere in their own tenant. TEAM was declared on the entity from the start
     * but never actually included in the original query — only PUBLIC or self-created
     * ever came back, so a dashboard shared with "TEAM" was invisible to teammates.
     */
    public List<CrmDashboard> getDashboards() {
        String orgId = UserContextHelper.currentOrganizationId();
        String username = UserContextHelper.currentUsername();

        List<CrmDashboard> own = orgId != null
                ? repository.findByOrganizationIdAndCreatedByIgnoreCase(orgId, username)
                : repository.findByCreatedByIgnoreCase(username);

        List<CrmDashboard> shared = orgId != null
                ? repository.findByOrganizationIdAndShareTypeIgnoreCaseIn(orgId, List.of("PUBLIC", "TEAM"))
                : repository.findByShareTypeIgnoreCaseIn(List.of("PUBLIC", "TEAM"));

        Map<Long, CrmDashboard> merged = new LinkedHashMap<>();
        for (CrmDashboard d : own) merged.put(d.getId(), d);
        for (CrmDashboard d : shared) merged.put(d.getId(), d);
        return new ArrayList<>(merged.values());
    }

    public CrmDashboard getDashboard(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dashboard not found"));
    }

    @Transactional
    public CrmDashboard saveDashboard(CrmDashboard dashboard, String username) {
        if (dashboard.getId() != null) {
            CrmDashboard existing = repository.findById(dashboard.getId())
                    .orElseThrow(() -> new NoSuchElementException("Dashboard not found"));
            String myOrg = UserContextHelper.currentOrganizationId();
            if (myOrg != null && existing.getOrganizationId() != null && !myOrg.equals(existing.getOrganizationId())) {
                throw new RuntimeException("Access denied.");
            }
            // Preserve the original owner/org on edit — don't let whoever last saved a
            // shared TEAM/PUBLIC dashboard silently take ownership of it.
            dashboard.setCreatedBy(existing.getCreatedBy());
            dashboard.setOrganizationId(existing.getOrganizationId());
            dashboard.setCreatedAt(existing.getCreatedAt());
        } else {
            dashboard.setCreatedBy(username);
            dashboard.setOrganizationId(UserContextHelper.currentOrganizationId());
        }
        return repository.save(dashboard);
    }

    @Transactional
    public void deleteDashboard(Long id) {
        repository.deleteById(id);
    }
}
