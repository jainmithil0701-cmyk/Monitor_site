package com.siteguard.web;

import com.siteguard.dto.SiteView;
import com.siteguard.exception.InvalidUrlException;
import com.siteguard.service.MonitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Handles the web pages (Thymeleaf dashboard and form actions). */
@Controller
public class DashboardController {

    private final MonitorService monitorService;

    public DashboardController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<SiteView> views = monitorService.buildViews();
        long upCount = views.stream().filter(v -> v.checked() && v.up()).count();
        long downCount = views.stream().filter(v -> v.checked() && !v.up()).count();

        model.addAttribute("views", views);
        model.addAttribute("total", views.size());
        model.addAttribute("upCount", upCount);
        model.addAttribute("downCount", downCount);
        return "dashboard";
    }

    @PostMapping("/sites")
    public String addSite(@RequestParam String name, @RequestParam String url, RedirectAttributes redirect) {
        try {
            monitorService.addSite(name, url);
            redirect.addFlashAttribute("message", "Site added and first check completed.");
        } catch (InvalidUrlException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/sites/{id}/check")
    public String checkNow(@PathVariable Long id, RedirectAttributes redirect) {
        monitorService.checkNow(id);
        redirect.addFlashAttribute("message", "Check completed.");
        return "redirect:/";
    }

    @PostMapping("/sites/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        monitorService.deleteSite(id);
        redirect.addFlashAttribute("message", "Site deleted.");
        return "redirect:/";
    }
}
