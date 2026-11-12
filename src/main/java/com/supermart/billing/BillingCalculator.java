package com.supermart.billing;

import com.supermart.exception.BusinessException;
import com.supermart.model.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation logic for a bill (no database access) so it can be unit tested.
 * All money values are BigDecimal, scale 2, RoundingMode.HALF_UP.
 *
 * Selling prices are tax-exclusive: GST is added on top of (line amount - allocated discount).
 */
@Component
public class BillingCalculator {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record LineInput(BigDecimal unitPrice, int quantity, BigDecimal gstPercent) { }

    public record LineResult(BigDecimal gross, BigDecimal discount, BigDecimal taxable,
                             BigDecimal tax, BigDecimal total) { }

    public record BillResult(List<LineResult> lines, BigDecimal subtotal, BigDecimal discount,
                             BigDecimal taxTotal, BigDecimal grandTotal) { }

    public static BigDecimal money(BigDecimal v) {
        return v.setScale(2, ROUNDING);
    }

    public BillResult calculate(List<LineInput> inputs, DiscountType type, BigDecimal discountValue) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        DiscountType dtype = type == null ? DiscountType.NONE : type;
        BigDecimal dvalue = discountValue == null ? BigDecimal.ZERO : discountValue;
        if (dvalue.signum() < 0) {
            throw new BusinessException("Discount cannot be negative");
        }

        // 1. gross amounts
        List<BigDecimal> grosses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineInput in : inputs) {
            if (in.quantity() <= 0) {
                throw new BusinessException("Quantity must be at least 1");
            }
            BigDecimal gross = money(in.unitPrice().multiply(BigDecimal.valueOf(in.quantity())));
            grosses.add(gross);
            subtotal = subtotal.add(gross);
        }

        // 2. bill level discount
        BigDecimal discount = BigDecimal.ZERO;
        if (dtype == DiscountType.PERCENT) {
            if (dvalue.compareTo(HUNDRED) > 0) {
                throw new BusinessException("Discount percentage cannot exceed 100");
            }
            discount = money(subtotal.multiply(dvalue).divide(HUNDRED, 2, ROUNDING));
        } else if (dtype == DiscountType.AMOUNT) {
            discount = money(dvalue);
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessException("Discount cannot exceed the bill subtotal");
        }

        // 3. allocate the discount proportionally; the last line takes the rounding remainder
        List<LineResult> lines = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal discountSum = BigDecimal.ZERO;
        BigDecimal taxSum = BigDecimal.ZERO;
        BigDecimal totalSum = BigDecimal.ZERO;
        for (int i = 0; i < inputs.size(); i++) {
            BigDecimal gross = grosses.get(i);
            BigDecimal share;
            if (subtotal.signum() == 0 || discount.signum() == 0) {
                share = BigDecimal.ZERO;
            } else if (i == inputs.size() - 1) {
                share = discount.subtract(allocated);
            } else {
                share = money(discount.multiply(gross).divide(subtotal, 2, ROUNDING));
            }
            if (share.compareTo(gross) > 0) share = gross;
            if (share.signum() < 0) share = BigDecimal.ZERO;
            allocated = allocated.add(share);

            BigDecimal taxable = gross.subtract(share);
            BigDecimal tax = money(taxable.multiply(inputs.get(i).gstPercent()).divide(HUNDRED, 2, ROUNDING));
            BigDecimal total = taxable.add(tax);
            lines.add(new LineResult(gross, share, taxable, tax, total));
            discountSum = discountSum.add(share);
            taxSum = taxSum.add(tax);
            totalSum = totalSum.add(total);
        }
        return new BillResult(lines, subtotal, discountSum, taxSum, totalSum);
    }
}
