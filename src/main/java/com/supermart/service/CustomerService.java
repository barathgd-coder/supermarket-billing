package com.supermart.service;

import com.supermart.dto.CustomerForm;
import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.Customer;
import com.supermart.model.Sale;
import com.supermart.repository.CustomerRepository;
import com.supermart.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, int page, int size) {
        return customerRepository.search(q == null ? "" : q.trim(),
                PageRequest.of(Math.max(page, 0), size, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional(readOnly = true)
    public CustomerForm toForm(Long id) {
        Customer c = get(id);
        CustomerForm f = new CustomerForm();
        f.setId(c.getId());
        f.setName(c.getName());
        f.setPhone(c.getPhone());
        return f;
    }

    @Transactional(readOnly = true)
    public boolean phoneTaken(String phone, Long exceptId) {
        String p = phone == null ? "" : phone.trim();
        return exceptId == null ? customerRepository.existsByPhone(p) : customerRepository.existsByPhoneAndIdNot(p, exceptId);
    }

    @Transactional
    public Customer save(CustomerForm f) {
        if (phoneTaken(f.getPhone(), f.getId())) {
            throw new BusinessException("A customer with phone " + f.getPhone().trim() + " already exists");
        }
        Customer c = f.getId() == null ? new Customer() : get(f.getId());
        c.setName(f.getName().trim());
        c.setPhone(f.getPhone().trim());
        return customerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Sale> purchaseHistory(Long customerId) {
        return saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId);
    }

    public BigDecimal totalSpent(List<Sale> sales) {
        return sales.stream().map(Sale::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
