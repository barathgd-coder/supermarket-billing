package com.supermart.service;

import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.Category;
import com.supermart.repository.CategoryRepository;
import com.supermart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Category> list() { return categoryRepository.findAllByOrderByNameAsc(); }

    @Transactional
    public Category add(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new BusinessException("Category name is required");
        if (n.length() > 80) throw new BusinessException("Category name is too long");
        if (categoryRepository.existsByNameIgnoreCase(n)) throw new BusinessException("Category '" + n + "' already exists");
        return categoryRepository.save(new Category(n));
    }

    @Transactional
    public void delete(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("Category '" + c.getName() + "' is used by products and cannot be deleted");
        }
        categoryRepository.delete(c);
    }
}
