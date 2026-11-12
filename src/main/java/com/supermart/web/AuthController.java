package com.supermart.web;

import com.supermart.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/access-denied")
    public String accessDenied() { return "access-denied"; }

    @GetMapping("/")
    public String home(Authentication auth) {
        boolean admin = auth != null && auth.getAuthorities().stream().anyMatch(a -> Role.ADMIN.equals(a.getAuthority()));
        return admin ? "redirect:/dashboard" : "redirect:/billing";
    }
}
