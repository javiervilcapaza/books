package com.aidanwhiteley.books.controller.config;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;

/**
 * Based on https://stackoverflow.com/questions/49095383/spring-security-5-stateless-oauth2-login-how-to-implement-cookies-based-author
 */
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "cloudy-oauth2-auth";

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return fetchCookie(request)
                .map(this::toOAuth2AuthorizationRequest)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }

        Cookie cookie = new Cookie(COOKIE_NAME, fromAuthorizationRequest(authorizationRequest));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(-1);   // Expire when browser closed - bug in API means explicit removal not possible
        response.addCookie(cookie);
    }


    private String fromAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {

        return Base64.getUrlEncoder().encodeToString(
                SerializationUtils.serialize(authorizationRequest));
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {

        fetchCookie(request).ifPresent(cookie -> {

            cookie.setValue("");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        });
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request) {

        // Question: How to remove the cookie, because we don't have access to response object here.
        return loadAuthorizationRequest(request);
    }

    private Optional<Cookie> fetchCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cooky : cookies) {
                if (cooky.getName().equals(COOKIE_NAME)) {
                    return Optional.of(cooky);
                }
            }
        }

        return Optional.empty();
    }

    private OAuth2AuthorizationRequest toOAuth2AuthorizationRequest(Cookie cookie) {

        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue()));
    }
}