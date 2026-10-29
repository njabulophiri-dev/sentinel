package dev.sentinel.api;

import dev.sentinel.api.ApiModels.AlertView;
import dev.sentinel.api.ApiModels.PageView;
import dev.sentinel.api.ApiModels.ResolveRequest;
import dev.sentinel.persistence.AlertStatus;
import dev.sentinel.service.AlertService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
class AlertController {

    private final AlertService alerts;

    AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    PageView<AlertView> list(@RequestParam(required = false) AlertStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return alerts.list(status, page, size);
    }

    @GetMapping("/{id}")
    AlertView get(@PathVariable long id) {
        return alerts.get(id);
    }

    @PostMapping("/{id}/resolution")
    AlertView resolve(@PathVariable long id, @Valid @RequestBody ResolveRequest request, Principal principal) {
        return alerts.resolve(id, request, principal.getName());
    }
}
