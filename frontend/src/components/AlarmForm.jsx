export default function AlarmForm({ form, onChange, onSubmit, result, isSubmitting, sourceRows }) {
    const suggestedSources = sourceRows.length ? sourceRows.map((source) => source.source) : [
        "payment-system",
        "core-banking",
        "customer-service",
        "monitoring-system",
        "batch-job",
        "api-gateway"
    ];

    return (
        <section className="card form-card">
            <div className="section-title">
                <p>Event Ingestion</p>
                <h2>事件上報</h2>
            </div>
            <form className="stack-form" onSubmit={onSubmit}>
                <label>
                    <span>事件來源</span>
                    <input list="event-source-options" maxLength={50} name="source" placeholder="payment-system" required type="text" value={form.source} onChange={onChange} />
                    <datalist id="event-source-options">
                        {suggestedSources.map((source) => (
                            <option key={source} value={source} />
                        ))}
                    </datalist>
                </label>
                <label>
                    <span>事件類型</span>
                    <select name="eventType" required value={form.eventType} onChange={onChange}>
                        <option value="TRANSACTION_ERROR">TRANSACTION_ERROR</option>
                        <option value="API_TIMEOUT">API_TIMEOUT</option>
                        <option value="CUSTOMER_CASE">CUSTOMER_CASE</option>
                        <option value="SYSTEM_ALERT">SYSTEM_ALERT</option>
                        <option value="BATCH_FAILED">BATCH_FAILED</option>
                        <option value="DUPLICATE_REQUEST">DUPLICATE_REQUEST</option>
                    </select>
                </label>
                <label>
                    <span>Business Key</span>
                    <input maxLength={100} name="businessKey" placeholder="TXN-10001" required type="text" value={form.businessKey} onChange={onChange} />
                </label>
                <label>
                    <span>嚴重程度</span>
                    <select name="severity" required value={form.severity} onChange={onChange}>
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                        <option value="CRITICAL">CRITICAL</option>
                    </select>
                </label>
                <label>
                    <span>事件訊息</span>
                    <textarea maxLength={500} name="message" placeholder="Transaction failed due to account validation error" required rows={4} value={form.message} onChange={onChange} />
                </label>
                <label>
                    <span>Payload</span>
                    <textarea maxLength={5000} name="payload" placeholder='{"transactionId":"TXN-10001"}' rows={4} value={form.payload} onChange={onChange} />
                </label>
                <label>
                    <span>Idempotency Key</span>
                    <input maxLength={100} name="idempotencyKey" placeholder="IDEMP-20260709-0001" type="text" value={form.idempotencyKey} onChange={onChange} />
                </label>
                <button className="primary-btn" disabled={isSubmitting} type="submit">
                    {isSubmitting ? "送出中..." : "送出事件"}
                </button>
            </form>
            <div className="result-box subtle-box">
                <pre>{result}</pre>
            </div>
        </section>
    );
}
