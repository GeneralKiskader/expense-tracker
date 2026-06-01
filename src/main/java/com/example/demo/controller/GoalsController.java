package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class GoalsController {
    private final UserRepository userRepository;

    @GetMapping("/goals")
    public String showGoals(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        model.addAttribute("currentUser", user);
        return "goals";
    }
}