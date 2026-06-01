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
public class BankController {

    private final UserRepository userRepository;

    @GetMapping("/bank")
    public String showBankIntegration(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        // Подтягиваем свежие данные из базы (защита от Detached entity)
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        model.addAttribute("currentUser", user);
        return "bank";
    }
}