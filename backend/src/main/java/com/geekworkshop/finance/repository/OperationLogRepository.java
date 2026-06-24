package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query("""
            select log from OperationLog log
            where (:module is null or log.module = :module)
              and (:action is null or log.action = :action)
              and (
                :keyword is null
                or lower(coalesce(log.username, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(log.realName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(log.targetName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(log.detail, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(log.ipAddress, '')) like lower(concat('%', :keyword, '%'))
              )
            order by log.createdAt desc, log.id desc
            """)
    List<OperationLog> search(
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("action") String action
    );
}
