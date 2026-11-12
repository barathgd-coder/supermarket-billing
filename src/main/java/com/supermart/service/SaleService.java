package com.supermart.service;

import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.Sale;
import com.supermart.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaleService {

    private static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(2999, 1, 1, 0, 0);

    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public Page<Sale> search(String invoice, LocalDate from, LocalDate to, int page, int size) {
        return saleRepository.search(clean(invoice), start(from), end(to), PageRequest.of(Math.max(page, 0), size));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalFor(String invoice, LocalDate from, LocalDate to) {
        return saleRepository.searchAll(clean(invoice), start(from), end(to)).stream()
                .map(Sale::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Sale get(Long id) {
        Sale s = saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        s.getItems().size();   // initialise lazily loaded lines
        return s;
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }

    private static LocalDateTime start(LocalDate d) { return d == null ? MIN : d.atStartOfDay(); }

    private static LocalDateTime end(LocalDate d) { return d == null ? MAX : d.plusDays(1).atStartOfDay(); }
}
