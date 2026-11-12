package com.supermart.web;

import com.supermart.model.Sale;
import com.supermart.service.InvoicePdfService;
import com.supermart.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private static final int PAGE_SIZE = 10;

    private final SaleService saleService;
    private final InvoicePdfService pdfService;

    @GetMapping
    public String history(@RequestParam(required = false) String invoice,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        Page<Sale> result = saleService.search(invoice, from, to, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("invoice", invoice);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("total", saleService.totalFor(invoice, from, to));
        model.addAttribute("baseQuery", QueryUtil.of("invoice", invoice, "from", QueryUtil.str(from), "to", QueryUtil.str(to)));
        return "sales/list";
    }

    @GetMapping("/{id}/invoice")
    public String invoice(@PathVariable Long id, Model model,
                          @RequestParam(name = "new", required = false) String isNew) {
        model.addAttribute("sale", saleService.get(id));
        model.addAttribute("justCreated", isNew != null);
        return "sales/invoice";
    }

    @GetMapping("/{id}/invoice.pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Sale sale = saleService.get(id);
        byte[] data = pdfService.generate(sale);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(sale.getInvoiceNumber() + ".pdf").build().toString())
                .body(data);
    }
}
