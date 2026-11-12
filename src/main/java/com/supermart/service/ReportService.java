package com.supermart.service;

import com.supermart.dto.ReportResult;
import com.supermart.model.PaymentMethod;
import com.supermart.model.Sale;
import com.supermart.model.SaleItem;
import com.supermart.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    public enum ReportType {
        DAILY, WEEKLY, MONTHLY, PRODUCT, CATEGORY, PROFIT, TAX, PAYMENT
    }

    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public ReportResult generate(ReportType type, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (end.isBefore(start)) { LocalDate t = start; start = end; end = t; }
        List<Sale> sales = saleRepository.findForReport(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        String range = " (" + start + " to " + end + ")";

        return switch (type) {
            case DAILY -> daily(sales, range);
            case WEEKLY -> weekly(sales, range);
            case MONTHLY -> monthly(sales, range);
            case PRODUCT -> product(sales, range);
            case CATEGORY -> category(sales, range);
            case PROFIT -> profit(sales, range);
            case TAX -> tax(sales, range);
            case PAYMENT -> payment(sales, range);
        };
    }

    // ---------------------------------------------------------------- sales by period

    private ReportResult daily(List<Sale> sales, String range) {
        Map<LocalDate, List<Sale>> byDay = sales.stream().collect(
                Collectors.groupingBy(s -> s.getSaleDate().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        byDay.forEach((d, list) -> rows.add(List.of(d.toString(), String.valueOf(list.size()),
                m(sum(list, Sale::getSubtotal)), m(sum(list, Sale::getDiscountAmount)),
                m(sum(list, Sale::getTaxTotal)), m(sum(list, Sale::getGrandTotal)))));
        return new ReportResult("Daily sales" + range,
                List.of("Date", "Bills", "Subtotal", "Discount", "Tax", "Total"), rows, 0, 5, "bar");
    }

    private ReportResult weekly(List<Sale> sales, String range) {
        Map<LocalDate, List<Sale>> byWeek = sales.stream().collect(Collectors.groupingBy(
                s -> s.getSaleDate().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                TreeMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        byWeek.forEach((d, list) -> rows.add(List.of("Week of " + d, String.valueOf(list.size()),
                m(sum(list, Sale::getDiscountAmount)), m(sum(list, Sale::getTaxTotal)), m(sum(list, Sale::getGrandTotal)))));
        return new ReportResult("Weekly sales" + range,
                List.of("Week", "Bills", "Discount", "Tax", "Total"), rows, 0, 4, "bar");
    }

    private ReportResult monthly(List<Sale> sales, String range) {
        Map<YearMonth, List<Sale>> byMonth = sales.stream().collect(Collectors.groupingBy(
                s -> YearMonth.from(s.getSaleDate()), TreeMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        byMonth.forEach((ym, list) -> rows.add(List.of(ym.toString(), String.valueOf(list.size()),
                m(sum(list, Sale::getDiscountAmount)), m(sum(list, Sale::getTaxTotal)), m(sum(list, Sale::getGrandTotal)))));
        return new ReportResult("Monthly sales" + range,
                List.of("Month", "Bills", "Discount", "Tax", "Total"), rows, 0, 4, "bar");
    }

    // ---------------------------------------------------------------- product / category

    private static final class Agg {
        long qty;
        BigDecimal net = BigDecimal.ZERO;   // line total without tax
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
    }

    private ReportResult product(List<Sale> sales, String range) {
        Map<String, Agg> map = new HashMap<>();
        for (Sale s : sales) {
            for (SaleItem i : s.getItems()) add(map.computeIfAbsent(i.getProductName(), k -> new Agg()), i);
        }
        return new ReportResult("Product-wise sales" + range,
                List.of("Product", "Qty sold", "Net sales (excl. tax)"), ranked(map), 0, 2, "bar");
    }

    private ReportResult category(List<Sale> sales, String range) {
        Map<String, Agg> map = new HashMap<>();
        for (Sale s : sales) {
            for (SaleItem i : s.getItems()) {
                String cat = i.getProduct() != null && i.getProduct().getCategory() != null
                        ? i.getProduct().getCategory().getName() : "Uncategorised";
                add(map.computeIfAbsent(cat, k -> new Agg()), i);
            }
        }
        return new ReportResult("Category-wise sales" + range,
                List.of("Category", "Qty sold", "Net sales (excl. tax)"), ranked(map), 0, 2, "pie");
    }

    private void add(Agg a, SaleItem i) {
        a.qty += i.getQuantity();
        a.net = a.net.add(i.getLineTotal().subtract(i.getTaxAmount()));
    }

    private List<List<String>> ranked(Map<String, Agg> map) {
        return map.entrySet().stream()
                .sorted((x, y) -> y.getValue().net.compareTo(x.getValue().net))
                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue().qty), m(e.getValue().net)))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- profit / tax / payment

    private ReportResult profit(List<Sale> sales, String range) {
        Map<LocalDate, List<Sale>> byDay = sales.stream().collect(
                Collectors.groupingBy(s -> s.getSaleDate().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<List<String>> rows = new ArrayList<>();
        byDay.forEach((d, list) -> {
            BigDecimal revenue = sum(list, s -> s.getGrandTotal().subtract(s.getTaxTotal()));
            BigDecimal cost = sum(list, Sale::getTotalCost);
            BigDecimal profit = revenue.subtract(cost);
            BigDecimal margin = revenue.signum() == 0 ? BigDecimal.ZERO
                    : profit.multiply(HUNDRED).divide(revenue, 2, RoundingMode.HALF_UP);
            rows.add(List.of(d.toString(), m(revenue), m(cost), m(profit), m(margin)));
        });
        return new ReportResult("Profit report" + range,
                List.of("Date", "Revenue (excl. tax)", "Cost", "Profit", "Margin %"), rows, 0, 3, "line");
    }

    private ReportResult tax(List<Sale> sales, String range) {
        Map<BigDecimal, Agg> map = new TreeMap<>();
        for (Sale s : sales) {
            for (SaleItem i : s.getItems()) {
                Agg a = map.computeIfAbsent(i.getGstPercent().stripTrailingZeros(), k -> new Agg());
                a.tax = a.tax.add(i.getTaxAmount());
                a.taxable = a.taxable.add(i.getLineTotal().subtract(i.getTaxAmount()));
            }
        }
        List<List<String>> rows = new ArrayList<>();
        map.forEach((rate, a) -> {
            BigDecimal cgst = a.tax.divide(TWO, 2, RoundingMode.HALF_UP);
            BigDecimal sgst = a.tax.subtract(cgst);
            rows.add(List.of(rate.toPlainString() + "%", m(a.taxable), m(cgst), m(sgst), m(a.tax)));
        });
        return new ReportResult("Tax (GST) report" + range,
                List.of("GST rate", "Taxable value", "CGST", "SGST", "Total tax"), rows, 0, 4, "bar");
    }

    private ReportResult payment(List<Sale> sales, String range) {
        Map<PaymentMethod, List<Sale>> by = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod pm : PaymentMethod.values()) by.put(pm, new ArrayList<>());
        for (Sale s : sales) by.get(s.getPaymentMethod()).add(s);
        List<List<String>> rows = new ArrayList<>();
        by.forEach((pm, list) -> rows.add(List.of(pm.name(), String.valueOf(list.size()), m(sum(list, Sale::getGrandTotal)))));
        return new ReportResult("Payment-method report" + range,
                List.of("Method", "Bills", "Total"), rows, 0, 2, "pie");
    }

    // ---------------------------------------------------------------- helpers

    private static BigDecimal sum(Collection<Sale> sales, Function<Sale, BigDecimal> f) {
        return sales.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String m(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    public String toCsv(ReportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(csvLine(r.getHeaders()));
        for (List<String> row : r.getRows()) sb.append(csvLine(row));
        return sb.toString();
    }

    private static String csvLine(List<String> cells) {
        return cells.stream().map(c -> "\"" + c.replace("\"", "\"\"") + "\"").collect(Collectors.joining(",")) + "\r\n";
    }
}
