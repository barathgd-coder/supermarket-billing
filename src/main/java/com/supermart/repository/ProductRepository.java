package com.supermart.repository;

import com.supermart.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    /** Row lock used while billing / adjusting stock so two cashiers cannot oversell the same item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    long countByActiveTrue();

    @Query("select count(p) from Product p where p.active = true and p.quantity <= p.reorderLevel")
    long countLowStock();

    @Query("select p from Product p where p.active = true and p.quantity <= p.reorderLevel order by p.quantity asc, p.name asc")
    List<Product> findLowStock();

    List<Product> findByActiveTrueAndExpiryDateBeforeOrderByExpiryDateAsc(LocalDate date);

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findBySupplierIdAndActiveTrueOrderByNameAsc(Long supplierId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySupplierId(Long supplierId);

    @Query("select p from Product p where p.active = true and "
            + "(lower(p.name) like lower(concat('%', :q, '%')) or lower(p.barcode) like lower(concat(:q, '%'))) "
            + "order by p.name")
    List<Product> searchForPos(@Param("q") String q, Pageable pageable);
}
