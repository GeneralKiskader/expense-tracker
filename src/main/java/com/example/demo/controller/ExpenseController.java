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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        List<String> categoryNames = new ArrayList<>();
        List<BigDecimal> categoryAmounts = new ArrayList<>();
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendData = new ArrayList<>();

        if (expenses != null && !expenses.isEmpty()) {
            total = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            average = total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

            Map<String, BigDecimal> categoryTotals = expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getCategory,
                            Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

            categoryNames.addAll(categoryTotals.keySet());
            categoryAmounts.addAll(categoryNames.stream().map(categoryTotals::get).collect(Collectors.toList()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("ru"));
            Map<LocalDate, BigDecimal> dailyTotals = expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getDate,
                            Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

            // Построение непрерывного графика за последние 14 дней
            LocalDate today = LocalDate.now();
            for (int i = 13; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                trendLabels.add(d.format(formatter));
                trendData.add(dailyTotals.getOrDefault(d, BigDecimal.ZERO));
            }

            expenses.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));
        }

        List<Expense> recentExpenses = (expenses != null && !expenses.isEmpty())
                ? expenses.stream().limit(5).collect(Collectors.toList())
                : new ArrayList<>();

        model.addAttribute("expenses", recentExpenses);
        model.addAttribute("expenseCount", expenses != null ? expenses.size() : 0);
        model.addAttribute("total", total);
        model.addAttribute("average", average);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categoryAmounts", categoryAmounts);
        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendData", trendData);
        model.addAttribute("currentUser", user);

        return "dashboard";
    }

    @GetMapping("/expenses")
    public String listExpenses(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Expense> expenses = expenseRepository.findByUser(user);
        expenses.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyTotal = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == LocalDate.now().getMonthValue()
                        && e.getDate().getYear() == LocalDate.now().getYear())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCheck = expenses.isEmpty() ? BigDecimal.ZERO :
                total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        String topCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        model.addAttribute("expenses", expenses);
        model.addAttribute("total", total);
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("avgCheck", avgCheck);
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("currentUser", user);

        return "expenses";
    }

    // ИСПРАВЛЕНИЕ: Вернули метод для отображения страницы добавления расхода!
    @GetMapping("/expenses/add")
    public String showAddForm(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Expense expense = new Expense();
        expense.setDate(LocalDate.now()); // Текущая дата по умолчанию

        model.addAttribute("expense", expense);
        model.addAttribute("currentUser", user);
        return "expense-form";
    }

    @PostMapping("/expenses")
    public String saveExpense(@Valid @ModelAttribute Expense expense, BindingResult result,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              RedirectAttributes redirectAttributes, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (result.hasErrors()) {
            model.addAttribute("currentUser", user);
            return "expense-form";
        }

        if (expense.getId() != null) {
            Expense existing = expenseRepository.findById(expense.getId())
                    .orElseThrow(() -> new RuntimeException("Расход не найден"));

            if (!existing.getUser().getId().equals(user.getId())) {
                return "redirect:/expenses";
            }

            existing.setAmount(expense.getAmount());
            existing.setDate(expense.getDate());
            existing.setCategory(expense.getCategory());
            existing.setDescription(expense.getDescription());
            expenseRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Расход успешно обновлен!");
        } else {
            expense.setUser(user);
            if(expense.getDate() == null) expense.setDate(LocalDate.now());
            expenseRepository.save(expense);
            redirectAttributes.addFlashAttribute("success", "Новый расход успешно добавлен!");
            redirectAttributes.addFlashAttribute("celebrate", true);
        }

        return "redirect:/expenses";
    }

    @GetMapping("/expenses/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Расход не найден"));

        if (!expense.getUser().getId().equals(user.getId())) {
            return "redirect:/expenses";
        }

        model.addAttribute("expense", expense);
        model.addAttribute("currentUser", user);
        return "expense-form";
    }

    @GetMapping("/expenses/delete/{id}")
    public String deleteExpense(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        Expense expense = expenseRepository.findById(id).orElse(null);

        if (expense != null && expense.getUser().getId().equals(user.getId())) {
            expenseRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Расход удален");
        }
        return "redirect:/expenses";
    }
}