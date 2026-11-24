package com.siteguard.dto;

import java.util.List;

/** Everything the dashboard needs to show for one site (Java 17 record). */
public record SiteView(
        Long id,
        String name,
        String url,
        boolean checked,        // false if no check has run yet
        boolean up,
        int statusCode,
        long responseMs,
        double uptimePercent,
        long totalChecks,
        String lastChecked,
        String error,
        List<Beat> history) {

    /** One small box in the "last 20 checks" strip. */
    public record Beat(boolean up, long ms, String time) { }
}
