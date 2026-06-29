package com.orque.crm.tag.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String colorHex;
}
