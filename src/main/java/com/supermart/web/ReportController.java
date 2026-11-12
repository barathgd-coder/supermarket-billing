package com.supermart.web;

import com.supermart.dto.ReportResult;
import com.supermart.service.ReportService;
import com.supermart.service.ReportService.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String page(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("types", ReportType.values());
        model.addAttribute("from", today.minusDays(29));
        model.addAttribute("to", today);
        return "reports/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public ReportResult data(@RequestParam ReportType type,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.generate(type, from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam ReportType type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ReportResult report = reportService.generate(type, from, to);
        byte[] csv = ("\uFEFF" + reportService.toCsv(report)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("report-" + type.name().toLowerCase() + ".csv").build().toString())
                .body(csv);
    }
}
