package com.supermart.web;

import com.supermart.dto.ProductForm;
import com.supermart.exception.BusinessException;
import com.supermart.model.Product;
import com.supermart.service.CategoryService;
import com.supermart.service.ProductService;
import com.supermart.service.SettingService;
import com.supermart.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final SettingService settingService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long category,
                       @RequestParam(required = false) Long supplier,
                       @RequestParam(required = false) String stock,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Product> result = productService.search(q, category, supplier, stock, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("supplier", supplier);
        model.addAttribute("stock", stock);
        model.addAttribute("categories", categoryService.list());
        model.addAttribute("suppliers", supplierService.list());
        model.addAttribute("baseQuery", QueryUtil.of("q", q, "category", QueryUtil.str(category),
                "supplier", QueryUtil.str(supplier), "stock", stock));
        return "products/list";
    }

    /** Printable labels: every product with its barcode and QR code (QR text = the product's barcode). */
    @GetMapping("/labels")
    public String labels(@RequestParam(required = false) String q, Model model) {
        Page<Product> result = productService.search(q, null, null, null, 0, 500);
        model.addAttribute("products", result.getContent());
        model.addAttribute("q", q);
        return "products/labels";
    }

    @GetMapping("/new")
    public String create(Model model) {
        ProductForm form = new ProductForm();
        form.setQuantity(0);
        form.setGstPercent(settingService.defaultGst());
        form.setReorderLevel(settingService.defaultReorder());
        return formView(model, form);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return formView(model, productService.toForm(id));
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
                       Model model, RedirectAttributes redirect, Principal principal) {
        if (!result.hasFieldErrors("barcode") && form.getBarcode() != null
                && productService.barcodeTaken(form.getBarcode(), form.getId())) {
            result.rejectValue("barcode", "duplicate", "This barcode / product ID already exists");
        }
        if (result.hasErrors()) {
            return formView(model, form);
        }
        try {
            productService.save(form, principal.getName());
        } catch (BusinessException ex) {
            result.reject("business", ex.getMessage());
            return formView(model, form);
        }
        redirect.addFlashAttribute("success", "Product saved successfully");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        productService.delete(id);
        redirect.addFlashAttribute("success", "Product deleted");
        return "redirect:/products";
    }

    private String formView(Model model, ProductForm form) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.list());
        model.addAttribute("suppliers", supplierService.list());
        return "products/form";
    }
}
