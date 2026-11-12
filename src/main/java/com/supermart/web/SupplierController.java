package com.supermart.web;

import com.supermart.dto.SupplierForm;
import com.supermart.exception.BusinessException;
import com.supermart.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public String list(@RequestParam(required = false) Long edit, Model model) {
        return listView(model, edit != null ? supplierService.toForm(edit) : new SupplierForm());
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") SupplierForm form, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return listView(model, form);
        }
        supplierService.save(form);
        redirect.addFlashAttribute("success", "Supplier saved");
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            supplierService.delete(id);
            redirect.addFlashAttribute("success", "Supplier deleted");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", supplierService.get(id));
        model.addAttribute("products", supplierService.productsOf(id));
        model.addAttribute("history", supplierService.purchaseHistory(id));
        return "suppliers/detail";
    }

    private String listView(Model model, SupplierForm form) {
        model.addAttribute("suppliers", supplierService.list());
        model.addAttribute("form", form);
        return "suppliers/list";
    }
}
