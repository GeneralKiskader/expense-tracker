package com.example.demo.controller;

import com.example.demo.dto.ExpenseResponseDto;
import com.example.demo.model.UserDetailsImpl;
import com.example.demo.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/expenses")
    public String listExpenses(Model model,
                               @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ExpenseResponseDto> expenses = expenseService.getAllExpensesByUser(userDetails.getUsername());
        model.addAttribute("expenses", expenses);
        model.addAttribute("currentUser", userDetails.getUser());

        return "expenses";
    }
}