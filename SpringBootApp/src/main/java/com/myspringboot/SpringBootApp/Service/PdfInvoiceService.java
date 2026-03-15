package com.myspringboot.SpringBootApp.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItem;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfInvoiceService {

    @Autowired private BillingRepository    billingRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    private static final Color PRIMARY     = new Color(14, 165, 233);   // sky-500
    private static final Color HEADER_FG   = Color.WHITE;
    private static final Color MUTED       = new Color(100, 116, 139);  // slate-500
    private static final Color BORDER      = new Color(226, 232, 240);  // slate-200
    private static final Color BG_ALT      = new Color(248, 250, 252);  // slate-50
    private static final Color TEXT        = new Color(15, 23, 42);     // slate-900

    private static final Font FONT_TITLE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT);
    private static final Font FONT_BODY    = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT);
    private static final Font FONT_MUTED   = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font FONT_TH      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_FG);
    private static final Font FONT_TOTAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT);

    /**
     * Generates a PDF invoice for the given bill ID.
     * Returns the raw bytes of the PDF document.
     */
    public byte[] generateInvoice(Long billId) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Billing billing = billingRepository.findByIdAndPharmacyId(billId, pharmacyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invoice not found or does not belong to the current pharmacy."));

        Pharmacy pharmacy = billing.getPharmacy();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Header bar ──────────────────────────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{60f, 40f});
            header.setSpacingAfter(16f);

            // Left: pharmacy name + address
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(
                    pharmacy != null ? pharmacy.getName() : "Pharmacy", FONT_TITLE));
            if (pharmacy != null && pharmacy.getAddress() != null) {
                leftCell.addElement(new Paragraph(pharmacy.getAddress(), FONT_MUTED));
            }
            if (pharmacy != null && pharmacy.getPhone() != null) {
                leftCell.addElement(new Paragraph("📞 " + pharmacy.getPhone(), FONT_MUTED));
            }
            if (pharmacy != null && pharmacy.getGstNumber() != null) {
                leftCell.addElement(new Paragraph("GSTIN: " + pharmacy.getGstNumber(), FONT_MUTED));
            }
            header.addCell(leftCell);

            // Right: TAX INVOICE label + bill info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Font invoiceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY);
            Paragraph invoiceLabel = new Paragraph("TAX INVOICE", invoiceFont);
            invoiceLabel.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invoiceLabel);

            Paragraph billNo = new Paragraph("Bill #: " + billing.getBillNumber(), FONT_SECTION);
            billNo.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(billNo);

            if (billing.getCreatedAt() != null) {
                String dateStr = billing.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                Paragraph datePara = new Paragraph("Date: " + dateStr, FONT_MUTED);
                datePara.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(datePara);
            }
            header.addCell(rightCell);
            doc.add(header);

            // ── Divider ──────────────────────────────────────────────────
            doc.add(new LineSeparator(1f, 100f, BORDER, Element.ALIGN_CENTER, -5f));

            // ── Customer info ────────────────────────────────────────────
            Paragraph customerSection = new Paragraph("BILLED TO", FONT_SECTION);
            customerSection.setSpacingBefore(12f);
            doc.add(customerSection);

            String customerName  = billing.getPatientName()  != null ? billing.getPatientName()  : "Walk-in Customer";
            String customerPhone = billing.getPatientPhone() != null ? billing.getPatientPhone() : "—";
            doc.add(new Paragraph(customerName,         FONT_BODY));
            doc.add(new Paragraph("Phone: " + customerPhone, FONT_MUTED));

            // ── Items table ──────────────────────────────────────────────
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1.2f, 1.8f, 1.4f, 1.4f, 1.8f});
            table.setSpacingBefore(14f);
            table.setSpacingAfter(8f);

            String[] cols = {"Medicine", "Qty", "Unit Price", "GST %", "GST Amt", "Total"};
            for (String col : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(col, FONT_TH));
                cell.setBackgroundColor(PRIMARY);
                cell.setPadding(6f);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            boolean alt = false;
            for (BillingItem item : billing.getItems()) {
                Color bg = alt ? BG_ALT : Color.WHITE;
                alt = !alt;
                addItemRow(table, bg,
                        item.getMedicineName()  != null ? item.getMedicineName() : "—",
                        String.valueOf(item.getQuantity()),
                        fmt(item.getUnitPrice()),
                        fmt(item.getGstPercentage()) + "%",
                        fmt(item.getGstAmount()),
                        fmt(item.getTotalAmount()));
            }
            doc.add(table);

            // ── Totals section ───────────────────────────────────────────
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[]{60f, 40f});
            totals.setSpacingBefore(4f);

            addTotalRow(totals, "Subtotal",   "₹" + fmt(billing.getSubtotal()),  false);
            addTotalRow(totals, "CGST",       "₹" + fmt(billing.getCgst()),      false);
            addTotalRow(totals, "SGST",       "₹" + fmt(billing.getSgst()),      false);
            addTotalRow(totals, "Grand Total","₹" + fmt(billing.getGrandTotal()), true);
            doc.add(totals);

            // ── Payment info ─────────────────────────────────────────────
            Paragraph payInfo = new Paragraph(
                    "Payment: " + billing.getPaymentType().name()
                    + "  |  Status: " + billing.getStatus().name(), FONT_MUTED);
            payInfo.setSpacingBefore(10f);
            doc.add(payInfo);

            // ── Footer ───────────────────────────────────────────────────
            doc.add(new LineSeparator(0.5f, 100f, BORDER, Element.ALIGN_CENTER, -5f));
            String footer = (pharmacy != null && pharmacy.getInvoiceFooter() != null)
                    ? pharmacy.getInvoiceFooter()
                    : "Thank you for your purchase! Stay healthy.";
            Paragraph footerPara = new Paragraph(footer, FONT_MUTED);
            footerPara.setAlignment(Element.ALIGN_CENTER);
            footerPara.setSpacingBefore(8f);
            doc.add(footerPara);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void addItemRow(PdfPTable table, Color bg, String... values) {
        for (String val : values) {
            PdfPCell cell = new PdfPCell(new Phrase(val, FONT_BODY));
            cell.setBackgroundColor(bg);
            cell.setPadding(5f);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(BORDER);
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        Font f = bold ? FONT_TOTAL : FONT_BODY;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, f));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3f);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, f));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3f);
        if (bold) {
            labelCell.setBackgroundColor(BG_ALT);
            valueCell.setBackgroundColor(BG_ALT);
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String fmt(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }
}
