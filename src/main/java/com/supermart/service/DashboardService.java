package com.supermart.service;

import com.supermart.dto.DashboardData;
import com.supermart.model.PaymentMethod;
import com.supermart.model.Sale;
import com.supermart.repository.ProductRepository;
import com.supermart.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardData build() {
        LocalDate today = LocalDate.now();
        List<Sale> todays = salesBetween(today, today);

        BigDecimal totalToday = BigDecimal.ZERO;
        BigDecimal profitToday = BigDecimal.ZERO;
        for (Sale s : todays) {
            totalToday = totalToday.add(s.getGrandTotal());
            profitToday = profitToday.add(s.getProfit());
        }

        // last 7 days
        Map<LocalDate, BigDecimal> perDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) perDay.put(today.minusDays(i), BigDecimal.ZERO);
        for (Sale s : salesBetween(today.minusDays(6), today)) {
            perDay.merge(s.getSaleDate().toLocalDate(), s.getGrandTotal(), BigDecimal::add);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        List<String> dayLabels = new ArrayList<>();
        List<BigDecimal> dayValues = new ArrayList<>();
        perDay.forEach((d, v) -> { dayLabels.add(d.format(fmt)); dayValues.add(v); });

        // payment split, last 30 days
        Map<PaymentMethod, BigDecimal> perMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) perMethod.put(m, BigDecimal.ZERO);
        for (Sale s : salesBetween(today.minusDays(29), today)) {
            perMethod.merge(s.getPaymentMethod(), s.getGrandTotal(), BigDecimal::add);
        }
        List<String> payLabels = new ArrayList<>();
        List<BigDecimal> payValues = new ArrayList<>();
        perMethod.forEach((m, v) -> { payLabels.add(m.name()); payValues.add(v); });

        return DashboardData.builder()
                .todaySales(totalToday)
                .billsToday(todays.size())
                .totalProducts(productRepository.countByActiveTrue())
                .lowStockProducts(productRepository.countLowStock())
                .todayProfit(profitToday)
                .recentSales(saleRepository.findTop10ByOrderBySaleDateDesc())
                .dayLabels(dayLabels).dayValues(dayValues)
                .payLabels(payLabels).payValues(payValues)
                .build();
    }

    private List<Sale> salesBetween(LocalDate from, LocalDate to) {
        return saleRepository.findBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
}
