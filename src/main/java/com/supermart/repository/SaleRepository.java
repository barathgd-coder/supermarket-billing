package com.supermart.repository;

import com.supermart.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query(value = "select s from Sale s where lower(s.invoiceNumber) like lower(concat('%', :inv, '%')) "
            + "and s.saleDate >= :from and s.saleDate < :to order by s.saleDate desc",
            countQuery = "select count(s) from Sale s where lower(s.invoiceNumber) like lower(concat('%', :inv, '%')) "
                    + "and s.saleDate >= :from and s.saleDate < :to")
    Page<Sale> search(@Param("inv") String inv, @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select s from Sale s where lower(s.invoiceNumber) like lower(concat('%', :inv, '%')) "
            + "and s.saleDate >= :from and s.saleDate < :to")
    List<Sale> searchAll(@Param("inv") String inv, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Sale> findBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(LocalDateTime from, LocalDateTime to);

    List<Sale> findTop10ByOrderBySaleDateDesc();

    List<Sale> findByCustomerIdOrderBySaleDateDesc(Long customerId);

    @Query("select distinct s from Sale s left join fetch s.items i left join fetch i.product p left join fetch p.category "
            + "where s.saleDate >= :from and s.saleDate < :to order by s.saleDate")
    List<Sale> findForReport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
