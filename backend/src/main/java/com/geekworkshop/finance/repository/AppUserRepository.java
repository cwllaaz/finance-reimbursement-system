package com.geekworkshop.finance.repository;

import com.geekworkshop.finance.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("""
            select u from AppUser u
            left join fetch u.department
            order by u.id asc
            """)
    List<AppUser> findAllWithDepartment();

    @Query("""
            select u from AppUser u
            left join fetch u.department
            where u.id = :id
            """)
    Optional<AppUser> findWithDepartmentById(@Param("id") Long id);

    @Query("""
            select u from AppUser u
            left join fetch u.department
            where u.username = :username
            """)
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
