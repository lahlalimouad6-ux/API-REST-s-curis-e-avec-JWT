package ma.ens.security.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints proteges servant a tester l'autorisation par roles.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    // Accessible aux roles USER et ADMIN
    @GetMapping("/user/profile")
    public Map<String, Object> profile(Authentication authentication) {
        return Map.of(
                "message", "Bienvenue dans l'espace utilisateur",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        );
    }

    // Accessible uniquement au role ADMIN
    @GetMapping("/admin/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        return Map.of(
                "message", "Bienvenue dans l'espace administrateur",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        );
    }
}
