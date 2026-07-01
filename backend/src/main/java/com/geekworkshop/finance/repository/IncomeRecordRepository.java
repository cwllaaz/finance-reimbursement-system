package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.IncomeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {
    @Query("""
        select r from IncomeRecord r
        left join fetch r.department
        left join fetch r.createdBy
        order by r.receiptDate desc, r.createdAt desc
        """)
    List<IncomeRecord> findAllDetails();

    @Query("""
        select r from IncomeRecord r
        left join fetch r.department
        left join fetch r.createdBy
        where r.id = :id
        """)
    Optional<IncomeRecord> findDetailById(@Param("id") Long id);

    Optional<IncomeRecord> findTopByIncomeNumberStartingWithOrderByIncomeNumberDesc(String prefix);
}
