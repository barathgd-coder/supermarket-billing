package com.supermart.service;

import com.supermart.dto.ProductForm;
import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.*;
import com.supermart.repository.CategoryRepository;
import com.supermart.repository.ProductRepository;
import com.supermart.repository.SupplierRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public Page<Product> search(String q, Long categoryId, Long supplierId, String stock, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            Class<?> rt = query.getResultType();
            if (rt != Long.class && rt != long.class) {
                root.fetch("category", JoinType.INNER);
                root.fetch("supplier", JoinType.LEFT);
            }
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("barcode")), like)));
            }
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (supplierId != null) ps.add(cb.equal(root.get("supplier").get("id"), supplierId));
            if (stock != null && !stock.isBlank()) {
                switch (stock) {
                    case "OUT" -> ps.add(cb.le(root.<Integer>get("quantity"), 0));
                    case "LOW" -> {
                        ps.add(cb.greaterThan(root.<Integer>get("quantity"), 0));
                        ps.add(cb.le(root.<Integer>get("quantity"), root.<Integer>get("reorderLevel")));
                    }
                    case "IN" -> ps.add(cb.greaterThan(root.<Integer>get("quantity"), root.<Integer>get("reorderLevel")));
                    case "EXPIRED" -> ps.add(cb.lessThan(root.<LocalDate>get("expiryDate"), LocalDate.now()));
                    default -> { }
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, PageRequest.of(Math.max(page, 0), size, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public List<Product> allActive() { return productRepository.findByActiveTrueOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public boolean barcodeTaken(String barcode, Long exceptId) {
        String b = barcode == null ? "" : barcode.trim();
        return exceptId == null ? productRepository.existsByBarcode(b) : productRepository.existsByBarcodeAndIdNot(b, exceptId);
    }

    @Transactional(readOnly = true)
    public ProductForm toForm(Long id) {
        Product p = get(id);
        ProductForm f = new ProductForm();
        f.setId(p.getId());
        f.setBarcode(p.getBarcode());
        f.setName(p.getName());
        f.setCategoryId(p.getCategory().getId());
        f.setSupplierId(p.getSupplier() == null ? null : p.getSupplier().getId());
        f.setPurchasePrice(p.getPurchasePrice());
        f.setSellingPrice(p.getSellingPrice());
        f.setQuantity(p.getQuantity());
        f.setGstPercent(p.getGstPercent());
        f.setExpiryDate(p.getExpiryDate());
        f.setReorderLevel(p.getReorderLevel());
        return f;
    }

    @Transactional
    public Product save(ProductForm f, String user) {
        String barcode = f.getBarcode().trim();
        if (barcodeTaken(barcode, f.getId())) {
            throw new BusinessException("A product with barcode '" + barcode + "' already exists");
        }
        Category category = categoryRepository.findById(f.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", f.getCategoryId()));
        Supplier supplier = f.getSupplierId() == null ? null : supplierRepository.findById(f.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", f.getSupplierId()));

        boolean isNew = f.getId() == null;
        Product p;
        int oldQty = 0;
        if (isNew) {
            p = new Product();
        } else {
            p = productRepository.findByIdForUpdate(f.getId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", f.getId()));
            oldQty = p.getQuantity();
        }
        p.setBarcode(barcode);
        p.setName(f.getName().trim());
        p.setCategory(category);
        p.setSupplier(supplier);
        p.setPurchasePrice(f.getPurchasePrice().setScale(2, RoundingMode.HALF_UP));
        p.setSellingPrice(f.getSellingPrice().setScale(2, RoundingMode.HALF_UP));
        p.setGstPercent(f.getGstPercent().setScale(2, RoundingMode.HALF_UP));
        p.setExpiryDate(f.getExpiryDate());
        p.setReorderLevel(f.getReorderLevel());
        p.setQuantity(oldQty);
        p = productRepository.save(p);

        int newQty = f.getQuantity();
        if (isNew && newQty > 0) {
            inventoryService.record(p, TransactionType.STOCK_IN, newQty, "Opening stock", null, user);
        } else if (!isNew && newQty != oldQty) {
            inventoryService.record(p, TransactionType.ADJUSTMENT, newQty - oldQty, "Edited on product form", null, user);
        }
        return p;
    }

    /** Soft delete: the product disappears from lists and the POS but old invoices keep working. */
    @Transactional
    public void delete(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
        p.setActive(false);
        productRepository.save(p);
    }

}
