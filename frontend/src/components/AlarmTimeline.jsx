import { formatDateTime } from "../lib/utils";

export default function AlarmTimeline({ alarms }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Alarm Timeline</p>
                    <h2>異常事件</h2>
                </div>
                <span className="pill">{alarms.length} 筆事件</span>
            </div>
            <div className="alarm-list">
                {alarms.map((alarm) => {
                    const severityClass = alarm.severity === "CRITICAL" || alarm.severity === "HIGH" ? "DOWN" : "MAINTENANCE";
                    return (
                        <article className="alarm-item" key={alarm.id}>
                            <div className="alarm-topline">
                                <strong>{alarm.equipmentId} / {alarm.alarmCode}</strong>
                                <span className={`badge ${severityClass}`}>{alarm.severity}</span>
                            </div>
                            <div>{alarm.message}</div>
                            <div className="alarm-meta">發生時間：{formatDateTime(alarm.occurredAt)}</div>
                        </article>
                    );
                })}
            </div>
        </section>
    );
}