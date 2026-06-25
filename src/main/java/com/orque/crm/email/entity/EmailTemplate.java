package com.orque.crm.email.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String templateName;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}