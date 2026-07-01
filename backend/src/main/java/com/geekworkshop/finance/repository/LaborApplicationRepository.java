package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.LaborApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LaborApplicationRepository extends JpaRepository<LaborApplication, Long> {
    @Query("""
        select distinct a from LaborApplication a
        left join fetch a.applicant
        left join fetch a.department
        left join fetch a.recipients
        order by a.createdAt desc
        """)
    List<LaborApplication> findAllDetails();

    @Query("""
        select distinct a from LaborApplication a
        left join fetch a.applicant
        left join fetch a.department
        left join fetch a.recipients
        where a.id = :id
        """)
    Optional<LaborApplication> findDetailById(@Param("id") Long id);

    Optional<LaborApplication> findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(String prefix);
    Optional<LaborApplication> findByApplicationNumber(String applicationNumber);
}
