package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BudgetController {

    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final GoalRepository goalRepository;

    @GetMapping("/budgets")
    public String showBudgets(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        LocalDate now = LocalDate.now();

        // 1. Получаем все данные за текущий месяц
        List<Budget> allBudgets = budgetRepository.findByUser(user);
        List<Expense> monthlyExpenses = expenseRepository.findByUser(user).stream()
                .filter(e -> e.getDate().getMonthValue() == now.getMonthValue() && e.getDate().getYear() == now.getYear())
                .toList();
        List<Income> monthlyIncomes = incomeRepository.findByUser(user).stream()
                .filter(i -> i.getDate().getMonthValue() == now.getMonthValue() && i.getDate().getYear() == now.getYear())
                .toList();
        List<Goal> goals = goalRepository.findByUser(user);

        // 2. Статистика по РАСХОДАМ (План/Факт)
        List<BudgetStat> expenseStats = new ArrayList<>();
        BigDecimal totalPlannedExp = BigDecimal.ZERO;
        for (Budget b : allBudgets.stream().filter(b -> "EXPENSE".equals(b.getType())).toList()) {
            BigDecimal spent = monthlyExpenses.stream()
                    .filter(e -> e.getCategory().equals(b.getCategory()))
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPlannedExp = totalPlannedExp.add(b.getPlannedAmount());
            int percent = b.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0 ?
                    spent.multiply(new BigDecimal(100)).divide(b.getPlannedAmount(), 0, RoundingMode.HALF_UP).intValue() : 0;
            expenseStats.add(new BudgetStat(b.getId(), b.getCategory(), b.getPlannedAmount(), spent, percent));
        }

        // 3. Статистика по ДОХОДАМ (План/Факт)
        List<BudgetStat> incomeStats = new ArrayList<>();
        BigDecimal totalPlannedInc = BigDecimal.ZERO;
        for (Budget b : allBudgets.stream().filter(b -> "INCOME".equals(b.getType())).toList()) {
            BigDecimal earned = monthlyIncomes.stream()
                    .filter(i -> i.getCategory().equals(b.getCategory()))
                    .map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPlannedInc = totalPlannedInc.add(b.getPlannedAmount());
            int percent = b.getPlannedAmount().compareTo(BigDecimal.ZERO) > 0 ?
                    earned.multiply(new BigDecimal(100)).divide(b.getPlannedAmount(), 0, RoundingMode.HALF_UP).intValue() : 0;
            incomeStats.add(new BudgetStat(b.getId(), b.getCategory(), b.getPlannedAmount(), earned, percent));
        }

        // 4. Общие итоги и Аналитика (Экономия/Перерасход)
        BigDecimal totalActualExp = monthlyExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActualInc = monthlyIncomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Разница в расходах: План - Факт. Если > 0 = Экономия, Если < 0 = Перерасход
        BigDecimal expenseDiff = totalPlannedExp.subtract(totalActualExp);
        boolean isOverspent = expenseDiff.compareTo(BigDecimal.ZERO) < 0;

        model.addAttribute("currentUser", user);
        model.addAttribute("expenseStats", expenseStats);
        model.addAttribute("incomeStats", incomeStats);
        model.addAttribute("goals", goals);

        model.addAttribute("totalPlannedExp", totalPlannedExp);
        model.addAttribute("totalActualExp", totalActualExp);
        model.addAttribute("totalPlannedInc", totalPlannedInc);
        model.addAttribute("totalActualInc", totalActualInc);

        model.addAttribute("expenseDiff", expenseDiff.abs());
        model.addAttribute("isOverspent", isOverspent);

        return "budgets";
    }

    // Сохранение плана (бюджета) для Расходов или Доходов
    @PostMapping("/budgets")
    public String saveBudget(@AuthenticationPrincipal UserDetailsImpl userDetails,
                             @RequestParam String type,
                             @RequestParam String category,
                             @RequestParam BigDecimal plannedAmount,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();

        Budget budget = budgetRepository.findByUserAndCategoryAndType(user, category, type).orElse(new Budget());
        budget.setUser(user);
        budget.setType(type); // "INCOME" или "EXPENSE"
        budget.setCategory(category);
        budget.setPlannedAmount(plannedAmount);
        budgetRepository.save(budget);

        redirectAttributes.addFlashAttribute("success", "План для '" + category + "' успешно сохранен!");
        return "redirect:/budgets";
    }

    // Сохранение факта ДОХОДА
    @PostMapping("/incomes")
    public String saveIncome(@AuthenticationPrincipal UserDetailsImpl userDetails,
                             @RequestParam String category,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String description,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();

        Income income = Income.builder()
                .user(user).category(category).amount(amount).description(description).date(LocalDate.now()).build();
        incomeRepository.save(income);

        redirectAttributes.addFlashAttribute("success", "Фактический доход добавлен!");
        return "redirect:/budgets";
    }

    // Быстрое пополнение Цели
    @PostMapping("/goals/fund")
    public String fundGoal(@AuthenticationPrincipal UserDetailsImpl userDetails,
                           @RequestParam Long goalId,
                           @RequestParam BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        Goal goal = goalRepository.findById(goalId).orElseThrow();

        if (goal.getUser().getId().equals(user.getId())) {
            goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
            goalRepository.save(goal);
            redirectAttributes.addFlashAttribute("success", "Цель пополнена! Вы стали на шаг ближе к мечте.");
            redirectAttributes.addFlashAttribute("celebrate", true);
        }
        return "redirect:/budgets";
    }

    @PostMapping("/goals/new")
    public String createGoal(@AuthenticationPrincipal UserDetailsImpl userDetails,
                             @RequestParam String name,
                             @RequestParam BigDecimal targetAmount,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        Goal goal = Goal.builder().user(user).name(name).targetAmount(targetAmount).currentAmount(BigDecimal.ZERO).build();
        goalRepository.save(goal);
        redirectAttributes.addFlashAttribute("success", "Новая цель создана!");
        return "redirect:/budgets";
    }

    @GetMapping("/budgets/delete/{id}")
    public String deleteBudget(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        budgetRepository.deleteById(id);
        return "redirect:/budgets";
    }

    @Getter @Setter @AllArgsConstructor
    public static class BudgetStat {
        private Long id;
        private String category;
        private BigDecimal planned;
        private BigDecimal actual;
        private int percent;
    }
}