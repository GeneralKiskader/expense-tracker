package com.example.demo.repository;

import com.example.demo.model.Budget;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser(User user);
    Optional<Budget> findByUserAndCategoryAndType(User user, String category, String type);
}