export default function TicketControl({ form, onChange, onAssign, onStatusUpdate, result, allowedStatuses, isAssigning, isUpdatingStatus }) {
    return (
        <section className="card form-card">
            <div className="section-title">
                <p>Ticket Control</p>
                <h2>工單處理</h2>
            </div>
            <form className="stack-form" onSubmit={onAssign}>
                <label>
                    <span>工單編號</span>
                    <input maxLength={30} name="ticketNo" placeholder="MT-20260628-0001" required type="text" value={form.ticketNo} onChange={onChange} />
                </label>
                <label>
                    <span>指派人員</span>
                    <input maxLength={50} name="assignee" placeholder="Dennis" required type="text" value={form.assignee} onChange={onChange} />
                </label>
                <button className="secondary-btn" disabled={isAssigning} type="submit">
                    {isAssigning ? "指派中..." : "指派工單"}
                </button>
            </form>
            <div className="status-actions">
                {[
                    ["IN_PROGRESS", "標記處理中"],
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