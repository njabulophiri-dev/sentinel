package dev.sentinel.api;

import dev.sentinel.api.ApiModels.BlacklistRequest;
import dev.sentinel.api.ApiModels.BlacklistView;
import dev.sentinel.service.BlacklistService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blacklist")
class BlacklistController {

    private final BlacklistService blacklist;

    BlacklistController(BlacklistService blacklist) {
        this.blacklist = blacklist;
    }

    @GetMapping
    List<BlacklistView> list() {
        return blacklist.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BlacklistView add(@Valid @RequestBody BlacklistRequest request, Principal principal) {
        return blacklist.add(request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable long id, Principal principal) {
        blacklist.remove(id, principal.getName());
    }
}
