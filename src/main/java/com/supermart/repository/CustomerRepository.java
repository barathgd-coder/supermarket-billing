package com.supermart.repository;

import com.supermart.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByPhone(String phone);

    @Query("select c from Customer c where lower(c.name) like lower(concat('%', :q, '%')) "
            + "or coalesce(c.phone, '') like concat('%', :q, '%')")
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
