package com.example.demo.service;

import com.example.demo.dto.ExpenseRequestDto;
import com.example.demo.dto.ExpenseResponseDto;

import java.util.List;

public interface ExpenseService {
    ExpenseResponseDto createExpense(ExpenseRequestDto dto, String email);
    List<ExpenseResponseDto> getAllExpensesByUser(String email);
}