package com.supermart.api;

import com.supermart.dto.CheckoutRequest;
import com.supermart.dto.CheckoutResponse;
import com.supermart.dto.ProductLookup;
import com.supermart.model.Sale;
import com.supermart.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** JSON endpoints used by the POS screen (/billing). */
@RestController
@RequestMapping("/billing/api")
@RequiredArgsConstructor
public class BillingApiController {

    private final BillingService billingService;

    @GetMapping("/products")
    public List<ProductLookup> search(@RequestParam("q") String q) {
        return billingService.search(q);
    }

    @GetMapping("/customers/lookup")
    public Map<String, String> customer(@RequestParam("phone") String phone) {
        return billingService.lookupCustomer(phone)
                .map(c -> Map.of("name", c.getName()))
                .orElse(Map.of());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request, Principal principal) {
        Sale sale = billingService.checkout(request, principal.getName());
        return new CheckoutResponse(sale.getId(), sale.getInvoiceNumber(), sale.getGrandTotal(),
                "/sales/" + sale.getId() + "/invoice?new=1");
    }
}
