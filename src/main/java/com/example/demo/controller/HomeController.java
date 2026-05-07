package com.example.demo.controller;

import com.example.demo.model.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails != null) {
            // Logged in user -> redirect to dashboard
            return "redirect:/dashboard";
        }
        // Not logged in -> show beautiful landing page
        return "index";
    }
}