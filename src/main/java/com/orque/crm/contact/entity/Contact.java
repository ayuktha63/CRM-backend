package com.orque.crm.contact.entity;

import com.orque.crm.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String company;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String jobTitle;

    private String industry;

    private String website;

    private String address;

    private String country;

    private String state;

    private String city;

    private String tags;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private ContactStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}