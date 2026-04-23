package com.alibou.book.Services;

import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Generates PDF eligibility reports from EligibilityRecord entities.
 * Mirrors the visual structure previously rendered by the Angular jsPDF code.
 */
@Service
@RequiredArgsConstructor
public class EligibilityReportService {

    private final EligibilityRecordRepository eligibilityRecordRepository;

    // ── Colour palette matching the Angular UI ───────────────────────────────
    private static final Color NAVY       = new Color(15,  32,  82);
    private static final Color BLUE       = new Color(0,  123, 191);
    private static final Color LIGHT_GREY = new Color(248, 250, 252);
    private static final Color MID_GREY   = new Color(220, 230, 240);
    private static final Color GREEN      = new Color(34,  139,  34);
    private static final Color ORANGE     = new Color(191,  87,   0);
    private static final Color CRIMSON    = new Color(220,  20,  60);
    private static final Color DARK_TEXT  = new Color(50,   50,  50);
    private static final Color SOFT_TEXT  = new Color(80,   80,  80);

    // ── Fonts ────────────────────────────────────────────────────────────────
    private static final Font TITLE_FONT     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  22, Color.WHITE);
    private static final Font SUBTITLE_FONT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  14, new Color(200,220,240));
    private static final Font SECTION_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, NAVY);
    private static final Font LABEL_BOLD     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, NAVY);
    private static final Font LABEL_NORMAL   = FontFactory.getFont(FontFactory.HELVETICA,        10, SOFT_TEXT);
    private static final Font SMALL_BOLD     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, DARK_TEXT);
    private static final Font SMALL_NORMAL   = FontFactory.getFont(FontFactory.HELVETICA,         9, DARK_TEXT);
    private static final Font SMALL_BLUE     = FontFactory.getFont(FontFactory.HELVETICA,         9, BLUE);
    private static final Font SMALL_GREEN    = FontFactory.getFont(FontFactory.HELVETICA,         9, GREEN);
    private static final Font SMALL_ORANGE   = FontFactory.getFont(FontFactory.HELVETICA,         9, ORANGE);
    private static final Font SMALL_CRIMSON  = FontFactory.getFont(FontFactory.HELVETICA,         9, CRIMSON);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm");

    // =========================================================================
    //  Public API
    // =========================================================================

    public byte[] generateReport(String recordId) {
        EligibilityRecord record = eligibilityRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Eligibility record not found: " + recordId));
        return buildPdf(record);
    }

    // =========================================================================
    //  PDF construction
    // =========================================================================

    private byte[] buildPdf(EligibilityRecord record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36f, 36f, 36f, 36f);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new FooterEvent());
        doc.open();

        addCoverHeader(doc, writer, record);
        addExecutiveSummary(doc, record);
        addSelectedCategories(doc, record);
        addUniversityDetails(doc, record);

        doc.close();
        return out.toByteArray();
    }

    // ─── Cover header ────────────────────────────────────────────────────────

    private void addCoverHeader(Document doc, PdfWriter writer, EligibilityRecord record) {
        PdfContentByte cb = writer.getDirectContent();

        // Navy banner
        cb.setColorFill(NAVY);
        cb.rectangle(0, PageSize.A4.getHeight() - 130, PageSize.A4.getWidth(), 130);
        cb.fill();

        // Blue accent stripe
        cb.setColorFill(BLUE);
        cb.rectangle(0, PageSize.A4.getHeight() - 150, PageSize.A4.getWidth(), 20);
        cb.fill();

        // Title text (absolute position)
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("ADMISSION ELIGIBILITY", TITLE_FONT),
                PageSize.A4.getWidth() / 2f, PageSize.A4.getHeight() - 60f, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("COMPREHENSIVE ANALYSIS REPORT", SUBTITLE_FONT),
                PageSize.A4.getWidth() / 2f, PageSize.A4.getHeight() - 85f, 0);

        // Spacer after banner
        try { doc.add(new Paragraph(" ")); } catch (DocumentException ignored) {}

        // Candidate info box
        String candidateName = record.getExamCheckRecord() != null
                && record.getExamCheckRecord().getCandidateName() != null
                ? record.getExamCheckRecord().getCandidateName()
                : "Candidate";

        PdfPTable infoBox = new PdfPTable(1);
        infoBox.setWidthPercentage(60);
        infoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoBox.setSpacingBefore(90f);

        PdfPCell labelCell = new PdfPCell(new Phrase("PREPARED FOR",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY)));
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(LIGHT_GREY);
        labelCell.setPadding(6f);
        infoBox.addCell(labelCell);

        PdfPCell nameCell = new PdfPCell(new Phrase(candidateName,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BLUE)));
        nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setBackgroundColor(LIGHT_GREY);
        nameCell.setPadding(4f);
        infoBox.addCell(nameCell);

        String reportId = "Report ID: " + record.getId();
        String generatedOn = "Generated: " + (record.getCreatedAt() != null
                ? record.getCreatedAt().format(DATE_FMT) : "N/A");

        PdfPCell metaCell = new PdfPCell(new Phrase(reportId + "\n" + generatedOn,
                FontFactory.getFont(FontFactory.HELVETICA, 8, SOFT_TEXT)));
        metaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setBackgroundColor(LIGHT_GREY);
        metaCell.setPaddingBottom(8f);
        infoBox.addCell(metaCell);

        addSafe(doc, infoBox);
    }

    // ─── Executive summary ───────────────────────────────────────────────────

    private void addExecutiveSummary(Document doc, EligibilityRecord record) {
        addSectionHeader(doc, "Executive Summary");

        List<UniversityEligibility> unis = record.getUniversities() != null
                ? record.getUniversities() : Collections.emptyList();

        int eligibleCount = unis.stream()
                .mapToInt(u -> u.getEligiblePrograms() != null ? u.getEligiblePrograms().size() : 0)
                .sum();
        int alternativeCount = unis.stream()
                .mapToInt(u -> u.getAlternativePrograms() != null ? u.getAlternativePrograms().size() : 0)
                .sum();
        int totalPrograms = eligibleCount + alternativeCount;
        String analysisDate = record.getCreatedAt() != null
                ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "N/A";

        PdfPTable table = createTwoColumnTable(80, 20);
        addSummaryRow(table, "Total Universities Analysed", String.valueOf(unis.size()));
        addSummaryRow(table, "Highly Eligible Programs",   String.valueOf(eligibleCount));
        addSummaryRow(table, "Alternative Options",         String.valueOf(alternativeCount));
        addSummaryRow(table, "Total Programs Found",        String.valueOf(totalPrograms));
        addSummaryRow(table, "Analysis Date",               analysisDate);
        addSafe(doc, table);
    }

    // ─── Selected categories ─────────────────────────────────────────────────

    private void addSelectedCategories(Document doc, EligibilityRecord record) {
        if (record.getSelectedCategories() == null || record.getSelectedCategories().isEmpty()) return;

        addSectionHeader(doc, "Selected Study Areas");

        StringBuilder sb = new StringBuilder();
        for (String cat : record.getSelectedCategories()) {
            sb.append("• ").append(cat).append("   ");
        }
        Paragraph cats = new Paragraph(sb.toString().trim(), SMALL_BLUE);
        cats.setIndentationLeft(10f);
        cats.setSpacingBefore(4f);
        cats.setSpacingAfter(10f);
        addSafe(doc, cats);
    }

    // ─── University details ───────────────────────────────────────────────────

    private void addUniversityDetails(Document doc, EligibilityRecord record) {
        addSectionHeader(doc, "Detailed University Analysis");

        List<UniversityEligibility> unis = record.getUniversities() != null
                ? record.getUniversities() : Collections.emptyList();

        for (UniversityEligibility uni : unis) {
            addUniversityBlock(doc, uni);
        }
    }

    private void addUniversityBlock(Document doc, UniversityEligibility uni) {
        // University name bar
        PdfPTable nameBar = new PdfPTable(1);
        nameBar.setWidthPercentage(100);
        nameBar.setSpacingBefore(12f);

        PdfPCell nameCell = new PdfPCell(
                new Phrase(uni.getUniversityName() != null
                        ? uni.getUniversityName().toUpperCase() : "UNKNOWN UNIVERSITY",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
        nameCell.setBackgroundColor(NAVY);
        nameCell.setPadding(7f);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameBar.addCell(nameCell);
        addSafe(doc, nameBar);

        // Details row (type, location, program count)
        int eligCount = uni.getEligiblePrograms() != null ? uni.getEligiblePrograms().size() : 0;
        int altCount  = uni.getAlternativePrograms() != null ? uni.getAlternativePrograms().size() : 0;

        Paragraph details = new Paragraph();
        details.setSpacingBefore(3f);
        details.setSpacingAfter(4f);
        details.add(new Chunk("Type: " + nvl(uni.getType(), "N/A")
                + "   |   Location: " + nvl(uni.getLocation(), "N/A")
                + "   |   Programs: " + eligCount + " Eligible, " + altCount + " Alternative",
                SMALL_NORMAL));
        addSafe(doc, details);

        // Eligible programs
        if (eligCount > 0) {
            addProgramGroupHeader(doc, "Highly Eligible Programs", GREEN);
            for (EligibleProgram p : uni.getEligiblePrograms()) {
                addProgramBlock(doc, p.getName(), p.getPercentage(),
                        p.getCategories(), p.getAiRecommendation(), true);
            }
        }

        // Alternative programs
        if (altCount > 0) {
            addProgramGroupHeader(doc, "Alternative Programs", ORANGE);
            for (AlternativeProgram p : uni.getAlternativePrograms()) {
                addProgramBlock(doc, p.getName(), p.getPercentage(),
                        p.getCategories(), p.getAiRecommendation(), false);
            }
        }
    }

    private void addProgramGroupHeader(Document doc, String title, Color color) {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(6f);

        PdfPCell cell = new PdfPCell(
                new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        cell.setBackgroundColor(color);
        cell.setPadding(5f);
        cell.setBorder(Rectangle.NO_BORDER);
        bar.addCell(cell);
        addSafe(doc, bar);
    }

    private void addProgramBlock(Document doc, String name, double percentage,
                                  List<String> categories, AIRecommendation aiRec,
                                  boolean isEligible) {
        // Program name + percentage row
        PdfPTable progTable = createTwoColumnTable(70, 30);
        progTable.setSpacingBefore(5f);

        Color statusColor = isEligible ? GREEN : ORANGE;

        PdfPCell nameCell = new PdfPCell(
                new Phrase(nvl(name, "Unknown Program"),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY)));
        nameCell.setBorderColor(MID_GREY);
        nameCell.setPadding(5f);
        nameCell.setBackgroundColor(Color.WHITE);
        progTable.addCell(nameCell);

        String pct = String.format("%.1f%%", percentage);
        PdfPCell pctCell = new PdfPCell(
                new Phrase(pct, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, statusColor)));
        pctCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pctCell.setBorderColor(MID_GREY);
        pctCell.setPadding(5f);
        pctCell.setBackgroundColor(Color.WHITE);
        progTable.addCell(pctCell);
        addSafe(doc, progTable);

        // Status badge + categories
        String badgeLabel = isEligible ? "Eligible" : "Alternative";
        StringBuilder badgeLine = new StringBuilder("[" + badgeLabel + "]");
        if (categories != null && !categories.isEmpty()) {
            badgeLine.append("  ").append(String.join(" • ", categories));
        }
        Paragraph badge = new Paragraph(badgeLine.toString(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, statusColor));
        badge.setIndentationLeft(5f);
        badge.setSpacingBefore(2f);
        badge.setSpacingAfter(4f);
        addSafe(doc, badge);

        // Requirement explanations from recommendationText
        if (aiRec != null && aiRec.getRecommendationText() != null
                && !aiRec.getRecommendationText().isBlank()) {
            addRequirementExplanations(doc, aiRec.getRecommendationText(), isEligible);
        }

        // AI career insights (only shown if populated)
        if (aiRec != null) {
            addAIInsightBlock(doc, "Career Path",        aiRec.getCareerPath(),        GREEN);
            addAIInsightBlock(doc, "Job Opportunities",  aiRec.getJobOpportunities(),  BLUE);
            addAIInsightBlock(doc, "Future Prospects",   aiRec.getFutureProspects(),   new Color(138, 43, 226));
        }

        // Divider
        Paragraph divider = new Paragraph(" ");
        divider.setSpacingAfter(4f);
        addSafe(doc, divider);
    }

    // ─── Requirement analysis tables ──────────────────────────────────────────

    private void addRequirementExplanations(Document doc, String recommendationText,
                                             boolean isEligible) {
        ReportSections sections = parseRecommendationText(recommendationText);
        Color themeColor = isEligible ? GREEN : ORANGE;

        if (!sections.coreLines.isEmpty()) {
            boolean hasContent = addAnalysisTable(doc, "Core Subjects Analysis",
                    sections.coreLines, NAVY);
            if (!hasContent) return;
        }
        if (!sections.altLines.isEmpty()) {
            addAnalysisTable(doc, "Alternative Requirements",
                    sections.altLines, new Color(245, 158, 11));
        }
        if (!sections.recLines.isEmpty()) {
            addNoteBlock(doc, sections.recLines);
        }
    }

    /**
     * Builds a 3-column OKAY | MISSING | BELOW REQUIREMENT table.
     * Returns true if at least one row was written.
     */
    private boolean addAnalysisTable(Document doc, String title,
                                      List<String> lines, Color headerColor) {
        List<String> ok   = new ArrayList<>();
        List<String> warn = new ArrayList<>();
        List<String> fail = new ArrayList<>();

        for (String line : lines) {
            String clean = line.replaceAll("[✅⚠️❌💡📋⭐]", "").trim();
            if (clean.isEmpty()) continue;
            LineStatus status = classifyLine(line);
            switch (status) {
                case OK   -> ok.add(clean);
                case WARN -> warn.add(clean);
                case FAIL -> fail.add(clean);
                default   -> { /* neutral lines not shown in table */ }
            }
        }

        if (ok.isEmpty() && warn.isEmpty() && fail.isEmpty()) return false;

        if (ok.isEmpty())   ok.add("N/A");
        if (warn.isEmpty()) warn.add("N/A");
        if (fail.isEmpty()) fail.add("N/A");

        // Section header
        PdfPTable headerBar = new PdfPTable(1);
        headerBar.setWidthPercentage(100);
        headerBar.setSpacingBefore(6f);

        PdfPCell hCell = new PdfPCell(
                new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        hCell.setBackgroundColor(headerColor);
        hCell.setPadding(4f);
        hCell.setBorder(Rectangle.NO_BORDER);
        headerBar.addCell(hCell);
        addSafe(doc, headerBar);

        // 3-column analysis table
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        table.setSpacingAfter(4f);

        // Header row
        addAnalysisHeaderCell(table, "OKAY",             GREEN,   headerColor);
        addAnalysisHeaderCell(table, "MISSING",          ORANGE,  headerColor);
        addAnalysisHeaderCell(table, "BELOW REQUIREMENT",CRIMSON, headerColor);

        int maxRows = Math.max(ok.size(), Math.max(warn.size(), fail.size()));
        for (int i = 0; i < maxRows; i++) {
            addAnalysisDataCell(table, i < ok.size()   ? ok.get(i)   : "N/A", GREEN);
            addAnalysisDataCell(table, i < warn.size() ? warn.get(i) : "N/A", ORANGE);
            addAnalysisDataCell(table, i < fail.size() ? fail.get(i) : "N/A", CRIMSON);
        }
        addSafe(doc, table);
        return true;
    }

    private void addAnalysisHeaderCell(PdfPTable table, String text, Color textColor, Color bgColor) {
        PdfPCell cell = new PdfPCell(
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, textColor)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setBorderColor(MID_GREY);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addAnalysisDataCell(PdfPTable table, String text, Color textColor) {
        PdfPCell cell = new PdfPCell(
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8, textColor)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(MID_GREY);
        cell.setPadding(3f);
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addNoteBlock(Document doc, List<String> recLines) {
        Paragraph note = new Paragraph();
        note.setSpacingBefore(4f);
        note.add(new Chunk("NOTE:  ", SMALL_BOLD));
        for (String line : recLines) {
            String clean = line.replaceAll("[💡📋⭐?]", "").trim();
            if (!clean.isEmpty()) {
                note.add(new Chunk(clean + "  ", SMALL_NORMAL));
            }
        }
        note.setSpacingAfter(6f);
        addSafe(doc, note);
    }

    // ─── AI insight block ─────────────────────────────────────────────────────

    private void addAIInsightBlock(Document doc, String title, String content, Color accentColor) {
        if (content == null || content.isBlank()) return;

        PdfPTable block = new PdfPTable(1);
        block.setWidthPercentage(95);
        block.setHorizontalAlignment(Element.ALIGN_RIGHT);
        block.setSpacingBefore(4f);
        block.setSpacingAfter(4f);

        String cleaned = content.replaceAll("[\\p{So}\\p{Cn}]", "").trim();

        PdfPCell titleCell = new PdfPCell(
                new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, accentColor)));
        titleCell.setBorder(Rectangle.LEFT);
        titleCell.setBorderColor(accentColor);
        titleCell.setBorderWidth(2f);
        titleCell.setBackgroundColor(LIGHT_GREY);
        titleCell.setPadding(5f);
        block.addCell(titleCell);

        PdfPCell contentCell = new PdfPCell(
                new Phrase(cleaned, FontFactory.getFont(FontFactory.HELVETICA, 8, DARK_TEXT)));
        contentCell.setBorder(Rectangle.LEFT);
        contentCell.setBorderColor(accentColor);
        contentCell.setBorderWidth(2f);
        contentCell.setBackgroundColor(Color.WHITE);
        contentCell.setPadding(5f);
        block.addCell(contentCell);

        addSafe(doc, block);
    }

    // ─── Section header helper ────────────────────────────────────────────────

    private void addSectionHeader(Document doc, String title) {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        addSafe(doc, p);

        // Blue underline via a thin coloured table
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(40);
        line.setHorizontalAlignment(Element.ALIGN_LEFT);
        line.setSpacingAfter(6f);
        PdfPCell lineCell = new PdfPCell(new Phrase(" "));
        lineCell.setBackgroundColor(BLUE);
        lineCell.setFixedHeight(2f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        addSafe(doc, line);
    }

    // ─── Summary table helpers ────────────────────────────────────────────────

    private PdfPTable createTwoColumnTable(int leftPct, int rightPct) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(6f);
        try { table.setWidths(new int[]{leftPct, rightPct}); } catch (DocumentException ignored) {}
        return table;
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, LABEL_BOLD));
        lCell.setBorderColor(MID_GREY);
        lCell.setPadding(4f);
        lCell.setBackgroundColor(LIGHT_GREY);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(
                new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLUE)));
        vCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        vCell.setBorderColor(MID_GREY);
        vCell.setPadding(4f);
        table.addCell(vCell);
    }

    // =========================================================================
    //  Recommendation text parsing  (port of Angular addRequirementExplanations)
    // =========================================================================

    private ReportSections parseRecommendationText(String text) {
        String[] rawLines = text.split("\n");
        List<String> lines = new ArrayList<>();
        for (String l : rawLines) {
            if (!l.trim().isEmpty()) lines.add(l);
        }

        int coreStart = -1, altStart = -1, recStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            String upper = lines.get(i).toUpperCase();
            if (upper.contains("CORE SUBJECTS ANALYSIS")   && coreStart == -1) coreStart = i;
            else if (upper.contains("ALTERNATIVE REQUIREMENTS") && altStart == -1) altStart = i;
            else if (upper.contains("RECOMMENDATIONS")         && recStart == -1) recStart = i;
        }

        int total = lines.size();
        ReportSections rs = new ReportSections();

        rs.coreLines = coreStart != -1
                ? lines.subList(coreStart + 1, altStart != -1 ? altStart : (recStart != -1 ? recStart : total))
                : Collections.emptyList();

        rs.altLines = altStart != -1
                ? lines.subList(altStart + 1, recStart != -1 ? recStart : total)
                : Collections.emptyList();

        rs.recLines = recStart != -1
                ? lines.subList(recStart + 1, total)
                : Collections.emptyList();

        return rs;
    }

    private LineStatus classifyLine(String line) {
        boolean hasOkEmoji   = line.contains("✅");
        boolean hasWarnEmoji = line.contains("⚠️");
        boolean hasFailEmoji = line.contains("❌");

        boolean hasOkText   = line.matches("(?i).*\\b(excellent|pass(?!ed)|meets?\\s+requirement)\\b.*");
        boolean hasWarnText = line.matches("(?i).*\\b(missing|partial|incomplete)\\b.*");
        boolean hasFailText = line.matches("(?i).*\\b(does\\s+not\\s+meet|below|fail(ed)?|no\\s+matching)\\b.*");

        if (hasOkEmoji   || (!hasWarnEmoji && !hasFailEmoji && hasOkText))   return LineStatus.OK;
        if (hasWarnEmoji || (!hasOkEmoji   && !hasFailEmoji && hasWarnText)) return LineStatus.WARN;
        if (hasFailEmoji || (!hasOkEmoji   && !hasWarnEmoji && hasFailText)) return LineStatus.FAIL;
        return LineStatus.NEUTRAL;
    }

    // =========================================================================
    //  Helper utilities
    // =========================================================================

    private void addSafe(Document doc, Element element) {
        try { doc.add(element); } catch (DocumentException e) {
            throw new RuntimeException("Failed to add element to PDF", e);
        }
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    // =========================================================================
    //  Inner types
    // =========================================================================

    private enum LineStatus { OK, WARN, FAIL, NEUTRAL }

    private static class ReportSections {
        List<String> coreLines = Collections.emptyList();
        List<String> altLines  = Collections.emptyList();
        List<String> recLines  = Collections.emptyList();
    }

    // ─── Footer page event ────────────────────────────────────────────────────

    private static class FooterEvent extends PdfPageEventHelper {

        private PdfTemplate totalPagesTemplate;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float bottom = document.bottom() - 20;
            float width  = document.right() - document.left();
            float left   = document.left();

            // Footer background bar
            cb.setColorFill(new Color(248, 250, 252));
            cb.rectangle(left - 36, bottom - 10, width + 72, 28);
            cb.fill();

            // Footer text
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(80, 80, 80));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("This report was generated by EduVision Pro Analytics Engine", footerFont),
                    left + width / 2f, bottom + 6, 0);

            Font emailFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(0, 123, 191));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("For questions, contact: optimusinforservice@gmail.com", emailFont),
                    left + width / 2f, bottom - 1, 0);

            // Page number (left side)
            Font pageFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 120, 120));
            Phrase pagePhrase = new Phrase("Page " + writer.getPageNumber() + " of ", pageFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    pagePhrase, left + width - 30, bottom + 6, 0);

            // Placeholder for total pages
            cb.addTemplate(totalPagesTemplate, left + width - 28, bottom + 4);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            // Fill in the total page count once the document is finished
            totalPagesTemplate.beginText();
            Font pageFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 120, 120));
            totalPagesTemplate.setFontAndSize(pageFont.getBaseFont(), 7);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPagesTemplate.endText();
        }
    }
}
