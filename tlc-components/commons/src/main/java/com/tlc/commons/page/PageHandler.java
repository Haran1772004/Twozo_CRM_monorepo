package com.tlc.commons.page;

import java.util.Objects;

/**
 * <p>
 * Performs the functions of the page handler
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public abstract class PageHandler<T> {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_PAGE = 0;

    private Page<T> currentPage;
    private long lastPage;
    private Long pageCount;
    private Long totalRecordsCount;
    private long orgId;
    private long userId;
    protected Integer recordStartingIndex;
    protected Integer recordsPerPage;

    protected PageHandler() {
    }

    protected PageHandler(final Integer recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    protected PageHandler(final Long pageCount, final Long totalRecordsCount) {
        this.pageCount = pageCount;
        this.totalRecordsCount = totalRecordsCount;
    }

    public PageHandler(final int start, final int limit) {
        this.recordStartingIndex = Math.max(start, DEFAULT_START);
        this.recordsPerPage = Math.min(limit, DEFAULT_LIMIT);
    }

    private void setPageCount(final Long pageCount) {
        this.pageCount = pageCount;
    }

    private void setTotalRecordsCount(final Long totalRecordsCount) {
        this.totalRecordsCount = totalRecordsCount;
    }

    private void setRecordStartingIndex(final Integer recordStartingIndex) {
        this.recordStartingIndex = recordStartingIndex;
    }

    private void setRecordsPerPage() {
        this.recordsPerPage = DEFAULT_LIMIT;
    }

    private void setCurrentPage(final Page<T> currentPage) {
        this.currentPage = currentPage;
    }

    private void setLastPage(final long lastPage) {
        this.lastPage = lastPage;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    /**
     * <p>
     * Retrieves the total page count based on the total records count and records per page
     * </p>
     *
     * @return the total number of pages
     */
    public Long getPageCount() {

        if (Objects.isNull(pageCount)) {
            setPageCount((long) Math.ceil((double) getTotalRecordsCount() / getRecordsPerPage()));
        }

        return pageCount;
    }

    /**
     * <p>
     * Retrieves the total count of records
     * </p>
     *
     * @return the total count of records
     */
    public Long getTotalRecordsCount() {

        if (Objects.isNull(totalRecordsCount)) {
            setTotalRecordsCount(totalRecordsCount());
        }

        return totalRecordsCount;
    }

    /**
     * <p>
     * Retrieves the starting index for a record within a page
     * </p>
     *
     * @return the starting index for the record
     */
    public Integer getRecordStartingIndex() {

        if (Objects.isNull(recordStartingIndex)) {
            setRecordStartingIndex(DEFAULT_START);
        }

        return recordStartingIndex;
    }

    /**
     * <p>
     * Retrieves the number of records per page to be displayed
     * </p>
     *
     * @return the number of records per page
     */
    public Integer getRecordsPerPage() {

        if (Objects.isNull(recordsPerPage)) {
            setRecordsPerPage();
        }

        return recordsPerPage;
    }

    /**
     * <p>
     * Retrieves the current page of data
     * </p>
     *
     * @return the current page of data
     */
    public Page<T> getCurrentPage() {

        if (Objects.isNull(currentPage)) {
            setCurrentPage(fetchPageData());
        }

        return currentPage;
    }

    /**
     * <<p>
     * Retrieves the page number of the last page in the handler
     * </p>
     *
     * @return the last page number
     */
    public long getLastPage() {
        return lastPage;
    }

    /**
     * <p>
     * Retrieves an organization id
     * </p>
     *
     * @return the organization id
     */
    public long getOrgId() {
        return orgId;
    }

    /**
     * <p>
     * Retrieves the user id
     * </p>
     *
     * @return the user id
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Checks if there is a next page available based on the total page count
     *
     * @return the true if there is a next page available.
     */
    public boolean hasNextPage() {
        return Objects.isNull(currentPage) ? DEFAULT_PAGE < getPageCount() : currentPage.getNumber() < getPageCount();
    }

    /**
     * <p>
     * Advances to the next page of data and return it
     * </p>
     *
     * @return the next page of data
     */
    public Page<T> goToNextPage() {
        final int number = getCurrentPage().getNumber();

        setRecordStartingIndex(getRecordStartingIndex() + getRecordsPerPage());
        final Page<T> page = fetchPageData();

        page.setNumber(number + DEFAULT_START);
        setLastPage(number);
        setCurrentPage(page);

        return page;
    }

    /**
     * <p>
     * Fetches a page of data
     * </p>
     *
     * @return the {@link Page}
     */
    protected abstract Page<T> fetchPageData();

    /**
     * <p>
     * Gets the total count
     * </p>
     *
     * @return the total count
     */
    protected abstract long totalRecordsCount();

    /**
     * <p>
     * A boolean indicating whether there are more records available
     * </p>
     *
     * @return true if there are more records available to fetch
     */
    public boolean hasMore() {
        return (getRecordStartingIndex() + getRecordsPerPage()) <= (Objects.isNull(totalRecordsCount) ? totalRecordsCount() : totalRecordsCount);
    }

    /**
     * Attempts to move to the next page if one is available.
     * <p>
     * This method first checks whether additional pages exist using {@link #hasMore()}.
     * If a next page is available, it advances to that page by invoking {@link #goToNextPage()}
     * and returns {@code true}. If no further pages exist, it performs no action and returns {@code false}.
     * <p>
     * This utility method simplifies pagination logic by combining the
     * availability check and navigation step into a single operation.
     *
     * @return {@code true} if the current position was successfully advanced to the next page;
     *         {@code false} if there are no more pages available.
     */
    public boolean navigateToNextPageIfAvailable() {
        if (hasMore()) {
            goToNextPage();
            return true;
        }
        return false;
    }
}
