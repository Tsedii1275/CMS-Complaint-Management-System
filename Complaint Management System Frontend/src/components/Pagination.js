import React from 'react';

/**
 * System-Wide Reusable Pagination Component
 * 
 * Layout:
 * Left side:  Showing X–Y of N records
 * Right side: Rows per page: [10 ▾]  ‹ Previous  1  2  3  ...  N  Next ›
 */
const Pagination = ({
    currentPage = 1,
    pageSize = 10,
    totalRecords = 0,
    onPageChange,
    onPageSizeChange,
    pageSizeOptions = [10, 25, 50, 100],
    showSizeChanger = true,
    itemUnit = 'records'
}) => {
    if (!totalRecords || totalRecords <= 0) {
        return null;
    }

    const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
    const validCurrentPage = Math.min(Math.max(1, currentPage), totalPages);

    const startRecord = (validCurrentPage - 1) * pageSize + 1;
    const endRecord = Math.min(validCurrentPage * pageSize, totalRecords);

    const handlePageChange = (page) => {
        if (page >= 1 && page <= totalPages && page !== validCurrentPage) {
            if (onPageChange) {
                onPageChange(page);
            }
        }
    };

    const handleSizeChange = (e) => {
        const newSize = Number(e.target.value);
        if (onPageSizeChange) {
            onPageSizeChange(newSize);
        }
        if (onPageChange) {
            onPageChange(1); // Reset to page 1 when page size changes
        }
    };

    // Generate page numbers array with intelligent ellipsis handling
    const getPageNumbers = () => {
        const pages = [];

        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) {
                pages.push(i);
            }
        } else {
            pages.push(1);

            let start = Math.max(2, validCurrentPage - 1);
            let end = Math.min(totalPages - 1, validCurrentPage + 1);

            if (validCurrentPage <= 3) {
                end = 4;
            }
            if (validCurrentPage >= totalPages - 2) {
                start = totalPages - 3;
            }

            if (start > 2) {
                pages.push('ellipsis-before');
            }

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }

            if (end < totalPages - 1) {
                pages.push('ellipsis-after');
            }

            pages.push(totalPages);
        }

        return pages;
    };

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '12px',
            padding: '12px 16px',
            marginTop: '12px',
            background: '#ffffff',
            borderRadius: '8px',
            border: '1px solid #e5e7eb',
            fontSize: '13px',
            color: '#374151',
            fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
        }}>
            {/* Left side: Showing X–Y of N records */}
            <div style={{ fontWeight: 500, color: '#4b5563' }}>
                Showing <strong style={{ color: '#111827' }}>{startRecord}–{endRecord}</strong> of <strong style={{ color: '#111827' }}>{totalRecords}</strong> {itemUnit}
            </div>

            {/* Right side: Rows per page + Page numbers + Prev/Next */}
            <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                flexWrap: 'wrap'
            }}>
                {/* Page size dropdown */}
                {showSizeChanger && onPageSizeChange && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <span style={{ color: '#6b7280', fontSize: '12px' }}>Rows per page:</span>
                        <select
                            value={pageSize}
                            onChange={handleSizeChange}
                            style={{
                                padding: '4px 8px',
                                borderRadius: '6px',
                                border: '1px solid #d1d5db',
                                background: '#ffffff',
                                color: '#111827',
                                fontSize: '12px',
                                fontWeight: 500,
                                outline: 'none',
                                cursor: 'pointer'
                            }}
                        >
                            {pageSizeOptions.map((size) => (
                                <option key={size} value={size}>
                                    {size}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                {/* Page controls */}
                {totalPages > 1 && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        {/* Previous Button */}
                        <button
                            type="button"
                            disabled={validCurrentPage === 1}
                            onClick={() => handlePageChange(validCurrentPage - 1)}
                            style={{
                                padding: '5px 10px',
                                borderRadius: '6px',
                                border: '1px solid #d1d5db',
                                background: validCurrentPage === 1 ? '#f3f4f6' : '#ffffff',
                                color: validCurrentPage === 1 ? '#9ca3af' : '#374151',
                                fontSize: '12px',
                                fontWeight: 500,
                                cursor: validCurrentPage === 1 ? 'not-allowed' : 'pointer',
                                transition: 'all 0.15s ease-in-out'
                            }}
                        >
                            ‹ Previous
                        </button>

                        {/* Page number buttons */}
                        {getPageNumbers().map((p) => {
                            if (p === 'ellipsis-before' || p === 'ellipsis-after') {
                                return (
                                    <span key={p} style={{ padding: '0 6px', color: '#9ca3af', userSelect: 'none' }}>
                                        …
                                    </span>
                                );
                            }
                            const isActive = p === validCurrentPage;
                            return (
                                <button
                                    key={`page-${p}`}
                                    type="button"
                                    onClick={() => handlePageChange(p)}
                                    style={{
                                        minWidth: '28px',
                                        height: '28px',
                                        padding: '0 6px',
                                        borderRadius: '6px',
                                        border: isActive ? '1px solid #1e3a8a' : '1px solid #d1d5db',
                                        background: isActive ? '#1e3a8a' : '#ffffff',
                                        color: isActive ? '#ffffff' : '#374151',
                                        fontSize: '12px',
                                        fontWeight: isActive ? 700 : 500,
                                        cursor: 'pointer',
                                        transition: 'all 0.15s ease-in-out'
                                    }}
                                >
                                    {p}
                                </button>
                            );
                        })}

                        {/* Next Button */}
                        <button
                            type="button"
                            disabled={validCurrentPage === totalPages}
                            onClick={() => handlePageChange(validCurrentPage + 1)}
                            style={{
                                padding: '5px 10px',
                                borderRadius: '6px',
                                border: '1px solid #d1d5db',
                                background: validCurrentPage === totalPages ? '#f3f4f6' : '#ffffff',
                                color: validCurrentPage === totalPages ? '#9ca3af' : '#374151',
                                fontSize: '12px',
                                fontWeight: 500,
                                cursor: validCurrentPage === totalPages ? 'not-allowed' : 'pointer',
                                transition: 'all 0.15s ease-in-out'
                            }}
                        >
                            Next ›
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Pagination;
