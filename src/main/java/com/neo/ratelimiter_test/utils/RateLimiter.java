package com.neo.ratelimiter_test.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Log4j2
@Component
@NoArgsConstructor
public class RateLimiter {

    private ConcurrentHashMap<String, Limit> limits = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> requestChronicle = new ConcurrentHashMap<>();

    public void addClientLimit(String clientIP, int maxRequestsPerPeriod, int timePeriodInMillis) {
        limits.put(clientIP, new Limit(maxRequestsPerPeriod, timePeriodInMillis));
    }

    public void addRequestToChronicle(String clientIP) {
        if (requestChronicle.containsKey(clientIP)) {
            requestChronicle.get(clientIP).add(Instant.now());
        } else {
            requestChronicle.put(clientIP, new CopyOnWriteArrayList<>(){{
                add(Instant.now());
            }});
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public boolean isAllowed(HttpServletRequest req) {
        String clientIP = getClientIp(req);
        log.info("Client IP: {}", clientIP);
        if (!limits.containsKey(clientIP)) return true;
        else {
            addRequestToChronicle(clientIP);
            CopyOnWriteArrayList<Instant> clientRequestChronicle = requestChronicle.get(clientIP).stream()
                    .filter(t -> t.isAfter(Instant.now().minusMillis(limits.get(clientIP).timePeriodInMillis())))
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            requestChronicle.replace(clientIP, clientRequestChronicle);
            return clientRequestChronicle.size() <= limits.get(clientIP).maxRequestsPerPeriod();
        }
    }
}
