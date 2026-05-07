package com.example.demo.service;

import com.example.demo.dto.ExpenseRequestDto;
import com.example.demo.dto.ExpenseResponseDto;

import com.example.demo.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;

public interface ExpenseService {
    ExpenseResponseDto createExpense(ExpenseRequestDto dto, String email);
    List<ExpenseResponseDto> getAllExpensesByUser(String email);
    void deleteExpense(Long id, String username);
    Map<String, Object> getDashboardStats(String username);
}
