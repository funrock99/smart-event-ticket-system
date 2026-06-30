export default function AlarmForm({ equipments, form, onChange, onSubmit, result, isSubmitting }) {
    return (
        <section className="card form-card">
            <div className="section-title">
                <p>Alarm Simulation</p>
                <h2>異常上報</h2>
            </div>
            <form className="stack-form" onSubmit={onSubmit}>
                <label>
                    <span>設備代號</span>
                    <select name="equipmentId" required value={form.equipmentId} onChange={onChange}>
                        <option value="">請選擇設備</option>
                        {equipments.map((equipment) => (
                            <option key={equipment.equipmentId} value={equipment.equipmentId}>
                                {equipment.equipmentId} | {equipment.name}
                            </option>
                        ))}
                    </select>
                </label>
                <label>
                    <span>異常代碼</span>
                    <input maxLength={50} name="alarmCode" placeholder="TEMP_HIGH" required type="text" value={form.alarmCode} onChange={onChange} />
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
                    <span>異常訊息</span>
                    <textarea maxLength={500} name="message" placeholder="Temperature exceeded threshold" required rows={4} value={form.message} onChange={onChange} />
                </label>
                <button className="primary-btn" disabled={isSubmitting} type="submit">
                    {isSubmitting ? "送出中..." : "送出異常事件"}
                </button>
            </form>
            <div className="result-box subtle-box">
                <pre>{result}</pre>
            </div>
        </section>
    );
}