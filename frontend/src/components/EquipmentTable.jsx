import { useState, useEffect } from "react";
import Pagination from "./Pagination";

export default function EquipmentTable({ sources }) {
    const [page, setPage] = useState(0);
    const size = 5;
    const totalPages = Math.ceil(sources.length / size);
    const paginatedSources = sources.slice(page * size, (page + 1) * size);

    useEffect(() => {
        setPage(0);
    }, [sources]);

    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Source Ranking</p>
                    <h2 style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        事件來源排行
                        <span style={{ fontSize: "14px", fontWeight: "normal", color: "#888" }}>(僅統計成功立案之有效事件)</span>
                    </h2>
                </div>
                <span className="pill">{sources.length} 個來源</span>
            </div>
            <div className="table-shell">
                <table>
                    <thead>
                    <tr>
                        <th>來源</th>
                        <th>事件數</th>
                        <th>最新事件類型</th>
                        <th>最高嚴重度</th>
                        <th>最近時間</th>
                    </tr>
                    </thead>
                    <tbody>
                    {paginatedSources.map((source) => (
                        <tr key={source.source}>
                            <td>{source.source}</td>
                            <td>{source.count}</td>
                            <td>{source.latestEventType}</td>
                            <td><span className={`badge ${source.highestSeverity}`}>{source.highestSeverity}</span></td>
                            <td>{new Date(source.latestOccurredAt).toLocaleString("zh-TW", { hour12: false })}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            {totalPages > 1 && (
                <Pagination 
                    pageInfo={{ number: page, totalPages: totalPages }} 
                    onPageChange={setPage} 
                />
            )}
        </section>
    );
}
