package com.linktrack.dto.response;

import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
    long totalClicks,
    List<Map<String, Object>> clicksPerDay,
    List<Map<String, Object>> clicksByCountry,
    List<Map<String, Object>> clicksByDevice
) {}
