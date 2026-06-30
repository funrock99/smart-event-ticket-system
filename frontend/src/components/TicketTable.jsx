export default function TicketTable({ tickets }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Ticket Board</p>
                    <h2>維修工單</h2>
                </div>
                <span className="pill">{tickets.length} 筆工單</span>
            </div>
            <div className="table-shell">
                <table>
                    <thead>
                    <tr>
                        <th>工單編號</th>
                        <th>設備</th>
                        <th>異常代碼</th>
                        <th>優先級</th>
                        <th>狀態</th>
                        <th>指派人</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tickets.map((ticket) => (
                        <tr key={ticket.ticketNo}>
                            <td>{ticket.ticketNo}</td>
                            <td>{ticket.equipmentId}</td>
                            <td>{ticket.alarmCode}</td>
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