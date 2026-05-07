package com.example.demo.controller;

import com.example.demo.model.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BudgetController {

    @GetMapping("/budgets")
    public String showBudgets(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        model.addAttribute("currentUser", userDetails.getUser());
        return "budgets";
    }
}