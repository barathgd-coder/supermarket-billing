package com.supermart.web;

import com.supermart.dto.CustomerForm;
import com.supermart.exception.BusinessException;
import com.supermart.model.Customer;
import com.supermart.model.Sale;
import com.supermart.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final int PAGE_SIZE = 10;

    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long edit,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        CustomerForm form = edit != null ? customerService.toForm(edit) : new CustomerForm();
        return listView(model, q, page, form);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") CustomerForm form, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (!result.hasFieldErrors("phone") && form.getPhone() != null
                && customerService.phoneTaken(form.getPhone(), form.getId())) {
            result.rejectValue("phone", "duplicate", "A customer with this phone number already exists");
        }
        if (result.hasErrors()) {
            return listView(model, null, 0, form);
        }
        try {
            customerService.save(form);
        } catch (BusinessException ex) {
            result.reject("business", ex.getMessage());
            return listView(model, null, 0, form);
        }
        redirect.addFlashAttribute("success", "Customer saved");
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        List<Sale> sales = customerService.purchaseHistory(id);
        model.addAttribute("customer", customer);
        model.addAttribute("sales", sales);
        model.addAttribute("totalSpent", customerService.totalSpent(sales));
        return "customers/detail";
    }

    private String listView(Model model, String q, int page, CustomerForm form) {
        Page<Customer> result = customerService.search(q, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("form", form);
        model.addAttribute("baseQuery", QueryUtil.of("q", q));
        return "customers/list";
    }
}
