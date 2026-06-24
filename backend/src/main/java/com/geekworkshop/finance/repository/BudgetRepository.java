package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("""
            select b from Budget b
            left join fetch b.department
            order by b.budgetYear desc, b.department.id asc
            """)
    List<Budget> findAllWithDepartment();

    @Query("""
            select b from Budget b
            left join fetch b.department
            where b.id = :id
            """)
    Optional<Budget> findDetailById(@Param("id") Long id);

    Optional<Budget> findByDepartmentIdAndBudgetYear(Long departmentId, Integer budgetYear);
}
