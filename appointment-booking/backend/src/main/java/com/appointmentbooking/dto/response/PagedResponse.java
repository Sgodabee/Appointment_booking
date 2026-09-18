package com.appointmentbooking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * PagedResponse — a strongly-typed wrapper for paginated API results.
 *
 * <p>Replaces the previous {@code Map<String, Object>} pattern, giving callers
 * a clear, compile-time-verified contract for any paginated endpoint.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "items":      [...],
 *   "total":      47,
 *   "page":       2,
 *   "size":       15,
 *   "totalPages": 4
 * }
 * </pre>
 *
 * @param <T> the type of each item in the page
 */
@Data
@Builder
public class PagedResponse<T> {

    /** The records for the current page. */
    private List<T> items;

    /** Total number of records across all pages. */
    private long total;

    /** Current 1-based page number. */
    private int page;

    /** Number of records per page. */
    private int size;

    /** Total number of pages. */
    private int totalPages;
}
