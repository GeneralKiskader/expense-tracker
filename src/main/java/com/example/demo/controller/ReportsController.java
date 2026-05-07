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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReportsController {

    private final ExpenseRepository expenseRepository;

    @GetMapping("/reports")
    public String showReports(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        if (expenses.isEmpty()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("hasData", false);
            return "reports";
        }

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Средний расход в день
        long days = expenses.stream()
                .map(Expense::getDate)
                .distinct()
                .count();
        BigDecimal avgDaily = days > 0 
                ? totalSpent.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        // Топ категория
        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        BigDecimal topCategoryAmount = categoryTotals.getOrDefault(topCategory, BigDecimal.ZERO);
        double topCategoryPercent = totalSpent.compareTo(BigDecimal.ZERO) > 0 
                ? topCategoryAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalSpent, 1, RoundingMode.HALF_UP).doubleValue() 
                : 0;

        // Топ 3 дня по расходам
        Map<LocalDate, BigDecimal> dailyTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getDate,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        List<Map<String, Object>> topDays = dailyTotals.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", entry.getKey().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    day.put("dayName", entry.getKey().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")));
                    day.put("amount", entry.getValue());
                    return day;
                })
                .collect(Collectors.toList());

        // Сравнение с прошлым месяцем
        LocalDate now = LocalDate.now();
        BigDecimal currentMonthTotal = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == now.getMonthValue() && e.getDate().getYear() == now.getYear())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prevMonthTotal = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == now.minusMonths(1).getMonthValue() 
                          && e.getDate().getYear() == now.minusMonths(1).getYear())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double monthChange = prevMonthTotal.compareTo(BigDecimal.ZERO) > 0 
                ? currentMonthTotal.subtract(prevMonthTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prevMonthTotal, 1, RoundingMode.HALF_UP).doubleValue() 
                : 0;

        // Данные для графиков
        List<String> categoryNames = new ArrayList<>(categoryTotals.keySet());
        List<BigDecimal> categoryAmounts = categoryNames.stream()
                .map(categoryTotals::get)
                .collect(Collectors.toList());

        // Топ дней для графика (последние 7 дней или все)
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendData = new ArrayList<>();
        dailyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(14)
                .forEach(entry -> {
                    trendLabels.add(entry.getKey().format(DateTimeFormatter.ofPattern("dd MMM")));
                    trendData.add(entry.getValue());
                });

        model.addAttribute("currentUser", user);
        model.addAttribute("hasData", true);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("avgDaily", avgDaily);
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("topCategoryPercent", topCategoryPercent);
        model.addAttribute("topDays", topDays);
        model.addAttribute("currentMonthTotal", currentMonthTotal);
        model.addAttribute("prevMonthTotal", prevMonthTotal);
        model.addAttribute("monthChange", monthChange);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categoryAmounts", categoryAmounts);
        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendData", trendData);
        model.addAttribute("expenseCount", expenses.size());

        return "reports";
    }
}