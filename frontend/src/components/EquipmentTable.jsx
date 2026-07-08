export default function EquipmentTable({ sources }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Source Ranking</p>
                    <h2>事件來源排行</h2>
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
                    {sources.map((source) => (
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
        </section>
    );
}
