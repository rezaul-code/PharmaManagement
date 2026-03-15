package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItem;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    @Autowired private MedicineRepository    medicineRepository;
    @Autowired private BillingRepository     billingRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── Inventory ────────────────────────────────────────────────────────────

    public byte[] exportInventory(String format) throws Exception {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        List<Medicine> medicines = medicineRepository.findByPharmacyId(pharmacyId);

        if ("csv".equalsIgnoreCase(format)) {
            return inventoryCsv(medicines);
        }
        return inventoryExcel(medicines);
    }

    private byte[] inventoryCsv(List<Medicine> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("Code,Name,Type,Manufacturer,Batch,Stock,Purchase Price,Selling Price,GST %,Expiry\n");
        for (Medicine m : list) {
            sb.append(csv(m.getMedicineCode())).append(',')
              .append(csv(m.getName())).append(',')
              .append(csv(m.getType() != null ? m.getType().name() : "")).append(',')
              .append(csv(m.getManufacturer())).append(',')
              .append(csv(m.getBatchNo())).append(',')
              .append(m.getStockQuantity()).append(',')
              .append(fmt(m.getPurchasePrice())).append(',')
              .append(fmt(m.getPrice())).append(',')
              .append(fmt(m.getGstPercentage())).append(',')
              .append(m.getExpiryDate() != null ? m.getExpiryDate().toString() : "").append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] inventoryExcel(List<Medicine> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
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
                sheet.setColumnWidth(i, 18 * 256);
            }

            int r = 1;
            for (Medicine m : list) {
                Row row = sheet.createRow(r++);
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
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Sales ────────────────────────────────────────────────────────────────

    public byte[] exportSales(String format) throws Exception {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        List<Billing> bills = billingRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId);

        if ("csv".equalsIgnoreCase(format)) {
            return salesCsv(bills);
        }
        return salesExcel(bills);
    }

    private byte[] salesCsv(List<Billing> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bill #,Patient,Phone,Date,Payment,Status,Subtotal,GST,Grand Total,Balance Due\n");
        for (Billing b : list) {
            sb.append(csv(b.getBillNumber())).append(',')
              .append(csv(b.getPatientName())).append(',')
              .append(csv(b.getPatientPhone())).append(',')
              .append(b.getCreatedAt() != null ? b.getCreatedAt().format(DT_FMT) : "").append(',')
              .append(b.getPaymentType().name()).append(',')
              .append(b.getStatus().name()).append(',')
              .append(fmt(b.getSubtotal())).append(',')
              .append(fmt(b.getTotalGst())).append(',')
              .append(fmt(b.getGrandTotal())).append(',')
              .append(fmt(b.getBalanceDue())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] salesExcel(List<Billing> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
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
                sheet.setColumnWidth(i, 20 * 256);
            }

            int r = 1;
            for (Billing b : list) {
                Row row = sheet.createRow(r++);
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
            wb.write(out);
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
