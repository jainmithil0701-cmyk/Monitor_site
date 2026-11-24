package com.siteguard.web;

import com.siteguard.dto.SiteView;
import com.siteguard.service.MonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Simple REST API (returns JSON). */
@RestController
public class ApiController {

    private final MonitorService monitorService;

    public ApiController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/api/sites")
    public List<SiteView> sites() {
        return monitorService.buildViews();
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
