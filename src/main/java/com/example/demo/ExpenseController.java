package com.example.demo;

import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class ExpenseController {

    @Autowired
    private ExpenseService service;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/expenses")
    public String showExpenses(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User currentUser = userDetails.getUser();
        model.addAttribute("expenses", service.findAllByUser(currentUser));
        model.addAttribute("total", service.getTotalAmount(currentUser));
        model.addAttribute("byCategory", service.getTotalByCategory(currentUser));
        model.addAttribute("currentUser", currentUser);
        return "expenses";
    }

    @GetMapping("/expenses/add")
    public String showAddForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User currentUser = userDetails.getUser();
        Expense expense = new Expense();
        expense.setUser(currentUser);
        model.addAttribute("expense", expense);
        model.addAttribute("currentUser", currentUser);
        return "expense-form";
    }

    @PostMapping("/expenses")
    public String saveExpense(@Valid @ModelAttribute Expense expense,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              Model model) {
        User currentUser = userDetails.getUser();
        if (result.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            return "expense-form";
        }
        expense.setUser(currentUser);
        service.save(expense);
        return "redirect:/expenses";
    }

    @GetMapping("/expenses/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetailsImpl userDetails,
                               Model model) {
        User currentUser = userDetails.getUser();
        Expense expense = service.findById(id);
        if (!expense.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/expenses";
        }
        model.addAttribute("expense", expense);
        model.addAttribute("currentUser", currentUser);
        return "expense-form";
    }

    @GetMapping("/expenses/delete/{id}")
    public String deleteExpense(@PathVariable("id") Long id,
                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = userDetails.getUser();
        Expense expense = service.findById(id);
        if (expense.getUser().getId().equals(currentUser.getId())) {
            service.deleteById(id);
        }
        return "redirect:/expenses";
    }
}