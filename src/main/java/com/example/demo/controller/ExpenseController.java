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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Безопасный расчёт среднего
        BigDecimal average = BigDecimal.ZERO;
        if (!expenses.isEmpty()) {
            average = total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("total", total);
        model.addAttribute("average", average);
        model.addAttribute("currentUser", user);

        // Данные для графиков (как в отчётах)
        if (!expenses.isEmpty()) {
            Map<String, BigDecimal> categoryTotals = expenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Expense::getCategory,
                            java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

            List<String> categoryNames = new java.util.ArrayList<>(categoryTotals.keySet());
            List<BigDecimal> categoryAmounts = categoryNames.stream()
                    .map(categoryTotals::get)
                    .collect(java.util.stream.Collectors.toList());

            Map<java.time.LocalDate, BigDecimal> dailyTotals = expenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Expense::getDate,
                            java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

            List<String> trendLabels = new java.util.ArrayList<>();
            List<BigDecimal> trendData = new java.util.ArrayList<>();
            dailyTotals.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .limit(14)
                    .forEach(entry -> {
                        trendLabels.add(entry.getKey().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));
                        trendData.add(entry.getValue());
                    });

            model.addAttribute("categoryNames", categoryNames);
            model.addAttribute("categoryAmounts", categoryAmounts);
            model.addAttribute("trendLabels", trendLabels);
            model.addAttribute("trendData", trendData);
        }

        return "dashboard";
    }

    @GetMapping("/expenses")
    public String listExpenses(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Динамические расчёты для карточек
        BigDecimal monthlyTotal = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == java.time.LocalDate.now().getMonthValue() 
                          && e.getDate().getYear() == java.time.LocalDate.now().getYear())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCheck = expenses.isEmpty() ? BigDecimal.ZERO : 
                total.divide(BigDecimal.valueOf(expenses.size()), 2, java.math.RoundingMode.HALF_UP);

        // Топ категория по сумме
        String topCategory = expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Expense::getCategory,
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("—");

        model.addAttribute("expenses", expenses);
        model.addAttribute("total", total);
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("avgCheck", avgCheck);
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("currentUser", user);
        return "expenses";
    }

    @GetMapping("/expenses/add")
    public String showAddForm(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        model.addAttribute("expense", new Expense());
        model.addAttribute("currentUser", userDetails.getUser());
        return "expense-form";
    }

    @PostMapping("/expenses")
    public String saveExpense(@ModelAttribute Expense expense, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        expense.setUser(userDetails.getUser());
        expenseRepository.save(expense);
        return "redirect:/expenses";
    }

    @GetMapping("/expenses/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Expense expense = expenseRepository.findById(id).orElseThrow();
        if (!expense.getUser().getId().equals(userDetails.getUser().getId())) {
            return "redirect:/expenses";
        }
        model.addAttribute("expense", expense);
        model.addAttribute("currentUser", userDetails.getUser());
        return "expense-form";
    }

    @GetMapping("/expenses/delete/{id}")
    public String deleteExpense(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Expense expense = expenseRepository.findById(id).orElseThrow();
        if (expense.getUser().getId().equals(userDetails.getUser().getId())) {
            expenseRepository.deleteById(id);
        }
        return "redirect:/expenses";
    }
}