package com.example.demo.service;

import com.example.demo.model.Expense;
import com.example.demo.model.User;
import com.example.demo.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository repository;

    public List<Expense> findAllByUser(User user) {
        return repository.findByUser(user);
    }

    public void save(Expense expense) {
        repository.save(expense);
    }

    public Expense findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid expense Id:" + id));
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public BigDecimal getTotalAmount(User user) {
        return findAllByUser(user).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> getTotalByCategory(User user) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Expense e : findAllByUser(user)) {
            map.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return map;
    }
}