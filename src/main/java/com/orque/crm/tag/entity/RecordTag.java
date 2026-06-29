package com.orque.crm.tag.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_record_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private Long recordId;

    @Column(nullable = false)
    private Long tagId;
}
