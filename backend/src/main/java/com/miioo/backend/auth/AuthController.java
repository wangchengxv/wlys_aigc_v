package com.miioo.backend.auth;

import com.miioo.backend.common.ApiResponse;
import com.miioo.backend.common.BizException;
import com.miioo.backend.common.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final JwtService jwtService;
    private final LoginCodeService loginCodeService;
    private final PhoneAuthUserService phoneAuthUserService;
    private final PasswordEncoder passwordEncoder;
    private final boolean returnCodeInResponse;
    private final String encodedPassword;

    public AuthController(JwtService jwtService,
                          LoginCodeService loginCodeService,
                          PhoneAuthUserService phoneAuthUserService,
                          PasswordEncoder passwordEncoder,
                          @Value("${miioo.auth.return-code-in-response:false}") boolean returnCodeInResponse) {
        this.jwtService = jwtService;
        this.loginCodeService = loginCodeService;
        this.phoneAuthUserService = phoneAuthUserService;
        this.passwordEncoder = passwordEncoder;
        this.returnCodeInResponse = returnCodeInResponse;
        this.encodedPassword = passwordEncoder.encode("admin123");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        if (!"admin".equals(request.username()) || !passwordEncoder.matches(request.password(), encodedPassword)) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = jwtService.generateToken(1L, "admin");
        return ApiResponse.success(Map.of("token", token, "userId", 1, "username", "admin"));
    }

    @GetMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(@RequestParam @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误") String phone) {
        LoginCodeService.SendCodeResult result = loginCodeService.sendCode(phone);
        if (returnCodeInResponse) {
            return ApiResponse.success(Map.of(
                    "resendAfterSeconds", result.resendAfterSeconds(),
                    "debugCode", result.code()));
        }
        return ApiResponse.success(Map.of("resendAfterSeconds", result.resendAfterSeconds()));
    }

    @PostMapping("/login/phone")
    public ApiResponse<Map<String, Object>> loginByPhone(@RequestBody @Valid PhoneLoginRequest request) {
        loginCodeService.verifyAndConsume(request.phone(), request.code());

        Long userId = phoneAuthUserService.getOrCreateUserId(request.phone());
        long expireSeconds = request.autoLogin() ? 7 * 24 * 60 * 60L : 2 * 60 * 60L;
        String token = jwtService.generateToken(userId, maskPhone(request.phone()), expireSeconds);
        Map<String, Object> userInfo = Map.of(
                "userId", userId,
                "phone", request.phone(),
                "username", maskPhone(request.phone()));
        return ApiResponse.success(Map.of("token", token, "userInfo", userInfo));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.success(Map.of("userId", userId, "username", userId == 1L ? "admin" : "user-" + userId));
    }

    @GetMapping("/wechat/qrcode")
    public ApiResponse<Map<String, Object>> wechatQrcode() {
        return ApiResponse.success(Map.of(
                "supported", false,
                "message", "微信登录尚未配置公众号参数"));
    }

    @GetMapping("/wechat/poll")
    public ApiResponse<Map<String, Object>> wechatPoll(@RequestParam String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(400, "轮询ID不能为空");
        }
        return ApiResponse.success(Map.of("status", "unsupported"));
    }

    @PostMapping("/wechat/bind")
    public ApiResponse<Void> wechatBind(@RequestBody Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BizException(400, "参数不能为空");
        }
        throw new BizException(501, "当前环境未启用微信登录");
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record PhoneLoginRequest(
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误") String phone,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误") String code,
            boolean autoLogin) {
    }
}
