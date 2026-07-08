import { formatDateTime } from "../lib/utils";

export default function AlarmTimeline({ events }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Recent Events</p>
                    <h2>事件流</h2>
                </div>
                <span className="pill">{events.length} 筆事件</span>
            </div>
            <div className="alarm-list">
                {events.map((event) => (
                    <article className="alarm-item" key={event.id}>
                        <div className="alarm-topline">
                            <strong>{event.source} / {event.eventType}</strong>
                            <span className={`badge ${event.severity}`}>{event.severity}</span>
                        </div>
                        <div className="event-gridline">
                            <span className="event-chip">Business Key: {event.businessKey}</span>
                            <span className="event-chip">Payload: {event.payload ? "Available" : "None"}</span>
                        </div>
                        <div>{event.message}</div>
                        <div className="alarm-meta">發生時間：{formatDateTime(event.occurredAt)}</div>
                    </article>
                ))}
            </div>
        </section>
    );
}
