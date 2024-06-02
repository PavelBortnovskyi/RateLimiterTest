package com.neo.ratelimiter_test.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Limit {

    private int maxRequestsPerPeriod;

    private long timePeriodInMillis;

    private List<Instant> requestChronicle;
}
