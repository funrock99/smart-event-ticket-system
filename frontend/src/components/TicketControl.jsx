export default function TicketControl({ form, onChange, onAssign, onStatusUpdate, result, allowedStatuses, isAssigning, isUpdatingStatus, tickets }) {
    return (
        <section className="card form-card">
            <div className="section-title">
                <p>Ticket Control</p>
                <h2>工單派發與狀態流轉</h2>
            </div>
            <form className="stack-form" onSubmit={onAssign}>
                <label>
                    <span>選擇工單</span>
                    <select name="ticketId" required value={form.ticketId} onChange={onChange}>
                        <option value="">請選擇工單</option>
                        {tickets.map((ticket) => (
                            <option key={ticket.id} value={ticket.id}>
                                #{ticket.id} | {ticket.ticketNo} | {ticket.source}
                            </option>
                        ))}
                    </select>
                </label>
                <label>
                    <span>指派人員</span>
                    <input maxLength={50} name="assignee" placeholder="Ops-Desk" required type="text" value={form.assignee} onChange={onChange} />
                </label>
                <button className="secondary-btn" disabled={isAssigning} type="submit">
                    {isAssigning ? "指派中..." : "指派工單"}
                </button>
            </form>
            <div className="status-actions">
                {[
                    ["PROCESSING", "標記處理中"],
                    ["RESOLVED", "標記已解決"],
                    ["CLOSED", "標記已關閉"]
                ].map(([status, label]) => (
                    <button className="status-btn" disabled={!allowedStatuses.includes(status) || isUpdatingStatus} key={status} type="button" onClick={() => onStatusUpdate(status)}>
                        {label}
                    </button>
                ))}
            </div>
            <div className="result-box subtle-box">
                <pre>{result}</pre>
            </div>
        </section>
    );
}
