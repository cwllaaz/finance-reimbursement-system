package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AdvanceApplication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AdvanceApplicationRepository extends JpaRepository<AdvanceApplication, Long> {
    @Query("""
        select a from AdvanceApplication a
        left join fetch a.applicant
        left join fetch a.department
        order by a.createdAt desc
        """)
    List<AdvanceApplication> findAllDetails();
    @Query("""
        select a from AdvanceApplication a
        left join fetch a.applicant
        left join fetch a.department
        where a.id = :id
        """)
    Optional<AdvanceApplication> findDetailById(@Param("id") Long id);
    Optional<AdvanceApplication> findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(String prefix);
    Optional<AdvanceApplication> findByApplicationNumber(String applicationNumber);
}
