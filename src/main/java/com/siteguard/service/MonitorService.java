package com.siteguard.service;

import com.siteguard.dto.SiteView;
import com.siteguard.exception.InvalidUrlException;
import com.siteguard.model.CheckResult;
import com.siteguard.model.Site;
import com.siteguard.repo.CheckResultRepository;
import com.siteguard.repo.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Core logic: runs the checks, stores results, builds dashboard data, triggers alerts. */
@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss").withZone(ZoneId.of("Asia/Kolkata"));

    private final SiteRepository siteRepo;
    private final CheckResultRepository resultRepo;
    private final AlertService alertService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Thread pool so many sites are checked in parallel instead of one by one
    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    public MonitorService(SiteRepository siteRepo, CheckResultRepository resultRepo, AlertService alertService) {
        this.siteRepo = siteRepo;
        this.resultRepo = resultRepo;
        this.alertService = alertService;
    }

    /** Background job: runs automatically every minute (see siteguard.interval-ms). */
    @Scheduled(initialDelay = 15000, fixedDelayString = "${siteguard.interval-ms:60000}")
    public void checkAllSites() {
        List<Site> sites = siteRepo.findAll();
        log.info("Scheduled run: checking {} site(s)", sites.size());

        List<CompletableFuture<Void>> tasks = sites.stream()
                .map(site -> CompletableFuture.runAsync(() -> checkSite(site), pool))
                .toList();

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
    }

    /** Checks one site, saves the result and sends an alert if its state changed. */
    public CheckResult checkSite(Site site) {
        boolean wasUp = resultRepo.findFirstBySiteIdOrderByCheckedAtDesc(site.getId())
                .map(CheckResult::isSuccess)
                .orElse(true);

        long start = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 0;
        String error = null;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(site.getUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "SiteGuard/1.0")
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            statusCode = response.statusCode();
            success = statusCode < 400;
            if (!success) {
                error = "HTTP " + statusCode;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "Check interrupted";
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        long elapsed = System.currentTimeMillis() - start;
        CheckResult saved = resultRepo.save(new CheckResult(site.getId(), success, statusCode, elapsed, error));

        if (wasUp && !success) {
            alertService.siteDown(site.getName(), site.getUrl(), error);
        } else if (!wasUp && success) {
            alertService.siteRecovered(site.getName(), site.getUrl());
        }
        return saved;
    }

    public Site addSite(String name, String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (name == null || name.isBlank()) {
            name = URI.create(url).getHost();
        }
        Site site = siteRepo.save(new Site(name.trim(), url));
        checkSite(site);   // first check immediately so the dashboard is not empty
        return site;
    }

    @Transactional
    public void deleteSite(Long id) {
        resultRepo.deleteBySiteId(id);
        siteRepo.deleteById(id);
    }

    public void checkNow(Long id) {
        siteRepo.findById(id).ifPresent(this::checkSite);
    }

    /** Builds the data shown on the dashboard (and returned by the REST API). */
    @Transactional(readOnly = true)
    public List<SiteView> buildViews() {
        return siteRepo.findAll(Sort.by("id")).stream().map(this::toView).toList();
    }

    private SiteView toView(Site site) {
        List<CheckResult> latestFirst = resultRepo.findTop20BySiteIdOrderByCheckedAtDesc(site.getId());
        long total = resultRepo.countBySiteId(site.getId());
        long upCount = resultRepo.countBySiteIdAndSuccessTrue(site.getId());
        double uptime = total == 0 ? 0 : Math.round(upCount * 1000.0 / total) / 10.0;

        if (latestFirst.isEmpty()) {
            return new SiteView(site.getId(), site.getName(), site.getUrl(),
                    false, false, 0, 0, 0, 0, "-", null, List.of());
        }

        CheckResult latest = latestFirst.get(0);
        List<SiteView.Beat> history = new ArrayList<>();
        for (CheckResult r : latestFirst) {
            history.add(new SiteView.Beat(r.isSuccess(), r.getResponseTimeMs(), format(r.getCheckedAt())));
        }
        Collections.reverse(history);   // oldest -> newest, left to right

        return new SiteView(site.getId(), site.getName(), site.getUrl(),
                true, latest.isSuccess(), latest.getStatusCode(), latest.getResponseTimeMs(),
                uptime, total, format(latest.getCheckedAt()), latest.getErrorMessage(), history);
    }

    public void seedDemoSitesIfEmpty() {
        if (siteRepo.count() > 0) {
            return;
        }
        siteRepo.save(new Site("Google", "https://www.google.com"));
        siteRepo.save(new Site("GitHub", "https://github.com"));
        siteRepo.save(new Site("Broken Demo Site (should be DOWN)", "https://this-site-does-not-exist-siteguard.com"));
        log.info("Demo sites added");
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL cannot be empty");
        }
        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null) {
                throw new InvalidUrlException("Invalid URL: " + rawUrl);
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL: " + rawUrl);
        }
        return url;
    }

    private String format(Instant instant) {
        return TIME_FORMAT.format(instant);
    }
}
