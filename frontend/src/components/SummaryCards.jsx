import { summaryDefinitions } from "../lib/utils";

export default function SummaryCards({ summary, lastRefreshText }) {
    return (
        <section className="hero-panel">
            <div className="panel-header">
                <span>System Pulse</span>
                <span>{lastRefreshText}</span>
            </div>
            <div className="summary-grid">
                {summaryDefinitions.map(([key, label]) => (
                    <article className="summary-card" key={key}>
                        <span className="summary-label">{label}</span>
                        <span className="summary-value">{summary[key] ?? 0}</span>
                    </article>
                ))}
            </div>
        </section>
    );
}