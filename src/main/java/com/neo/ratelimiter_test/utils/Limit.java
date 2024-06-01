package com.neo.ratelimiter_test.utils;

public record Limit(int maxRequestsPerPeriod, long timePeriodInMillis) {
}
