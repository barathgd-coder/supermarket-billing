package com.supermart.web;

import com.supermart.model.PaymentMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BillingController {

    @GetMapping("/billing")
    public String pos(Model model) {
        model.addAttribute("methods", PaymentMethod.values());
        return "billing/pos";
    }
}
