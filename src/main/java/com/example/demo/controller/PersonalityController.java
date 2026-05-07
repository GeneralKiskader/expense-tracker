package com.example.demo.controller;

import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.repository.ExpenseRepository;
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

    @GetMapping("/personality")
    public String showPersonality(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Примерный средний расход в месяц (грубая оценка по количеству записей)
        int monthsEstimate = Math.max(1, expenses.size() / 8);
        BigDecimal avgMonthlySpent = expenses.isEmpty() 
                ? BigDecimal.valueOf(18500) 
                : totalSpent.divide(BigDecimal.valueOf(monthsEstimate), 2, RoundingMode.HALF_UP);

        // Динамические данные для "Будущего Я"
        model.addAttribute("currentAge", 28);
        model.addAttribute("currentSavings", totalSpent.multiply(BigDecimal.valueOf(3)).setScale(0, RoundingMode.HALF_UP)); // гипотетические накопления
        model.addAttribute("monthlySavings", avgMonthlySpent.multiply(BigDecimal.valueOf(0.35)).setScale(0, RoundingMode.HALF_UP)); // ~35% от среднего расхода как потенциал сбережений
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("expenseCount", expenses.size());

        // Личность пока оставляем как есть (в будущем можно сделать умный расчёт по категориям)
        String personalityType = "Стратегический Накопитель";
        String personalityIcon = "fa-solid fa-chess-king";
        String personalityColor = "emerald";
        String personalityDescription = "Ты тщательно планируешь каждый расход и всегда думаешь о будущем. " +
                "Твоя главная суперсила — дисциплина и долгосрочное мышление.";

        List<String> strengths = List.of(
                "Отличное планирование бюджета",
                "Высокий уровень самоконтроля",
                "Умеешь копить на большие цели",
                "Редко совершаешь импульсивные покупки"
        );

        List<String> weaknesses = List.of(
                "Иногда слишком жёстко ограничиваешь себя",
                "Можешь упускать возможности для приятных моментов",
                "Стресс из-за мелких трат"
        );

        List<String> tips = List.of(
                "Выдели 5-10% бюджета на \"радость\" без чувства вины",
                "Раз в квартал устраивай себе \"день без контроля\"",
                "Используй правило 48 часов перед крупными покупками"
        );

        model.addAttribute("personalityType", personalityType);
        model.addAttribute("personalityIcon", personalityIcon);
        model.addAttribute("personalityColor", personalityColor);
        model.addAttribute("personalityDescription", personalityDescription);
        model.addAttribute("strengths", strengths);
        model.addAttribute("weaknesses", weaknesses);
        model.addAttribute("tips", tips);
        model.addAttribute("currentUser", user);

        return "personality";
    }
}