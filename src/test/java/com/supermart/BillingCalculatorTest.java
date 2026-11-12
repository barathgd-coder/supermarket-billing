package com.supermart;

import com.supermart.billing.BillingCalculator;
import com.supermart.billing.BillingCalculator.BillResult;
import com.supermart.billing.BillingCalculator.LineInput;
import com.supermart.exception.BusinessException;
import com.supermart.model.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingCalculatorTest {

    private final BillingCalculator calculator = new BillingCalculator();

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void percentDiscountAndGstAreCalculatedCorrectly() {
        BillResult r = calculator.calculate(List.of(
                new LineInput(bd("50.00"), 2, bd("5")),
                new LineInput(bd("100.00"), 1, bd("12"))), DiscountType.PERCENT, bd("10"));

        assertThat(r.subtotal()).isEqualByComparingTo("200.00");
        assertThat(r.discount()).isEqualByComparingTo("20.00");
        assertThat(r.taxTotal()).isEqualByComparingTo("15.30");   // 4.50 + 10.80
        assertThat(r.grandTotal()).isEqualByComparingTo("195.30");
    }

    @Test
    void roundingUsesHalfUp() {
        BillResult r = calculator.calculate(List.of(new LineInput(bd("33.33"), 3, bd("18"))), DiscountType.NONE, null);
        assertThat(r.subtotal()).isEqualByComparingTo("99.99");
        assertThat(r.taxTotal()).isEqualByComparingTo("18.00");   // 17.9982 -> 18.00
        assertThat(r.grandTotal()).isEqualByComparingTo("117.99");
        assertThat(r.grandTotal().scale()).isEqualTo(2);
    }

    @Test
    void fixedDiscountIsAllocatedWithoutLosingACent() {
        BillResult r = calculator.calculate(List.of(
                new LineInput(bd("10.00"), 1, bd("18")),
                new LineInput(bd("10.00"), 1, bd("18")),
                new LineInput(bd("10.00"), 1, bd("18"))), DiscountType.AMOUNT, bd("10.00"));
        assertThat(r.discount()).isEqualByComparingTo("10.00");
        BigDecimal allocated = r.lines().stream().map(l -> l.discount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(allocated).isEqualByComparingTo("10.00");
        assertThat(r.grandTotal()).isEqualByComparingTo("23.60");  // 20.00 taxable + 3.60 tax
    }

    @Test
    void discountAboveSubtotalIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(List.of(new LineInput(bd("10"), 1, BigDecimal.ZERO)),
                DiscountType.AMOUNT, bd("11"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> calculator.calculate(List.of(new LineInput(bd("10"), 1, BigDecimal.ZERO)),
                DiscountType.PERCENT, bd("101"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void emptyCartAndBadQuantityAreRejected() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), DiscountType.NONE, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> calculator.calculate(List.of(new LineInput(bd("10"), 0, BigDecimal.ZERO)),
                DiscountType.NONE, null)).isInstanceOf(BusinessException.class);
    }
}
