/*
 * Copyright (C) 2026 mySupply ApS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysupply.phase4.peppolstandalone.controller;

import com.helger.base.string.StringHelper;
import com.mysupply.phase4.peppolstandalone.APConfig;
import com.mysupply.phase4.persistence.DocumentConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * REST controller providing an HTML admin view for Peppol Reports (TSR and EUSR)
 * and their lifecycle states: stored, valid, and sent to the Peppol infrastructure.
 * <p>
 * The page queries three schemas in the same PostgreSQL database:
 * <ul>
 *   <li>{@code phase4_peppol_reporting.peppol_reporting_item} — raw per-transaction items
 *       tracked for building reports.</li>
 *   <li>{@code phase4_peppol_reports.peppol_report} — the generated and stored TSR / EUSR
 *       XML payloads together with their validity flag.</li>
 *   <li>{@code phase4_peppol_reports.peppol_sending_report} — records produced when a
 *       report is successfully submitted to the Peppol infrastructure.</li>
 * </ul>
 */
@RestController
public class PeppolReportsViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolReportsViewController.class);

    private static final String REPORT_TYPE_TSR = "tsr10";
    private static final String REPORT_TYPE_EUSR = "eusr11";

    private static final DateTimeFormatter HTML_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Status filter: show all months regardless of report state. */
    private static final String STATUS_FILTER_ALL = "all";
    /** Status filter: months where the TSR has not been sent yet. */
    private static final String STATUS_FILTER_PENDING_TSR = "pending_tsr";
    /** Status filter: months where the EUSR has not been sent yet. */
    private static final String STATUS_FILTER_PENDING_EUSR = "pending_eusr";
    /** Status filter: months where either the TSR or EUSR has not been sent yet. */
    private static final String STATUS_FILTER_PENDING_ANY = "pending_any";
    /** Status filter: months where both TSR and EUSR have been successfully sent. */
    private static final String STATUS_FILTER_COMPLETE = "complete";

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 120;

    @PersistenceContext
    private EntityManager entityManager;

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /**
     * Renders an HTML overview page of all Peppol reports grouped by year/month, with optional
     * year and report-status filtering, and cursor-based pagination by year-month.
     *
     * @param token              The API token used for authentication.
     * @param yearFilter         Optional year to filter by (e.g. {@code "2026"}).
     * @param reportStatusFilter Optional status filter: {@code "all"}, {@code "pending_tsr"},
     *                           {@code "pending_eusr"}, {@code "pending_any"}, or {@code "complete"}.
     * @param pageSize           Number of months to display per page (1–120, default 12).
     * @param beforeYearMonthStr Cursor for pagination: show months strictly before this
     *                           year-month (ISO format {@code "yyyy-MM"}).
     * @return HTML page with the reports overview, or an error response.
     */
    @GetMapping(path = "/retriever/v1.0/viewReports", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewReports(
            @RequestParam("token") final String token,
            @RequestParam(value = "year", required = false) final String yearFilter,
            @RequestParam(value = "reportStatus", required = false) final String reportStatusFilter,
            @RequestParam(value = "pageSize", required = false, defaultValue = "12") final int pageSize,
            @RequestParam(value = "beforeYearMonth", required = false) final String beforeYearMonthStr) {

        ResponseEntity<String> errorResponse = validateToken(token);
        if (errorResponse != null) {
            return errorResponse;
        }

        try {
            Map<YearMonth, PeppolMonthlyReportSummary> allData = buildMonthlyReportSummary();

            List<Integer> availableYears = allData.keySet().stream()
                    .map(YearMonth::getYear)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            YearMonth beforeYearMonth = null;
            if (beforeYearMonthStr != null && !beforeYearMonthStr.isEmpty()) {
                beforeYearMonth = YearMonth.parse(beforeYearMonthStr);
            }

            int effectivePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
            long totalFiltered = countFiltered(allData, yearFilter, reportStatusFilter);

            // Fetch one extra entry to detect whether a next page exists.
            Map<YearMonth, PeppolMonthlyReportSummary> fetchedData =
                    applyFiltersAndPaginate(allData, yearFilter, reportStatusFilter,
                            beforeYearMonth, effectivePageSize + 1);

            boolean hasMore = fetchedData.size() > effectivePageSize;

            // Trim the over-fetched entry before rendering.
            Map<YearMonth, PeppolMonthlyReportSummary> pageData;
            if (hasMore) {
                pageData = new LinkedHashMap<>();
                int entryCount = 0;
                for (Map.Entry<YearMonth, PeppolMonthlyReportSummary> entry : fetchedData.entrySet()) {
                    if (entryCount >= effectivePageSize) {
                        break;
                    }
                    pageData.put(entry.getKey(), entry.getValue());
                    entryCount++;
                }
            } else {
                pageData = fetchedData;
            }

            // The last entry on this page becomes the cursor for the next page.
            YearMonth lastYearMonthOnPage = null;
            for (YearMonth yearMonth : pageData.keySet()) {
                lastYearMonthOnPage = yearMonth;
            }

            String htmlContent = buildHtmlPage(token, allData, pageData, availableYears,
                    yearFilter, reportStatusFilter, effectivePageSize, totalFiltered,
                    hasMore, lastYearMonthOnPage, beforeYearMonth);
            return ResponseEntity.ok(htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Failed to render Peppol reports view", ex);
            return ResponseEntity.internalServerError()
                    .body("Failed to load Peppol reports: " + ex.getMessage());
        }
    }

    /**
     * Downloads the stored XML content for a specific report type, year and month as an XML file.
     * If multiple runs exist for the same period, the most recently created XML is returned.
     *
     * @param token      The API token used for authentication.
     * @param reportType The report type identifier: {@code "tsr10"} or {@code "eusr11"}.
     * @param year       The report year (e.g. {@code 2026}).
     * @param month      The report month (1–12).
     * @return The report XML as a downloadable attachment, or 404 if not found.
     */
    @GetMapping(path = "/retriever/v1.0/downloadReport", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> downloadReport(
            @RequestParam("token") final String token,
            @RequestParam("type") final String reportType,
            @RequestParam("year") final int year,
            @RequestParam("month") final int month) {

        ResponseEntity<String> errorResponse = validateToken(token);
        if (errorResponse != null) {
            return ResponseEntity.badRequest().body("Invalid token".getBytes(StandardCharsets.UTF_8));
        }

        if (!REPORT_TYPE_TSR.equals(reportType) && !REPORT_TYPE_EUSR.equals(reportType)) {
            return ResponseEntity.badRequest().body("Invalid report type".getBytes(StandardCharsets.UTF_8));
        }

        try {
            String reportXml = queryReportXml(reportType, year, month);
            if (reportXml == null) {
                return ResponseEntity.notFound().build();
            }

            String typeLabel = REPORT_TYPE_TSR.equals(reportType) ? "TSR" : "EUSR";
            String filename = typeLabel + "_" + year + "_" + String.format("%02d", month) + ".xml";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(reportXml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            LOGGER.error("Failed to download report type={} year={} month={}", reportType, year, month, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private Map<YearMonth, PeppolMonthlyReportSummary> buildMonthlyReportSummary() {
        TreeMap<YearMonth, PeppolMonthlyReportSummary> summaries =
                new TreeMap<>(Comparator.reverseOrder());

        try {
            loadReportingItemCounts(summaries);
        } catch (Exception ex) {
            LOGGER.warn("Could not query reporting items — schema may not exist yet: {}", ex.getMessage());
        }

        try {
            loadStoredReports(summaries);
        } catch (Exception ex) {
            LOGGER.warn("Could not query stored reports — schema may not exist yet: {}", ex.getMessage());
        }

        try {
            loadSentReports(summaries);
        } catch (Exception ex) {
            LOGGER.warn("Could not query sending reports — schema may not exist yet: {}", ex.getMessage());
        }

        return summaries;
    }

    @SuppressWarnings("unchecked")
    private void loadReportingItemCounts(final Map<YearMonth, PeppolMonthlyReportSummary> summaries) {
        String sql = "SELECT EXTRACT(YEAR FROM exchangedt)::int AS year, " +
                     "EXTRACT(MONTH FROM exchangedt)::int AS month, " +
                     "COUNT(*) AS item_count " +
                     "FROM " + APConfig.getPeppolReportingTablePrefix() + DocumentConstants.PEPPOL_REPORTING_ITEM_TABLE_NAME + " " +
                     "GROUP BY EXTRACT(YEAR FROM exchangedt), EXTRACT(MONTH FROM exchangedt) " +
                     "ORDER BY year DESC, month DESC";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            YearMonth yearMonth = YearMonth.of(year, month);
            PeppolMonthlyReportSummary summary =
                    summaries.computeIfAbsent(yearMonth, k -> new PeppolMonthlyReportSummary());
            summary.setReportingItemCount(count);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStoredReports(final Map<YearMonth, PeppolMonthlyReportSummary> summaries) {
        // DISTINCT ON keeps only the latest repcreatedt for each (reptype, repyear, repmonth).
        String sql = "SELECT DISTINCT ON (reptype, repyear, repmonth) " +
                     "reptype, repyear, repmonth, repcreatedt, repvalid " +
                     "FROM " + APConfig.getPeppolReportsTablePrefix() + DocumentConstants.PEPPOL_REPORT_TABLE_NAME + " " +
                     "ORDER BY reptype, repyear, repmonth, repcreatedt DESC";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        for (Object[] row : rows) {
            String reportType = (String) row[0];
            int year = ((Number) row[1]).intValue();
            int month = ((Number) row[2]).intValue();
            LocalDateTime createdDate = toLocalDateTime(row[3]);
            boolean valid = (Boolean) row[4];

            YearMonth yearMonth = YearMonth.of(year, month);
            PeppolMonthlyReportSummary summary =
                    summaries.computeIfAbsent(yearMonth, k -> new PeppolMonthlyReportSummary());

            if (REPORT_TYPE_TSR.equals(reportType)) {
                summary.setTsrValid(valid);
                summary.setTsrCreatedDate(createdDate);
            } else if (REPORT_TYPE_EUSR.equals(reportType)) {
                summary.setEusrValid(valid);
                summary.setEusrCreatedDate(createdDate);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSentReports(final Map<YearMonth, PeppolMonthlyReportSummary> summaries) {
        // DISTINCT ON keeps only the latest repcreatedt for each (reptype, repyear, repmonth).
        String sql = "SELECT DISTINCT ON (reptype, repyear, repmonth) " +
                     "reptype, repyear, repmonth, repcreatedt " +
                     "FROM " + APConfig.getPeppolReportsTablePrefix() + DocumentConstants.PEPPOL_SENDING_REPORT_TABLE_NAME + " " +
                     "ORDER BY reptype, repyear, repmonth, repcreatedt DESC";

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        for (Object[] row : rows) {
            String reportType = (String) row[0];
            int year = ((Number) row[1]).intValue();
            int month = ((Number) row[2]).intValue();
            LocalDateTime sentDate = toLocalDateTime(row[3]);

            YearMonth yearMonth = YearMonth.of(year, month);
            PeppolMonthlyReportSummary summary =
                    summaries.computeIfAbsent(yearMonth, k -> new PeppolMonthlyReportSummary());

            if (REPORT_TYPE_TSR.equals(reportType)) {
                summary.setTsrSent(true);
                summary.setTsrSentDate(sentDate);
            } else if (REPORT_TYPE_EUSR.equals(reportType)) {
                summary.setEusrSent(true);
                summary.setEusrSentDate(sentDate);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String queryReportXml(final String reportType, final int year, final int month) {
        String sql = "SELECT report FROM " + APConfig.getPeppolReportsTablePrefix() + DocumentConstants.PEPPOL_REPORT_TABLE_NAME + " " +
                     "WHERE reptype = :reptype AND repyear = :year AND repmonth = :month " +
                     "ORDER BY repcreatedt DESC LIMIT 1";

        List<String> results = entityManager.createNativeQuery(sql)
                .setParameter("reptype", reportType)
                .setParameter("year", year)
                .setParameter("month", month)
                .getResultList();

        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private LocalDateTime toLocalDateTime(final Object timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toLocalDateTime();
        }
        if (timestamp instanceof LocalDateTime) {
            return (LocalDateTime) timestamp;
        }
        LOGGER.warn("Unexpected timestamp type encountered: {}", timestamp.getClass().getName());
        return null;
    }

    // -------------------------------------------------------------------------
    // Filtering and pagination helpers
    // -------------------------------------------------------------------------

    /**
     * Iterates {@code allData} (which is already sorted descending by year-month) and returns
     * a {@link LinkedHashMap} of at most {@code limit} entries that pass all active filters.
     * The caller fetches {@code pageSize + 1} entries to detect whether a next page exists.
     *
     * @param allData            Complete data set loaded from the database.
     * @param yearFilter         Optional year string to restrict results.
     * @param reportStatusFilter Optional status filter value.
     * @param beforeYearMonth    Cursor: only include months strictly before this value.
     * @param limit              Maximum number of entries to return.
     * @return Filtered and paginated entries preserving descending year-month order.
     */
    private Map<YearMonth, PeppolMonthlyReportSummary> applyFiltersAndPaginate(
            final Map<YearMonth, PeppolMonthlyReportSummary> allData,
            final String yearFilter,
            final String reportStatusFilter,
            final YearMonth beforeYearMonth,
            final int limit) {

        LinkedHashMap<YearMonth, PeppolMonthlyReportSummary> result = new LinkedHashMap<>();
        for (Map.Entry<YearMonth, PeppolMonthlyReportSummary> entry : allData.entrySet()) {
            YearMonth yearMonth = entry.getKey();
            PeppolMonthlyReportSummary summary = entry.getValue();

            // Cursor filter: skip months not strictly before the cursor.
            if (beforeYearMonth != null && !yearMonth.isBefore(beforeYearMonth)) {
                continue;
            }
            if (!matchesYearFilter(yearMonth, yearFilter)) {
                continue;
            }
            if (!matchesStatusFilter(summary, reportStatusFilter)) {
                continue;
            }

            result.put(yearMonth, summary);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    /**
     * Returns {@code true} when the given year-month matches the supplied year filter.
     *
     * @param yearMonth  The year-month to test.
     * @param yearFilter Optional year string; {@code null} or empty means "match all".
     * @return Whether the entry should be included.
     */
    private boolean matchesYearFilter(final YearMonth yearMonth, final String yearFilter) {
        if (yearFilter == null || yearFilter.isEmpty()) {
            return true;
        }
        try {
            return yearMonth.getYear() == Integer.parseInt(yearFilter);
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    /**
     * Returns {@code true} when the given monthly summary matches the supplied status filter.
     *
     * @param summary            The summary to test.
     * @param reportStatusFilter One of the {@code STATUS_FILTER_*} constants, or {@code null}.
     * @return Whether the entry should be included.
     */
    private boolean matchesStatusFilter(final PeppolMonthlyReportSummary summary,
                                         final String reportStatusFilter) {
        if (reportStatusFilter == null || reportStatusFilter.isEmpty()
                || STATUS_FILTER_ALL.equals(reportStatusFilter)) {
            return true;
        }
        return switch (reportStatusFilter) {
            case STATUS_FILTER_PENDING_TSR -> !summary.isTsrSent();
            case STATUS_FILTER_PENDING_EUSR -> !summary.isEusrSent();
            case STATUS_FILTER_PENDING_ANY -> !summary.isTsrSent() || !summary.isEusrSent();
            case STATUS_FILTER_COMPLETE -> summary.isTsrSent() && summary.isEusrSent();
            default -> true;
        };
    }

    /**
     * Counts the total number of months in {@code allData} that match the supplied filters,
     * ignoring any pagination cursor. Used for the "Showing X of Y" display.
     *
     * @param allData            Complete data set.
     * @param yearFilter         Optional year filter.
     * @param reportStatusFilter Optional status filter.
     * @return Count of matching months.
     */
    private long countFiltered(final Map<YearMonth, PeppolMonthlyReportSummary> allData,
                                final String yearFilter,
                                final String reportStatusFilter) {
        return allData.entrySet().stream()
                .filter(e -> matchesYearFilter(e.getKey(), yearFilter))
                .filter(e -> matchesStatusFilter(e.getValue(), reportStatusFilter))
                .count();
    }

    /**
     * Builds a relative URL for the {@code viewReports} page, encoding all active filter
     * and pagination parameters.
     *
     * @param token              The API token.
     * @param yearFilter         Optional year filter.
     * @param reportStatusFilter Optional status filter.
     * @param pageSize           Page size.
     * @param beforeYearMonth    Optional cursor; {@code null} means first page.
     * @return A relative URL string suitable for use in HTML {@code href} attributes.
     */
    private String buildUrl(final String token,
                             final String yearFilter,
                             final String reportStatusFilter,
                             final int pageSize,
                             final YearMonth beforeYearMonth) {
        StringBuilder url = new StringBuilder("viewReports?token=").append(escapeHtml(token));
        if (yearFilter != null && !yearFilter.isEmpty()) {
            url.append("&year=").append(escapeHtml(yearFilter));
        }
        if (reportStatusFilter != null && !reportStatusFilter.isEmpty()
                && !STATUS_FILTER_ALL.equals(reportStatusFilter)) {
            url.append("&reportStatus=").append(escapeHtml(reportStatusFilter));
        }
        url.append("&pageSize=").append(pageSize);
        if (beforeYearMonth != null) {
            url.append("&beforeYearMonth=").append(beforeYearMonth.toString());
        }
        return url.toString();
    }

    // -------------------------------------------------------------------------
    // HTML rendering
    // -------------------------------------------------------------------------

    private String buildHtmlPage(final String token,
                                  final Map<YearMonth, PeppolMonthlyReportSummary> allData,
                                  final Map<YearMonth, PeppolMonthlyReportSummary> pageData,
                                  final List<Integer> availableYears,
                                  final String yearFilter,
                                  final String reportStatusFilter,
                                  final int effectivePageSize,
                                  final long totalFiltered,
                                  final boolean hasMore,
                                  final YearMonth lastYearMonthOnPage,
                                  final YearMonth currentBeforeYearMonth) {

        long totalMonths = allData.size();
        long totalReportingItems = allData.values().stream()
                .mapToLong(PeppolMonthlyReportSummary::getReportingItemCount).sum();
        long totalTsrStored = allData.values().stream()
                .filter(s -> s.getTsrValid() != null).count();
        long totalTsrSent = allData.values().stream()
                .filter(PeppolMonthlyReportSummary::isTsrSent).count();
        long totalEusrStored = allData.values().stream()
                .filter(s -> s.getEusrValid() != null).count();
        long totalEusrSent = allData.values().stream()
                .filter(PeppolMonthlyReportSummary::isEusrSent).count();

        boolean hasActiveFilters = (yearFilter != null && !yearFilter.isEmpty())
                || (reportStatusFilter != null && !reportStatusFilter.isEmpty()
                        && !STATUS_FILTER_ALL.equals(reportStatusFilter));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Peppol Reports Status - phase4</title>");
        html.append(buildCss());
        html.append("</head><body>");

        html.append("<h1>Peppol Reports Status</h1>");

        // Navigation
        html.append("<div style=\"margin-bottom: 20px;\">");
        html.append("<a class=\"nav-btn\" href=\"viewDocuments?token=")
                .append(escapeHtml(token)).append("\">📋 Document Database</a> ");
        html.append("<a class=\"nav-btn refresh-btn\" href=\"viewReports?token=")
                .append(escapeHtml(token)).append("\">🔄 Refresh / Clear Filters</a>");
        html.append("</div>");

        // Global stats (always from the full unfiltered data set)
        html.append("<div class=\"stats-container\">");
        appendStatCard(html, "Total Months", totalMonths, "");
        appendStatCard(html, "Total Reporting Items", totalReportingItems, "items");
        appendStatCard(html, "TSR Stored", totalTsrStored, "tsr");
        appendStatCard(html, "TSR Sent", totalTsrSent, "tsr-sent");
        appendStatCard(html, "EUSR Stored", totalEusrStored, "eusr");
        appendStatCard(html, "EUSR Sent", totalEusrSent, "eusr-sent");
        html.append("</div>");

        // Search / filter form
        html.append("<div class=\"search-form\">");
        html.append("<h3>🔍 Filter Reports</h3>");
        html.append("<form method=\"get\" action=\"viewReports\">");
        html.append("<input type=\"hidden\" name=\"token\" value=\"").append(escapeHtml(token)).append("\">");
        html.append("<div class=\"search-grid\">");

        // Year dropdown — populated from years present in allData
        html.append("<div class=\"search-field\"><label>Year</label><select name=\"year\">");
        html.append("<option value=\"\">All years</option>");
        for (int year : availableYears) {
            boolean selected = String.valueOf(year).equals(yearFilter);
            html.append("<option value=\"").append(year).append("\"")
                    .append(selected ? " selected" : "").append(">").append(year).append("</option>");
        }
        html.append("</select></div>");

        // Report status dropdown
        html.append("<div class=\"search-field\"><label>Report Status</label><select name=\"reportStatus\">");
        appendStatusOption(html, STATUS_FILTER_ALL, "All statuses", reportStatusFilter);
        appendStatusOption(html, STATUS_FILTER_PENDING_ANY, "Any report pending", reportStatusFilter);
        appendStatusOption(html, STATUS_FILTER_PENDING_TSR, "TSR pending", reportStatusFilter);
        appendStatusOption(html, STATUS_FILTER_PENDING_EUSR, "EUSR pending", reportStatusFilter);
        appendStatusOption(html, STATUS_FILTER_COMPLETE, "Both reports sent", reportStatusFilter);
        html.append("</select></div>");

        // Page size dropdown
        html.append("<div class=\"search-field\"><label>Page Size</label><select name=\"pageSize\">");
        for (int size : new int[]{12, 24, 36}) {
            html.append("<option value=\"").append(size).append("\"")
                    .append(effectivePageSize == size ? " selected" : "")
                    .append(">").append(size).append(" months</option>");
        }
        html.append("</select></div>");

        html.append("</div>"); // search-grid
        html.append("<div class=\"search-buttons\">");
        html.append("<button type=\"submit\" class=\"search-btn\">🔍 Apply</button> ");
        html.append("<a href=\"viewReports?token=").append(escapeHtml(token))
                .append("\" class=\"clear-btn\">✕ Clear</a>");
        html.append("</div></form></div>"); // search-form

        // Active filter chips
        if (hasActiveFilters) {
            html.append("<div class=\"active-filters\"><strong>Active filters:</strong> ");
            if (yearFilter != null && !yearFilter.isEmpty()) {
                html.append("<span>Year: ").append(escapeHtml(yearFilter)).append("</span> ");
            }
            if (reportStatusFilter != null && !reportStatusFilter.isEmpty()
                    && !STATUS_FILTER_ALL.equals(reportStatusFilter)) {
                html.append("<span>Status: ")
                        .append(escapeHtml(getStatusLabel(reportStatusFilter))).append("</span>");
            }
            html.append("</div>");
        }

        // Pagination info bar
        html.append("<div class=\"pagination-info\">");
        html.append("<span>Showing <strong>").append(pageData.size()).append("</strong> of <strong>")
                .append(totalFiltered).append("</strong> ")
                .append(hasActiveFilters ? "filtered" : "total")
                .append(" months (page size: ").append(effectivePageSize).append(")</span>");
        html.append("<div>");

        if (currentBeforeYearMonth != null) {
            String firstPageUrl = buildUrl(token, yearFilter, reportStatusFilter, effectivePageSize, null);
            html.append("<a href=\"").append(firstPageUrl)
                    .append("\" class=\"search-btn\" style=\"margin-right: 8px;\">⏮ First Page</a>");
        }
        if (hasMore && lastYearMonthOnPage != null) {
            String nextPageUrl = buildUrl(token, yearFilter, reportStatusFilter,
                    effectivePageSize, lastYearMonthOnPage);
            html.append("<a href=\"").append(nextPageUrl)
                    .append("\" class=\"search-btn\">Next Page ⏭</a>");
        }

        html.append("</div></div>"); // pagination-info

        // Legend
        html.append("<div class=\"legend\">");
        html.append("<strong>Status legend:</strong> ");
        html.append("<span class=\"status-not-stored\">✗ Not stored</span> — ");
        html.append("<span class=\"status-invalid\">⚠ Stored (invalid)</span> — ");
        html.append("<span class=\"status-stored\">✓ Stored (valid)</span> — ");
        html.append("<span class=\"status-sent\">✓✓ Sent</span>");
        html.append("</div>");

        if (pageData.isEmpty()) {
            html.append("<div class=\"empty-state\">");
            if (hasActiveFilters) {
                html.append("<p>No months match the current filter criteria. ")
                        .append("<a href=\"viewReports?token=").append(escapeHtml(token))
                        .append("\">Clear filters</a> to see all data.</p>");
            } else {
                html.append("<p>No Peppol reporting data found. Reports are generated on a scheduled ")
                        .append("basis or via the <code>/create-tsr</code> and ")
                        .append("<code>/create-eusr</code> APIs.</p>");
            }
            html.append("</div>");
        } else {
            html.append("<div class=\"table-container\">");
            html.append("<table>");
            html.append("<thead><tr>");
            html.append("<th>Year-Month</th>");
            html.append("<th style=\"text-align: center;\"># Reporting Items</th>");
            html.append("<th>TSR Status</th>");
            html.append("<th>TSR Created</th>");
            html.append("<th>TSR Sent</th>");
            html.append("<th>TSR</th>");
            html.append("<th>EUSR Status</th>");
            html.append("<th>EUSR Created</th>");
            html.append("<th>EUSR Sent</th>");
            html.append("<th>EUSR</th>");
            html.append("</tr></thead><tbody>");

            for (Map.Entry<YearMonth, PeppolMonthlyReportSummary> entry : pageData.entrySet()) {
                YearMonth yearMonth = entry.getKey();
                PeppolMonthlyReportSummary summary = entry.getValue();

                html.append("<tr>");
                html.append("<td><strong>").append(yearMonth.toString()).append("</strong></td>");
                html.append("<td style=\"text-align: center;\">")
                        .append(summary.getReportingItemCount()).append("</td>");

                appendReportColumns(html, token, yearMonth, REPORT_TYPE_TSR,
                        summary.getTsrValid(), summary.getTsrCreatedDate(),
                        summary.isTsrSent(), summary.getTsrSentDate());

                appendReportColumns(html, token, yearMonth, REPORT_TYPE_EUSR,
                        summary.getEusrValid(), summary.getEusrCreatedDate(),
                        summary.isEusrSent(), summary.getEusrSentDate());

                html.append("</tr>");
            }

            html.append("</tbody></table></div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Appends four table cells for one report type: status, created date, sent date, and download.
     */
    private void appendReportColumns(final StringBuilder html,
                                      final String token,
                                      final YearMonth yearMonth,
                                      final String reportType,
                                      final Boolean valid,
                                      final LocalDateTime createdDate,
                                      final boolean sent,
                                      final LocalDateTime sentDate) {
        if (valid == null) {
            html.append("<td><span class=\"status-not-stored\">✗ Not stored</span></td>");
        } else if (!valid) {
            html.append("<td><span class=\"status-invalid\">⚠ Stored (invalid)</span></td>");
        } else if (!sent) {
            html.append("<td><span class=\"status-stored\">✓ Stored (valid)</span></td>");
        } else {
            html.append("<td><span class=\"status-sent\">✓✓ Sent</span></td>");
        }

        html.append("<td>").append(createdDate != null ? createdDate.format(HTML_DATE_TIME_FORMATTER) : "-").append("</td>");
        html.append("<td>").append(sentDate != null ? sentDate.format(HTML_DATE_TIME_FORMATTER) : "-").append("</td>");

        if (valid != null) {
            String downloadUrl = "downloadReport?token=" + escapeHtml(token)
                    + "&type=" + reportType
                    + "&year=" + yearMonth.getYear()
                    + "&month=" + yearMonth.getMonthValue();
            html.append("<td><a class=\"download-btn\" href=\"").append(downloadUrl)
                    .append("\">⬇ XML</a></td>");
        } else {
            html.append("<td>-</td>");
        }
    }

    private void appendStatCard(final StringBuilder html,
                                 final String label,
                                 final long value,
                                 final String cssModifier) {
        String cssClass = "stat-card" + (cssModifier.isEmpty() ? "" : " " + cssModifier);
        html.append("<div class=\"").append(cssClass).append("\">");
        html.append("<h3>").append(label).append("</h3>");
        html.append("<div class=\"number\">").append(value).append("</div>");
        html.append("</div>");
    }

    /**
     * Appends a single {@code <option>} element to the status filter dropdown.
     *
     * @param html          Target builder.
     * @param value         The option value attribute.
     * @param label         The visible label text.
     * @param currentFilter The currently active filter value, used to set {@code selected}.
     */
    private void appendStatusOption(final StringBuilder html,
                                     final String value,
                                     final String label,
                                     final String currentFilter) {
        boolean isSelected = value.equals(currentFilter)
                || (STATUS_FILTER_ALL.equals(value)
                        && (currentFilter == null || currentFilter.isEmpty()));
        html.append("<option value=\"").append(value).append("\"")
                .append(isSelected ? " selected" : "").append(">").append(label).append("</option>");
    }

    /**
     * Returns a human-readable label for a status filter value, used in the active-filter chips.
     *
     * @param reportStatusFilter A {@code STATUS_FILTER_*} constant value.
     * @return Display label string.
     */
    private String getStatusLabel(final String reportStatusFilter) {
        return switch (reportStatusFilter) {
            case STATUS_FILTER_PENDING_TSR -> "TSR pending";
            case STATUS_FILTER_PENDING_EUSR -> "EUSR pending";
            case STATUS_FILTER_PENDING_ANY -> "Any report pending";
            case STATUS_FILTER_COMPLETE -> "Both reports sent";
            default -> reportStatusFilter;
        };
    }

    private String buildCss() {
        return "<style>" +
               "* { font-family: sans-serif; box-sizing: border-box; }" +
               "body { margin: 20px; background-color: #f5f5f5; }" +
               "h1 { color: #333; }" +
               "code { font-family: 'Courier New', monospace; color: firebrick; background: #eee; padding: 1px 4px; }" +
               ".stats-container { display: flex; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }" +
               ".stat-card { background: white; padding: 16px 20px; border-radius: 8px;" +
               "  box-shadow: 0 2px 5px rgba(0,0,0,0.1); text-align: center; min-width: 130px; }" +
               ".stat-card h3 { margin: 0 0 6px; color: #666; font-size: 13px; text-transform: uppercase; }" +
               ".stat-card .number { font-size: 32px; font-weight: bold; color: #333; }" +
               ".stat-card.items .number { color: #455a64; }" +
               ".stat-card.tsr .number { color: #1565c0; }" +
               ".stat-card.tsr-sent .number { color: #0d47a1; }" +
               ".stat-card.eusr .number { color: #6a1b9a; }" +
               ".stat-card.eusr-sent .number { color: #4a148c; }" +
               ".search-form { background: white; padding: 20px; border-radius: 8px;" +
               "  box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin-bottom: 16px; }" +
               ".search-form h3 { margin: 0 0 14px; color: #333; font-size: 15px; }" +
               ".search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; }" +
               ".search-field { display: flex; flex-direction: column; }" +
               ".search-field label { font-size: 12px; color: #666; margin-bottom: 4px; }" +
               ".search-field select { padding: 8px; border: 1px solid #ddd; border-radius: 4px;" +
               "  font-size: 14px; background: white; }" +
               ".search-field select:focus { outline: none; border-color: #1565c0; }" +
               ".search-buttons { margin-top: 14px; display: flex; gap: 10px; align-items: center; }" +
               ".search-btn { padding: 8px 16px; background-color: #1565c0; color: white; border: none;" +
               "  border-radius: 4px; cursor: pointer; font-size: 14px; text-decoration: none;" +
               "  display: inline-block; }" +
               ".search-btn:hover { background-color: #0d47a1; }" +
               ".clear-btn { padding: 8px 16px; background-color: #f44336; color: white; border: none;" +
               "  border-radius: 4px; cursor: pointer; font-size: 14px; text-decoration: none;" +
               "  display: inline-block; }" +
               ".clear-btn:hover { background-color: #c62828; }" +
               ".active-filters { background: #e3f2fd; padding: 10px 16px; border-radius: 6px;" +
               "  margin-bottom: 14px; font-size: 13px; }" +
               ".active-filters span { background: #1565c0; color: white; padding: 2px 8px;" +
               "  border-radius: 3px; margin-right: 6px; font-size: 12px; }" +
               ".pagination-info { background: white; padding: 10px 16px; border-radius: 6px;" +
               "  box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin-bottom: 16px;" +
               "  display: flex; justify-content: space-between; align-items: center; font-size: 14px; }" +
               ".legend { background: white; padding: 10px 16px; border-radius: 6px;" +
               "  box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin-bottom: 18px; font-size: 13px; }" +
               ".table-container { overflow-x: auto; background: white; border-radius: 8px;" +
               "  box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
               "table { border-collapse: collapse; width: 100%; }" +
               "th, td { padding: 9px 14px; text-align: left; border-bottom: 1px solid #ddd; white-space: nowrap; }" +
               "th { background-color: #1565c0; color: white; font-size: 13px; }" +
               "tr:hover { background-color: #f5f5f5; }" +
               ".status-not-stored { color: #9e9e9e; }" +
               ".status-invalid { color: #e65100; font-weight: bold; }" +
               ".status-stored { color: #2e7d32; font-weight: bold; }" +
               ".status-sent { color: #1b5e20; font-weight: bold; }" +
               ".download-btn { padding: 4px 9px; background-color: #2196F3; color: white;" +
               "  border-radius: 3px; font-size: 11px; text-decoration: none; }" +
               ".download-btn:hover { background-color: #1565c0; }" +
               ".nav-btn { padding: 9px 18px; background-color: #607d8b; color: white;" +
               "  border-radius: 4px; font-size: 14px; text-decoration: none; display: inline-block; }" +
               ".nav-btn:hover { background-color: #455a64; }" +
               ".refresh-btn { background-color: #2196F3; }" +
               ".refresh-btn:hover { background-color: #1565c0; }" +
               ".empty-state { background: white; padding: 30px; border-radius: 8px;" +
               "  text-align: center; color: #666; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
               "</style>";
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private String escapeHtml(final String text) {
        if (text == null) {
            return "-";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private ResponseEntity<String> validateToken(final String token) {
        if (StringHelper.isEmpty(token)) {
            return ResponseEntity.badRequest().body("The specific token header is missing");
        }
        if (!token.equals(APConfig.getPhase4ApiRequiredToken())) {
            return ResponseEntity.badRequest()
                    .body("The specified token value does not match the configured required token");
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Inner data class
    // -------------------------------------------------------------------------

    /**
     * Holds the aggregated Peppol reporting data for a single year/month period.
     * Fields default to "not present" (null / false / 0) and are populated
     * incrementally by the three load methods.
     */
    private static final class PeppolMonthlyReportSummary {

        private long reportingItemCount;

        /** {@code null} = not stored, {@code false} = stored but invalid, {@code true} = stored valid. */
        private Boolean tsrValid;
        private LocalDateTime tsrCreatedDate;
        private boolean tsrSent;
        private LocalDateTime tsrSentDate;

        /** {@code null} = not stored, {@code false} = stored but invalid, {@code true} = stored valid. */
        private Boolean eusrValid;
        private LocalDateTime eusrCreatedDate;
        private boolean eusrSent;
        private LocalDateTime eusrSentDate;

        public long getReportingItemCount() { return reportingItemCount; }
        public void setReportingItemCount(final long count) { this.reportingItemCount = count; }

        public Boolean getTsrValid() { return tsrValid; }
        public void setTsrValid(final Boolean valid) { this.tsrValid = valid; }

        public LocalDateTime getTsrCreatedDate() { return tsrCreatedDate; }
        public void setTsrCreatedDate(final LocalDateTime date) { this.tsrCreatedDate = date; }

        public boolean isTsrSent() { return tsrSent; }
        public void setTsrSent(final boolean sent) { this.tsrSent = sent; }

        public LocalDateTime getTsrSentDate() { return tsrSentDate; }
        public void setTsrSentDate(final LocalDateTime date) { this.tsrSentDate = date; }

        public Boolean getEusrValid() { return eusrValid; }
        public void setEusrValid(final Boolean valid) { this.eusrValid = valid; }

        public LocalDateTime getEusrCreatedDate() { return eusrCreatedDate; }
        public void setEusrCreatedDate(final LocalDateTime date) { this.eusrCreatedDate = date; }

        public boolean isEusrSent() { return eusrSent; }
        public void setEusrSent(final boolean sent) { this.eusrSent = sent; }

        public LocalDateTime getEusrSentDate() { return eusrSentDate; }
        public void setEusrSentDate(final LocalDateTime date) { this.eusrSentDate = date; }
    }
}













