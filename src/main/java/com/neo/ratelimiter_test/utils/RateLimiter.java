package com.neo.ratelimiter_test.utils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Log4j2
@Component
@NoArgsConstructor
public class RateLimiter {

    private final Map<String, Limit> limits = new ConcurrentHashMap<>(1000);
    private final List<String> ipHeaders = new CopyOnWriteArrayList<>();

    @PostConstruct
    private void basicInit() {
        ipHeaders.add("X-Forwarded-For");
        ipHeaders.add("X-Real-IP");
        //This should be configured somewhere in business logic. Now it is here just for test
        this.addClientLimit("0:0:0:0:0:0:0:1", 3, 3000);
    }

    public void addClientLimit(String clientIP, int maxRequestsPerPeriod, int timePeriodInMillis) {
        limits.put(clientIP, new Limit(maxRequestsPerPeriod, timePeriodInMillis, new CopyOnWriteArrayList<>()));
    }

    private void addRequestToChronicle(String clientIP) {
        limits.computeIfPresent(clientIP, (key, limit) -> {
            limit.getRequestChronicle().add(Instant.now());
            return limit;
        });
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = "";
        for (String headerName : ipHeaders) {
            clientIp = request.getHeader(headerName);
            if (!checkAddress(clientIp)) return clientIp;
        }
        return request.getRemoteAddr();
    }

    private boolean checkAddress(String ipAddress) {
        return ipAddress == null || ipAddress.isEmpty() || ipAddress.equalsIgnoreCase("unknown");
    }

    public boolean isAllowed(HttpServletRequest req) {
        String clientIP = getClientIp(req);
        log.info("Client IP: {}", clientIP);
        if (!limits.containsKey(clientIP)) return true;
        else {
            Limit clientLimit = limits.get(clientIP);
            Instant threshold = Instant.now().minusMillis(clientLimit.getTimePeriodInMillis());

            synchronized (clientLimit) {
                addRequestToChronicle(clientIP);
                clientLimit.getRequestChronicle().removeIf(t -> t.isBefore(threshold));
                return clientLimit.getRequestChronicle().size() <= clientLimit.getMaxRequestsPerPeriod();
            }
        }
    }
}
