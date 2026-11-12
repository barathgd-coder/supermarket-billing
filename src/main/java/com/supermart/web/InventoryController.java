package com.supermart.web;

import com.supermart.dto.StockEntryForm;
import com.supermart.model.InventoryTransaction;
import com.supermart.model.Product;
import com.supermart.service.InventoryService;
import com.supermart.service.ProductService;
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
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final int PAGE_SIZE = 10;

    private final InventoryService inventoryService;
    private final ProductService productService;

    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String stock,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Product> result = productService.search(q, null, null, stock, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("stock", stock);
        model.addAttribute("baseQuery", QueryUtil.of("q", q, "stock", stock));
        model.addAttribute("lowStock", inventoryService.lowStock());
        model.addAttribute("expired", inventoryService.expired());
        model.addAttribute("products", productService.allActive());
        model.addAttribute("entry", new StockEntryForm());
        return "inventory/index";
    }

    @PostMapping("/entry")
    public String entry(@Valid @ModelAttribute("entry") StockEntryForm form, BindingResult result,
                        RedirectAttributes redirect, Principal principal) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/inventory";
        }
        inventoryService.enterStock(form, principal.getName());
        redirect.addFlashAttribute("success", form.getType().name().equals("STOCK_IN")
                ? "Stock added successfully" : "Stock removed successfully");
        return "redirect:/inventory";
    }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        Page<InventoryTransaction> result = inventoryService.history(q, page, 15);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("baseQuery", QueryUtil.of("q", q));
        return "inventory/history";
    }
}
