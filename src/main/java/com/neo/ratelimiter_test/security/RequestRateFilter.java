package com.neo.ratelimiter_test.security;

import com.neo.ratelimiter_test.utils.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Log4j2
@Component
@RequiredArgsConstructor
public class RequestRateFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (rateLimiter.isAllowed(request)) {
            log.info("Rate filter passed");
            doFilter(request, response, filterChain);
        } else {
            log.info("Rate filter pass failed");
            response.setStatus(429);
        }
    }
}
