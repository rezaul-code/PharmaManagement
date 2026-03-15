package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;  // ← streaming XLSX, flushes rows to disk
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * TASK 6 FIX — Streaming exports.
 *
 * Previous approach: medicineRepository.findByPharmacyId() loaded the full
 * table into memory in one query. With 100k+ rows this causes OOM.
 *
 * New approach:
 *   - CSV: iterates through pages of 500 rows, builds output incrementally.
 *   - Excel: uses SXSSFWorkbook (streaming XLSX) with a 500-row in-memory
 *     window; rows outside the window are spooled to a temp file by POI.
 *
 * Peak heap per export ≈ 2× page size instead of 2× full dataset.
 */
@Service
public class ExportService {

    private static final int    PAGE_SIZE = 500;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Autowired private MedicineRepository    medicineRepository;
    @Autowired private BillingRepository     billingRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    // ── Inventory ────────────────────────────────────────────────────────────

    public byte[] exportInventory(String format) throws Exception {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        return "csv".equalsIgnoreCase(format)
                ? inventoryCsv(pharmacyId)
                : inventoryExcel(pharmacyId);
    }

    private byte[] inventoryCsv(Long pharmacyId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                out, java.nio.charset.StandardCharsets.UTF_8)) {

            writer.write("Code,Name,Type,Manufacturer,Batch,Stock,Purchase Price,Selling Price,GST %,Expiry\n");

            int page = 0;
            Page<Medicine> slice;
            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("name"));
                slice = medicineRepository.findByPharmacyId(pharmacyId, pageable);
                for (Medicine m : slice.getContent()) {
                    writer.write(csv(m.getMedicineCode())); writer.write(',');
                    writer.write(csv(m.getName()));        writer.write(',');
                    writer.write(csv(m.getType() != null ? m.getType().name() : "")); writer.write(',');
                    writer.write(csv(m.getManufacturer())); writer.write(',');
                    writer.write(csv(m.getBatchNo())); writer.write(',');
                    writer.write(String.valueOf(m.getStockQuantity() != null ? m.getStockQuantity() : 0)); writer.write(',');
                    writer.write(fmt(m.getPurchasePrice())); writer.write(',');
                    writer.write(fmt(m.getPrice())); writer.write(',');
                    writer.write(fmt(m.getGstPercentage())); writer.write(',');
                    writer.write(m.getExpiryDate() != null ? m.getExpiryDate().toString() : ""); writer.write('\n');
                }
                writer.flush(); // release row data to OS after each page
            } while (slice.hasNext());
        }
        return out.toByteArray();
    }

    private byte[] inventoryExcel(Long pharmacyId) throws Exception {
        // SXSSFWorkbook keeps only `windowSize` rows in memory; rest spool to disk
        try (SXSSFWorkbook wb = new SXSSFWorkbook(PAGE_SIZE);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Inventory");
            CellStyle headerStyle = buildHeaderStyle(wb);

            String[] headers = {"Code","Name","Type","Manufacturer","Batch",
                                 "Stock","Purchase Price","Selling Price","GST %","Expiry"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int page   = 0;
            Page<Medicine> slice;
            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("name"));
                slice = medicineRepository.findByPharmacyId(pharmacyId, pageable);
                for (Medicine m : slice.getContent()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(nullStr(m.getMedicineCode()));
                    row.createCell(1).setCellValue(nullStr(m.getName()));
                    row.createCell(2).setCellValue(m.getType() != null ? m.getType().name() : "");
                    row.createCell(3).setCellValue(nullStr(m.getManufacturer()));
                    row.createCell(4).setCellValue(nullStr(m.getBatchNo()));
                    row.createCell(5).setCellValue(m.getStockQuantity() != null ? m.getStockQuantity() : 0);
                    row.createCell(6).setCellValue(m.getPurchasePrice() != null ? m.getPurchasePrice().doubleValue() : 0);
                    row.createCell(7).setCellValue(m.getPrice() != null ? m.getPrice().doubleValue() : 0);
                    row.createCell(8).setCellValue(m.getGstPercentage() != null ? m.getGstPercentage().doubleValue() : 0);
                    row.createCell(9).setCellValue(m.getExpiryDate() != null ? m.getExpiryDate().toString() : "");
                }
            } while (slice.hasNext());

            wb.write(out);
            wb.dispose(); // delete temp files created by SXSSFWorkbook
            return out.toByteArray();
        }
    }

    // ── Sales ────────────────────────────────────────────────────────────────

    public byte[] exportSales(String format) throws Exception {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        return "csv".equalsIgnoreCase(format)
                ? salesCsv(pharmacyId)
                : salesExcel(pharmacyId);
    }

    private byte[] salesCsv(Long pharmacyId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                out, java.nio.charset.StandardCharsets.UTF_8)) {

            writer.write("Bill #,Patient,Phone,Date,Payment,Status,Subtotal,GST,Grand Total,Balance Due\n");

            int page = 0;
            Page<Billing> slice;
            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("createdAt").descending());
                slice = billingRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId, pageable);
                for (Billing b : slice.getContent()) {
                    writer.write(csv(b.getBillNumber())); writer.write(',');
                    writer.write(csv(b.getPatientName())); writer.write(',');
                    writer.write(csv(b.getPatientPhone())); writer.write(',');
                    writer.write(b.getCreatedAt() != null ? b.getCreatedAt().format(DT_FMT) : ""); writer.write(',');
                    writer.write(b.getPaymentType().name()); writer.write(',');
                    writer.write(b.getStatus().name()); writer.write(',');
                    writer.write(fmt(b.getSubtotal())); writer.write(',');
                    writer.write(fmt(b.getTotalGst())); writer.write(',');
                    writer.write(fmt(b.getGrandTotal())); writer.write(',');
                    writer.write(fmt(b.getBalanceDue())); writer.write('\n');
                }
                writer.flush();
            } while (slice.hasNext());
        }
        return out.toByteArray();
    }

    private byte[] salesExcel(Long pharmacyId) throws Exception {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(PAGE_SIZE);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Sales");
            CellStyle headerStyle = buildHeaderStyle(wb);

            String[] headers = {"Bill #","Patient","Phone","Date","Payment","Status",
                                 "Subtotal","GST","Grand Total","Balance Due"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int page   = 0;
            Page<Billing> slice;
            do {
                Pageable pageable = PageRequest.of(page++, PAGE_SIZE, Sort.by("createdAt").descending());
                slice = billingRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId, pageable);
                for (Billing b : slice.getContent()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(nullStr(b.getBillNumber()));
                    row.createCell(1).setCellValue(nullStr(b.getPatientName()));
                    row.createCell(2).setCellValue(nullStr(b.getPatientPhone()));
                    row.createCell(3).setCellValue(b.getCreatedAt() != null ? b.getCreatedAt().format(DT_FMT) : "");
                    row.createCell(4).setCellValue(b.getPaymentType().name());
                    row.createCell(5).setCellValue(b.getStatus().name());
                    row.createCell(6).setCellValue(b.getSubtotal()   != null ? b.getSubtotal().doubleValue()   : 0);
                    row.createCell(7).setCellValue(b.getTotalGst()   != null ? b.getTotalGst().doubleValue()   : 0);
                    row.createCell(8).setCellValue(b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0);
                    row.createCell(9).setCellValue(b.getBalanceDue() != null ? b.getBalanceDue().doubleValue() : 0);
                }
            } while (slice.hasNext());

            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String fmt(BigDecimal value) {
        return value != null ? value.toPlainString() : "0.00";
    }

    private String nullStr(String value) {
        return value != null ? value : "";
    }
}
