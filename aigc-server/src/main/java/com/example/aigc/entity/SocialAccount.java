package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "social_account")
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(nullable = false)
    public String provider;

    @Column(name = "provider_user_id", nullable = false)
    public String providerUserId;

    @Column(name = "linked_at")
    public Instant linkedAt;
}
