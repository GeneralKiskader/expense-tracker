package com.example.demo.mapper;

import com.example.demo.dto.ExpenseRequestDto;
import com.example.demo.dto.ExpenseResponseDto;
import com.example.demo.model.Expense;
import com.example.demo.model.User;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public Expense toEntity(ExpenseRequestDto dto, User user) {
        if (dto == null) return null;
        return Expense.builder()
                .amount(dto.getAmount())
                .date(dto.getDate())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .user(user)
                .build();
    }

    public ExpenseResponseDto toResponseDto(Expense expense) {
        if (expense == null) return null;
        return ExpenseResponseDto.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .userId(expense.getUser() != null ? expense.getUser().getId() : null)
                .build();
    }
}