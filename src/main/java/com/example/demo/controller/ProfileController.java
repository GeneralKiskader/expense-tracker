package com.example.demo.controller;

import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpenseRepository expenseRepository;

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentUser", user);
        model.addAttribute("expenseCount", expenses.size());
        model.addAttribute("totalAmount", totalAmount);
        // Дата регистрации пока не хранится в модели — оставляем заглушку или можно добавить позже
        model.addAttribute("registrationDate", "15 янв 2026");

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлён!");
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        
        User user = userDetails.getUser();
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Текущий пароль неверный");
            return "redirect:/profile";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Пароли не совпадают");
            return "redirect:/profile";
        }
        
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль должен быть не менее 6 символов");
            return "redirect:/profile";
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Пароль успешно изменён!");
        return "redirect:/profile";
    }
}