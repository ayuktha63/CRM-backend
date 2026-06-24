package com.orque.crm.pipeline.entity;

import com.orque.crm.enums.PipelineStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_stage_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStage previousStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStage newStage;

    private String movedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private LocalDateTime movedAt;
}