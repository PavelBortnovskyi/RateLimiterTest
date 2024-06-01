package com.neo.ratelimiter_test.utils;

import jakarta.annotation.PostConstruct;
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

    private final ConcurrentHashMap<String, Limit> limits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> requestChronicle = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> ipHeaders = new CopyOnWriteArrayList<>();
    private final String UNKNOWN = "unknown";

    @PostConstruct
    private void setIpHeaders() {
        ipHeaders.add("X-Forwarded-For");
        ipHeaders.add("X-Real-IP");
    }

    public void addClientLimit(String clientIP, int maxRequestsPerPeriod, int timePeriodInMillis) {
        limits.put(clientIP, new Limit(maxRequestsPerPeriod, timePeriodInMillis));
    }

    private void addRequestToChronicle(String clientIP) {
        if (requestChronicle.containsKey(clientIP)) {
            requestChronicle.get(clientIP).add(Instant.now());
        } else {
            requestChronicle.put(clientIP, new CopyOnWriteArrayList<>(){{
                add(Instant.now());
            }});
        }
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
        return ipAddress == null || ipAddress.isEmpty() || ipAddress.equalsIgnoreCase(UNKNOWN);
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
