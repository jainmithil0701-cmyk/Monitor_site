package com.siteguard.model;

import jakarta.persistence.*;
import java.time.Instant;

/** The result of one health check of a site (stored every minute). */
@Entity
@Table(name = "check_result")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long siteId;

    @Column(nullable = false)
    private boolean success;

    private int statusCode;

    private long responseTimeMs;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private Instant checkedAt = Instant.now();

    protected CheckResult() { }

    public CheckResult(Long siteId, boolean success, int statusCode, long responseTimeMs, String errorMessage) {
        this.siteId = siteId;
        this.success = success;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        if (errorMessage != null && errorMessage.length() > 490) {
            errorMessage = errorMessage.substring(0, 490);
        }
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public Long getSiteId() { return siteId; }
    public boolean isSuccess() { return success; }
    public int getStatusCode() { return statusCode; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCheckedAt() { return checkedAt; }
}
