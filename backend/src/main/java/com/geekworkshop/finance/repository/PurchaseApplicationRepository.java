package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.PurchaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PurchaseApplicationRepository extends JpaRepository<PurchaseApplication, Long> {
    @Query("""
        select distinct p from PurchaseApplication p
        left join fetch p.applicant
        left join fetch p.department
        left join fetch p.items
        order by p.createdAt desc
        """)
    List<PurchaseApplication> findAllDetails();

    @Query("""
        select distinct p from PurchaseApplication p
        left join fetch p.applicant
        left join fetch p.department
        left join fetch p.items
        where p.id = :id
        """)
    Optional<PurchaseApplication> findDetailById(@Param("id") Long id);

    Optional<PurchaseApplication> findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(String prefix);
    Optional<PurchaseApplication> findByApplicationNumber(String applicationNumber);
}
