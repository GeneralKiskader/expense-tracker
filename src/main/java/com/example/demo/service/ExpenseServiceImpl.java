package com.example.demo.service.impl;

import com.example.demo.dto.ExpenseRequestDto;
import com.example.demo.dto.ExpenseResponseDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ExpenseMapper;
import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    public ExpenseResponseDto createExpense(ExpenseRequestDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + email));

        var expense = expenseMapper.toEntity(dto, user);
        var saved = expenseRepository.save(expense);
        return expenseMapper.toResponseDto(saved);
    }

    @Override
    public List<ExpenseResponseDto> getAllExpensesByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + email));

        return expenseRepository.findByUser(user).stream()
                .map(expenseMapper::toResponseDto)
                .toList();
    }

    @Override
    public void deleteExpense(Long id, String username) {
        // Находим расход
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Расход не найден"));

        // Проверяем, что расход принадлежит текущему пользователю
        if (!expense.getUser().getEmail().equals(username)) {
            throw new RuntimeException("У вас нет прав на удаление этого расхода");
        }

        expenseRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> getDashboardStats(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<Expense> expenses = expenseRepository.findByUser(user);

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double averageAmount = expenses.isEmpty() ? 0 :
                totalAmount.doubleValue() / expenses.size();

        // Группировка по категориям
        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        List<String> categoryNames = new ArrayList<>(categoryTotals.keySet());
        List<BigDecimal> categoryAmounts = categoryNames.stream()
                .map(categoryTotals::get)
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAmount", totalAmount);
        stats.put("expenseCount", expenses.size());
        stats.put("averageAmount", BigDecimal.valueOf(averageAmount).setScale(2, RoundingMode.HALF_UP));
        stats.put("categoryNames", categoryNames);
        stats.put("categoryAmounts", categoryAmounts);

        return stats;
    }
}