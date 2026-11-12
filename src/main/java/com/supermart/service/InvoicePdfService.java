package com.supermart.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.supermart.model.Sale;
import com.supermart.model.SaleItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final SettingService settingService;

    public byte[] generate(Sale sale) {
        Map<String, Object> store = settingService.storeInfo();
        String symbol = String.valueOf(store.get("currency"));
        String cur = "\u20B9".equals(symbol) ? "Rs. " : symbol + " ";   // built-in PDF fonts have no rupee glyph

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 28, 28, 28, 28);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            settingService.logoFile().ifPresent(p -> addLogo(doc, p));
            center(doc, String.valueOf(store.get("name")), title);
            center(doc, String.valueOf(store.get("address")), small);
            center(doc, "Phone: " + store.get("phone") + (String.valueOf(store.get("gstin")).isBlank() ? "" : "   GSTIN: " + store.get("gstin")), small);
            doc.add(new Paragraph(" ", small));

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(plain("Invoice: " + sale.getInvoiceNumber(), bold));
            meta.addCell(plainRight("Date: " + sale.getSaleDate().format(DATE_TIME), normal));
            meta.addCell(plain("Cashier: " + sale.getCashier().getFullName(), normal));
            meta.addCell(plainRight("Payment: " + sale.getPaymentMethod(), normal));
            String customer = sale.getCustomerName() == null ? "Walk-in customer" : sale.getCustomerName();
            if (sale.getCustomerPhone() != null) customer += " (" + sale.getCustomerPhone() + ")";
            meta.addCell(plain("Customer: " + customer, normal));
            meta.addCell(plain("", normal));
            doc.add(meta);
            doc.add(new Paragraph(" ", small));

            PdfPTable items = new PdfPTable(new float[]{4.2f, 0.9f, 1.6f, 1.4f, 1.4f, 1.8f});
            items.setWidthPercentage(100);
            for (String h : new String[]{"Item", "Qty", "Price", "Disc.", "Tax", "Total"}) {
                items.addCell(headCell(h, bold));
            }
            for (SaleItem i : sale.getItems()) {
                items.addCell(cell(i.getProductName(), normal, Element.ALIGN_LEFT));
                items.addCell(cell(String.valueOf(i.getQuantity()), normal, Element.ALIGN_RIGHT));
                items.addCell(cell(num(i.getUnitPrice()), normal, Element.ALIGN_RIGHT));
                items.addCell(cell(num(i.getDiscountAmount()), normal, Element.ALIGN_RIGHT));
                items.addCell(cell(num(i.getTaxAmount()) + " (" + i.getGstPercent().stripTrailingZeros().toPlainString() + "%)", small, Element.ALIGN_RIGHT));
                items.addCell(cell(num(i.getLineTotal()), normal, Element.ALIGN_RIGHT));
            }
            doc.add(items);
            doc.add(new Paragraph(" ", small));

            PdfPTable totals = new PdfPTable(new float[]{3, 1.4f});
            totals.setWidthPercentage(55);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.addCell(plain("Subtotal", normal));
            totals.addCell(plainRight(cur + num(sale.getSubtotal()), normal));
            totals.addCell(plain("Discount", normal));
            totals.addCell(plainRight("- " + cur + num(sale.getDiscountAmount()), normal));
            totals.addCell(plain("GST", normal));
            totals.addCell(plainRight(cur + num(sale.getTaxTotal()), normal));
            totals.addCell(plain("Grand total", bold));
            totals.addCell(plainRight(cur + num(sale.getGrandTotal()), bold));
            doc.add(totals);

            doc.add(new Paragraph(" ", small));
            center(doc, String.valueOf(store.get("footer")), normal);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate invoice PDF", e);
        }
        return out.toByteArray();
    }

    private void addLogo(Document doc, Path path) {
        try {
            Image img = Image.getInstance(path.toString());
            img.scaleToFit(60, 60);
            img.setAlignment(Element.ALIGN_CENTER);
            doc.add(img);
        } catch (Exception ignored) {
            // an unreadable logo must never prevent the invoice from being generated
        }
    }

    private void center(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private static String num(BigDecimal v) { return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }

    private static PdfPCell plain(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2);
        return c;
    }

    private static PdfPCell plainRight(String text, Font f) {
        PdfPCell c = plain(text, f);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static PdfPCell headCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        c.setPadding(3);
        c.setHorizontalAlignment(text.equals("Item") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
        return c;
    }

    private static PdfPCell cell(String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(java.awt.Color.LIGHT_GRAY);
        c.setPadding(3);
        c.setHorizontalAlignment(align);
        return c;
    }
}
