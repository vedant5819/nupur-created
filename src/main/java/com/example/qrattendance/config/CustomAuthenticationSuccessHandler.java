package com.example.qrattendance.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isTeacher = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        boolean isStudent = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        // If there is a saved request (e.g., student scanned QR before login)
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            if (isStudent && (targetUrl.contains("/attendance") || targetUrl.contains("/student"))) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
            if (isTeacher && targetUrl.contains("/teacher")) {
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }
        }

        // Default role-based redirect
        if (isTeacher) {
            getRedirectStrategy().sendRedirect(request, response, "/teacher");
        } else if (isStudent) {
            getRedirectStrategy().sendRedirect(request, response, "/student");
        } else {
            getRedirectStrategy().sendRedirect(request, response, "/");
        }
    }
}
