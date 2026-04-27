package com.example.aigc.service;

import com.example.aigc.dto.SocialUserInfo;
import com.example.aigc.dto.SocialLinkItemResponse;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.SocialAccount;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.SocialAccountRepository;
import com.example.aigc.service.social.SocialProvider;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private SocialProvider onelinkProvider;

    @Mock
    private SocialProvider wechatProvider;

    private SocialAuthService socialAuthService;

    @BeforeEach
    void setUp() {
        when(onelinkProvider.provider()).thenReturn("onelinkai");
        when(wechatProvider.provider()).thenReturn("wechat");
        socialAuthService = new SocialAuthService(
                appUserRepository,
                socialAccountRepository,
                jwtTokenService,
                passwordCodec,
                List.of(onelinkProvider, wechatProvider)
        );
    }

    @Test
    void buildAuthUrlShouldUseWechatProvider() {
        when(wechatProvider.getAuthUrl(any())).thenReturn("https://wechat.example/auth");

        String authUrl = socialAuthService.buildAuthUrl("wechat").authUrl();

        assertThat(authUrl).isEqualTo("https://wechat.example/auth");
        verify(wechatProvider).getAuthUrl(any());
    }

    @Test
    void handleCallbackShouldUseProviderInfoToResolveUser() {
        AppUser user = new AppUser();
        user.userId = "u-1";
        user.username = "wechat_u1";
        user.displayName = "微信用户";
        user.role = com.example.aigc.enums.UserRole.STUDENT;
        user.enabled = true;

        when(wechatProvider.getAuthUrl(any())).thenAnswer(invocation -> "https://wechat.example/auth?state=" + invocation.getArgument(0));
        when(wechatProvider.exchangeCodeForToken("code")).thenReturn("token");
        when(wechatProvider.getUserInfo("token")).thenReturn(new SocialUserInfo("union-1", "", "微信用户", "", "", "wechat"));
        when(appUserRepository.findByProviderAndProviderUserId("wechat", "union-1")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);
        when(jwtTokenService.createToken(user)).thenReturn(new JwtTokenService.TokenPayload("jwt", Instant.now().plusSeconds(3600)));
        String state = socialAuthService.buildAuthUrl("wechat").authUrl().replace("https://wechat.example/auth?state=", "");

        assertThat(socialAuthService.handleCallback("wechat", "code", state, "127.0.0.1").accessToken()).isEqualTo("jwt");
        verify(wechatProvider).exchangeCodeForToken("code");
        verify(wechatProvider).getUserInfo("token");
        verify(appUserRepository).findByProviderAndProviderUserId(eq("wechat"), eq("union-1"));
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
                .hasMessageContaining("仅绑定该第三方登录");
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
