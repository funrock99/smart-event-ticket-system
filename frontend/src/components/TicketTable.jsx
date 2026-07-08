export default function TicketTable({ tickets }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Ticket Board</p>
                    <h2>派發工單</h2>
                </div>
                <span className="pill">{tickets.length} 筆工單</span>
            </div>
            <div className="table-shell">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>工單編號</th>
                        <th>來源</th>
                        <th>事件類型</th>
                        <th>Business Key</th>
                        <th>優先級</th>
                        <th>狀態</th>
                        <th>指派人</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tickets.map((ticket) => (
                        <tr key={ticket.id}>
                            <td>{ticket.id}</td>
                            <td>{ticket.ticketNo}</td>
                            <td>{ticket.source}</td>
                            <td>{ticket.eventType}</td>
                            <td>{ticket.businessKey}</td>
                            <td>{ticket.priority}</td>
                            <td><span className={`badge ${ticket.status}`}>{ticket.status}</span></td>
                            <td>{ticket.assignee || "-"}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
