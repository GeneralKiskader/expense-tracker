package com.example.demo.controller;

import com.example.demo.model.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BankController {

    @GetMapping("/bank")
    public String showBankIntegration(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        model.addAttribute("currentUser", userDetails.getUser());
        // For demo: show as connected
        model.addAttribute("connected", true);
        return "bank";
    }
}