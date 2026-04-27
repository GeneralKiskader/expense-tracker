package com.example.demo.service.impl;

import com.example.demo.dto.ExpenseRequestDto;
import com.example.demo.dto.ExpenseResponseDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ExpenseMapper;
import com.example.demo.model.User;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}