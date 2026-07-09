export default function Pagination({ pageInfo, onPageChange }) {
    if (!pageInfo || pageInfo.totalPages <= 1) return null;

    return (
        <div className="pagination">
            <button 
                className="ghost-link" 
                disabled={pageInfo.number === 0} 
                onClick={() => onPageChange(pageInfo.number - 1)}
            >
                上一頁
            </button>
            <span className="pagination-text">
                第 {pageInfo.number + 1} 頁 / 共 {pageInfo.totalPages} 頁
            </span>
            <button 
                className="ghost-link" 
                disabled={pageInfo.number >= pageInfo.totalPages - 1} 
                onClick={() => onPageChange(pageInfo.number + 1)}
            >
                下一頁
            </button>
        </div>
    );
}
