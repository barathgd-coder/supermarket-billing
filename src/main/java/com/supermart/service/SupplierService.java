package com.supermart.service;

import com.supermart.dto.SupplierForm;
import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.InventoryTransaction;
import com.supermart.model.Product;
import com.supermart.model.Supplier;
import com.supermart.model.TransactionType;
import com.supermart.repository.InventoryTransactionRepository;
import com.supermart.repository.ProductRepository;
import com.supermart.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Supplier> list() { return supplierRepository.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    @Transactional(readOnly = true)
    public SupplierForm toForm(Long id) {
        Supplier s = get(id);
        SupplierForm f = new SupplierForm();
        f.setId(s.getId());
        f.setName(s.getName());
        f.setContactNumber(s.getContactNumber());
        f.setEmail(s.getEmail());
        f.setAddress(s.getAddress());
        return f;
    }

    @Transactional
    public Supplier save(SupplierForm f) {
        Supplier s = f.getId() == null ? new Supplier() : get(f.getId());
        s.setName(f.getName().trim());
        s.setContactNumber(blankToNull(f.getContactNumber()));
        s.setEmail(blankToNull(f.getEmail()));
        s.setAddress(blankToNull(f.getAddress()));
        return supplierRepository.save(s);
    }

    @Transactional
    public void delete(Long id) {
        Supplier s = get(id);
        if (productRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier '" + s.getName() + "' still supplies products and cannot be deleted");
        }
        supplierRepository.delete(s);
    }

    @Transactional(readOnly = true)
    public List<Product> productsOf(Long id) {
        return productRepository.findBySupplierIdAndActiveTrueOrderByNameAsc(id);
    }

    /** Purchase history = stock-in entries for the products this supplier provides. */
    @Transactional(readOnly = true)
    public List<InventoryTransaction> purchaseHistory(Long id) {
        return transactionRepository.findBySupplierAndType(id, TransactionType.STOCK_IN);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
