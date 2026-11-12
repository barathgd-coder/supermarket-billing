package com.supermart.repository;

import com.supermart.model.InventoryTransaction;
import com.supermart.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query(value = "select t from InventoryTransaction t where lower(t.product.name) like lower(concat('%', :q, '%')) "
            + "or lower(t.product.barcode) like lower(concat('%', :q, '%')) order by t.createdAt desc, t.id desc",
            countQuery = "select count(t) from InventoryTransaction t where lower(t.product.name) like lower(concat('%', :q, '%')) "
                    + "or lower(t.product.barcode) like lower(concat('%', :q, '%'))")
    Page<InventoryTransaction> search(@Param("q") String q, Pageable pageable);

    @Query("select t from InventoryTransaction t where t.product.supplier.id = :supplierId and t.type = :type "
            + "order by t.createdAt desc, t.id desc")
    List<InventoryTransaction> findBySupplierAndType(@Param("supplierId") Long supplierId,
                                                     @Param("type") TransactionType type);

    long countByProductId(Long productId);
}
