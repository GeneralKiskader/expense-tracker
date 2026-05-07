package com.example.demo.controller;

import com.example.demo.model.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GoalsController {

    @GetMapping("/goals")
    public String showGoals(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        model.addAttribute("currentUser", userDetails.getUser());
        return "goals";
    }
}