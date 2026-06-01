package com.example.demo.controller;

import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PersonalityController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping("/personality")
    public String showPersonality(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int monthsEstimate = Math.max(1, expenses.size() / 15); // Примерный подсчет месяцев
        BigDecimal avgMonthlySpent = expenses.isEmpty()
                ? BigDecimal.valueOf(1500)
                : totalSpent.divide(BigDecimal.valueOf(monthsEstimate), 2, RoundingMode.HALF_UP);

        BigDecimal monthlySavings = avgMonthlySpent.multiply(BigDecimal.valueOf(0.35));
        BigDecimal annualSavings = monthlySavings.multiply(BigDecimal.valueOf(12));
        BigDecimal tenYearSavings = annualSavings.multiply(BigDecimal.valueOf(10));

        // Выбираем "личность"
        String personalityType = expenses.size() > 20 ? "Стратег-Накопитель" : "Начинающий Инвестор";
        String personalityIcon = "fa-chess-king";
        String personalityColor = "emerald";
        String personalityDescription = "Вы тщательно контролируете расходы и думаете о будущем. Ваша суперсила — дисциплина.";

        List<String> strengths = List.of("Умение копить на большие цели", "Высокий уровень самоконтроля");
        List<String> weaknesses = List.of("Стресс из-за мелких трат", "Иногда слишком жёстко ограничиваете себя");
        List<String> tips = List.of("Выделите 10% бюджета на «радость» без чувства вины", "Используйте правило 48 часов перед крупными покупками");

        model.addAttribute("personalityType", personalityType);
        model.addAttribute("personalityIcon", personalityIcon);
        model.addAttribute("personalityColor", personalityColor);
        model.addAttribute("personalityDescription", personalityDescription);
        model.addAttribute("strengths", strengths);
        model.addAttribute("weaknesses", weaknesses);
        model.addAttribute("tips", tips);
        model.addAttribute("avgMonthlySpent", avgMonthlySpent);
        model.addAttribute("tenYearSavings", tenYearSavings);
        model.addAttribute("currentUser", user);

        return "personality";
    }
}