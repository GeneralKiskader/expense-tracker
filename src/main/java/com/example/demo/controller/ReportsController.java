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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReportsController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping("/reports")
    public String showReports(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        List<Expense> expenses = expenseRepository.findByUser(user);

        if (expenses.isEmpty()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("hasData", false);
            return "reports";
        }

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long days = expenses.stream().map(Expense::getDate).distinct().count();
        BigDecimal avgDaily = days == 0 ? BigDecimal.ZERO : totalSpent.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");

        BigDecimal topCategoryTotal = categoryTotals.getOrDefault(topCategory, BigDecimal.ZERO);
        long topCategoryPercent = totalSpent.compareTo(BigDecimal.ZERO) > 0
                ? topCategoryTotal.multiply(BigDecimal.valueOf(100)).divide(totalSpent, 0, RoundingMode.HALF_UP).longValue() : 0;

        List<String> categoryNames = new ArrayList<>(categoryTotals.keySet());
        List<BigDecimal> categoryAmounts = new ArrayList<>(categoryTotals.values());

        // Тренды за последние 14 дней
        Map<LocalDate, BigDecimal> dailyTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate, Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendData = new ArrayList<>();
        dailyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(14)
                .forEach(entry -> {
                    trendLabels.add(entry.getKey().format(DateTimeFormatter.ofPattern("dd MMM", new Locale("ru"))));
                    trendData.add(entry.getValue());
                });

        model.addAttribute("currentUser", user);
        model.addAttribute("hasData", true);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("avgDaily", avgDaily);
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("topCategoryPercent", topCategoryPercent);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categoryAmounts", categoryAmounts);
        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendData", trendData);
        model.addAttribute("expenseCount", expenses.size());

        return "reports";
    }
}