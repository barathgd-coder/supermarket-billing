package com.supermart.dto;

import com.supermart.model.Sale;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardData {
    private BigDecimal todaySales;
    private long billsToday;
    private long totalProducts;
    private long lowStockProducts;
    private BigDecimal todayProfit;
    private List<Sale> recentSales;
    private List<String> dayLabels;
    private List<BigDecimal> dayValues;
    private List<String> payLabels;
    private List<BigDecimal> payValues;
}
