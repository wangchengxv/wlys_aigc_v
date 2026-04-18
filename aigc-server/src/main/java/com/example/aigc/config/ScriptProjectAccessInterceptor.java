package com.example.aigc.config;

import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.ScriptProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class ScriptProjectAccessInterceptor implements HandlerInterceptor {
    private final RequestAuthService requestAuthService;
    private final ScriptProjectService scriptProjectService;

    public ScriptProjectAccessInterceptor(
            RequestAuthService requestAuthService,
            ScriptProjectService scriptProjectService
    ) {
        this.requestAuthService = requestAuthService;
        this.scriptProjectService = scriptProjectService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        RequestUserContext userContext = requestAuthService.requireUserContext(
                request.getHeader("Authorization"),
                request.getHeader("x-aigc-token"),
                request.getHeader("x-user-id"),
                request.getHeader("x-user-name"),
                request.getHeader("x-org-unit-id"),
                request.getHeader("x-course-id")
        );
        request.setAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT, userContext);

        Object uriVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariables instanceof Map<?, ?> rawMap) {
            Object projectId = ((Map<String, Object>) rawMap).get("projectId");
            if (projectId instanceof String id && !id.isBlank()) {
                boolean writeAccess = !"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod());
                String uri = request.getRequestURI();
                if (uri != null && (uri.endsWith("/content-review/approve") || uri.endsWith("/content-review/reject"))) {
                    writeAccess = false;
                }
                scriptProjectService.assertProjectAccess(id, userContext, writeAccess);
            }
        }
        return true;
    }
}
