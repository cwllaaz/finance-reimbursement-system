package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AdvanceOffsetRecord;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AdvanceOffsetRecordRepository extends JpaRepository<AdvanceOffsetRecord, Long> {
    @Query("""
        select r from AdvanceOffsetRecord r left join fetch r.operator
        where r.advanceApplication.id = :id order by r.createdAt asc
        """)
    List<AdvanceOffsetRecord> findDetailsByApplicationId(@Param("id") Long id);
    void deleteByAdvanceApplicationId(Long id);
}
