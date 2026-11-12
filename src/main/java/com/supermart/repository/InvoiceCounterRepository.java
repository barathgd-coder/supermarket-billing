package com.supermart.repository;

import com.supermart.model.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InvoiceCounter c where c.dateKey = :key")
    Optional<InvoiceCounter> lockByDateKey(@Param("key") String key);
}
