package com.siteguard.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A website or API endpoint that we monitor. */
@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Site() { }   // required by JPA

    public Site(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Instant getCreatedAt() { return createdAt; }
}
