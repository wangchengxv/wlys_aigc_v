package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.SocialLinkItemResponse;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.SocialAccount;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.SocialAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordCodec passwordCodec;

    private SocialAuthService socialAuthService;

    @BeforeEach
    void setUp() {
        socialAuthService = new SocialAuthService(
                appUserRepository,
                socialAccountRepository,
                jwtTokenService,
                passwordCodec,
                new AuthProperties(),
                new ObjectMapper()
        );
    }

    @Test
    void getLinksShouldReturnDescendingByLinkedAt() {
        SocialAccount older = social("u-1", "onelinkai", "p-1", Instant.parse("2026-01-01T00:00:00Z"));
        SocialAccount newer = social("u-1", "onelinkai", "p-2", Instant.parse("2026-02-01T00:00:00Z"));
        when(socialAccountRepository.findAllByUserId("u-1")).thenReturn(List.of(older, newer));

        List<SocialLinkItemResponse> links = socialAuthService.getLinks("u-1");

        assertThat(links).hasSize(2);
        assertThat(links.get(0).providerUserId()).isEqualTo("p-2");
        assertThat(links.get(1).providerUserId()).isEqualTo("p-1");
    }

    @Test
    void unbindShouldRejectWhenOnlySocialLoginRemains() {
        AppUser user = new AppUser();
        user.userId = "u-1";
        user.provider = "onelinkai";
        user.providerUserId = "p-1";

        SocialAccount linked = social("u-1", "onelinkai", "p-1", Instant.now());

        when(appUserRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserIdAndProvider("u-1", "onelinkai")).thenReturn(Optional.of(linked));
        when(socialAccountRepository.countByUserId("u-1")).thenReturn(1L);

        assertThatThrownBy(() -> socialAuthService.unbind("u-1", "onelinkai"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅绑定 OneLinkAI");
        verify(socialAccountRepository, never()).delete(any());
    }

    @Test
    void unbindShouldDeleteLinkAndClearPrimarySocialFields() {
        AppUser user = new AppUser();
        user.userId = "u-1";
        user.provider = "onelinkai";
        user.providerUserId = "p-1";
        user.updatedAt = Instant.parse("2026-01-01T00:00:00Z");

        SocialAccount linked = social("u-1", "onelinkai", "p-1", Instant.now());

        when(appUserRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserIdAndProvider("u-1", "onelinkai")).thenReturn(Optional.of(linked));
        when(socialAccountRepository.countByUserId("u-1")).thenReturn(2L);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        socialAuthService.unbind("u-1", "onelinkai");

        verify(socialAccountRepository).delete(linked);
        verify(appUserRepository).save(user);
        assertThat(user.provider).isNull();
        assertThat(user.providerUserId).isNull();
        assertThat(user.linkedAt).isNull();
    }

    private SocialAccount social(String userId, String provider, String providerUserId, Instant linkedAt) {
        SocialAccount account = new SocialAccount();
        account.userId = userId;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.linkedAt = linkedAt;
        return account;
    }
}
