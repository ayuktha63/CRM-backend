package com.orque.crm.campaign.controller;

import com.orque.crm.campaign.dto.*;
import com.orque.crm.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @RequestBody CreateCampaignRequest request
    ) {
        return ResponseEntity.ok(
                campaignService.createCampaign(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(
                campaignService.getAllCampaigns()
        );
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable Long campaignId
    ) {
        return ResponseEntity.ok(
                campaignService.getCampaign(campaignId)
        );
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody CreateCampaignRequest request
    ) {
        return ResponseEntity.ok(
                campaignService.updateCampaign(campaignId, request)
        );
    }

    @PostMapping("/{campaignId}/recipients")
    public ResponseEntity<CampaignRecipientResponse> addRecipient(
            @PathVariable Long campaignId,
            @RequestBody AddRecipientsRequest request
    ) {
        return ResponseEntity.ok(
                campaignService.addRecipient(campaignId, request)
        );
    }

    @GetMapping("/{campaignId}/recipients")
    public ResponseEntity<List<CampaignRecipientResponse>> getRecipients(
            @PathVariable Long campaignId
    ) {
        return ResponseEntity.ok(
                campaignService.getRecipients(campaignId)
        );
    }
    @PostMapping("/{campaignId}/launch")
    public ResponseEntity<String> launchCampaign(
            @PathVariable Long campaignId
    ) {
        campaignService.launchCampaign(campaignId);
        return ResponseEntity.ok("Campaign launched successfully");
    }
    @GetMapping("/{campaignId}/history")
    public ResponseEntity<List<CampaignHistoryResponse>> getCampaignHistory(
            @PathVariable Long campaignId
    ) {
        return ResponseEntity.ok(
                campaignService.getCampaignHistory(campaignId)
        );
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable Long campaignId
    ) {
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignId}/metrics")
    public ResponseEntity<CampaignMetricsResponse> getCampaignMetrics(
            @PathVariable Long campaignId
    ) {
        return ResponseEntity.ok(
                campaignService.getCampaignMetrics(campaignId)
        );
    }
}