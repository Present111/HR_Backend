// web/ReportController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.service.ReportService;
import com.hrm.hrmapi.web.dto.report.*;
import com.opencsv.CSVWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/attendance.csv")
    public ResponseEntity<byte[]> exportAttendanceCsv(
            @RequestParam String month,
            @RequestParam(required = false) String department
    ) throws IOException {
        AttendanceReport r = reportService.buildAttendanceReport(month, department);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter w = new CSVWriter(new java.io.OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            w.writeNext(new String[] {"Month", r.getMonth(), "Department", r.getDepartment() == null ? "" : r.getDepartment()});
            w.writeNext(new String[] {"EmployeeId","Name","Department","Date","Status","CheckIn","CheckOut","LateMin","EarlyMin","OTMin"});
            for (var row : r.getRows()) {
                w.writeNext(new String[]{
                        row.getEmployeeId(), row.getEmployeeName(), row.getDepartment(), row.getDate(),
                        row.getStatus(),
                        row.getCheckIn() == null ? "" : row.getCheckIn(),
                        row.getCheckOut() == null ? "" : row.getCheckOut(),
                        String.valueOf(row.getLateMinutes()),
                        String.valueOf(row.getEarlyMinutes()),
                        String.valueOf(row.getOtMinutes())
                });
            }
            var s = r.getSummary();
            w.writeNext(new String[] {});
            w.writeNext(new String[] {"TotalLateMin", String.valueOf(s.getTotalLateMinutes()),
                    "TotalEarlyMin", String.valueOf(s.getTotalEarlyMinutes()),
                    "TotalOTMin", String.valueOf(s.getTotalOtMinutes())});
            w.writeNext(new String[] {"Present", String.valueOf(s.getTotalPresentDays()),
                    "Leave", String.valueOf(s.getTotalLeaveDays()),
                    "WFH", String.valueOf(s.getTotalWfhDays()),
                    "Holiday", String.valueOf(s.getTotalHolidayDays()),
                    "Absent", String.valueOf(s.getTotalAbsentDays())});
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + month + ".csv");
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    @GetMapping("/attendance.pdf")
    public ResponseEntity<byte[]> exportAttendancePdf(
            @RequestParam String month,
            @RequestParam(required = false) String department
    ) {
        AttendanceReport r = reportService.buildAttendanceReport(month, department);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font title = new Font(Font.HELVETICA, 16, Font.BOLD);
        doc.add(new Paragraph("Attendance Report - " + month + (department != null ? " - " + department : ""), title));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{12,18,14,12,10,10,10,8,8,8});
        } catch (DocumentException ignored) { }
        addHeader(table, "EmployeeId","Name","Department","Date","Status","CheckIn","CheckOut","Late","Early","OT");
        for (var row : r.getRows()) {
            addCell(table, row.getEmployeeId(), row.getEmployeeName(), row.getDepartment(), row.getDate(),
                    row.getStatus(),
                    nullToEmpty(row.getCheckIn()),
                    nullToEmpty(row.getCheckOut()),
                    String.valueOf(row.getLateMinutes()),
                    String.valueOf(row.getEarlyMinutes()),
                    String.valueOf(row.getOtMinutes()));
        }
        doc.add(table);

        doc.add(new Paragraph(" "));
        var s = r.getSummary();
        doc.add(new Paragraph(
                "Totals  Late: " + s.getTotalLateMinutes()
                        + "  Early: " + s.getTotalEarlyMinutes()
                        + "  OT: " + s.getTotalOtMinutes()
                        + "  Present: " + s.getTotalPresentDays()
                        + "  Leave: " + s.getTotalLeaveDays()
                        + "  WFH: " + s.getTotalWfhDays()
                        + "  Holiday: " + s.getTotalHolidayDays()
                        + "  Absent: " + s.getTotalAbsentDays()
        ));

        doc.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + month + ".pdf");
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    @GetMapping("/leave.csv")
    public ResponseEntity<byte[]> exportLeaveCsv(
            @RequestParam String month,
            @RequestParam(required = false) String department
    ) throws IOException {
        LeaveReport r = reportService.buildLeaveReport(month, department);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter w = new CSVWriter(new java.io.OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            w.writeNext(new String[] {"Month", r.getMonth(), "Department", r.getDepartment() == null ? "" : r.getDepartment()});
            w.writeNext(new String[] {"RequestId","EmployeeId","Name","Department","Type","From","To","Days","Status"});
            for (var row : r.getRows()) {
                w.writeNext(new String[]{
                        row.getRequestId(), row.getEmployeeId(), row.getEmployeeName(), row.getDepartment(),
                        row.getType(), row.getFromDate(), row.getToDate(),
                        String.valueOf(row.getDays()), row.getStatus()
                });
            }
            w.writeNext(new String[] {});
            w.writeNext(new String[] {"TotalDaysApproved", String.valueOf(r.getTotalDaysApproved()),
                    "TotalDaysPending", String.valueOf(r.getTotalDaysPending())});
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-" + month + ".csv");
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    @GetMapping("/leave.pdf")
    public ResponseEntity<byte[]> exportLeavePdf(
            @RequestParam String month,
            @RequestParam(required = false) String department
    ) {
        LeaveReport r = reportService.buildLeaveReport(month, department);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, baos);
        doc.open();
        Font title = new Font(Font.HELVETICA, 16, Font.BOLD);
        doc.add(new Paragraph("Leave Report - " + month + (department != null ? " - " + department : ""), title));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{14,12,18,14,12,12,12,8,10});
        } catch (DocumentException ignored) { }
        addHeader(table, "RequestId","EmpId","Name","Department","Type","From","To","Days","Status");
        for (var row : r.getRows()) {
            addCell(table, row.getRequestId(), row.getEmployeeId(), row.getEmployeeName(), row.getDepartment(),
                    row.getType(), row.getFromDate(), row.getToDate(),
                    String.valueOf(row.getDays()), row.getStatus());
        }
        doc.add(table);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Total days approved: " + r.getTotalDaysApproved()
                + "   Total days pending: " + r.getTotalDaysPending()));

        doc.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-" + month + ".pdf");
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    private static void addHeader(PdfPTable t, String... cells) {
        Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, bold));
            cell.setGrayFill(0.9f);
            t.addCell(cell);
        }
    }
    private static void addCell(PdfPTable t, String... cells) {
        for (String c : cells) t.addCell(new Phrase(c == null ? "" : c));
    }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
