import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "./api/client";
import AlarmForm from "./components/AlarmForm";
import AlarmTimeline from "./components/AlarmTimeline";
import EquipmentTable from "./components/EquipmentTable";
import SummaryCards from "./components/SummaryCards";
import TicketControl from "./components/TicketControl";
import TicketTable from "./components/TicketTable";
import Toast from "./components/Toast";
import { createInitialSummary, getAllowedNextStatuses, normalizeErrorMessage, stringifyResult } from "./lib/utils";

const initialEventForm = {
    source: "payment-system",
    eventType: "TRANSACTION_ERROR",
    businessKey: "TXN-10001",
    severity: "HIGH",
    message: "Transaction failed due to account validation error",
    payload: '{"transactionId":"TXN-10001"}',
    idempotencyKey: ""
};

const initialTicketForm = {
    ticketId: "",
    assignee: "Ops-Desk"
};

export default function App() {
    const [summary, setSummary] = useState(createInitialSummary);
    const [tickets, setTickets] = useState([]);
    const [events, setEvents] = useState([]);
    const [lastRefreshText, setLastRefreshText] = useState("尚未更新");
    const [eventForm, setEventForm] = useState(initialEventForm);
    const [ticketForm, setTicketForm] = useState(initialTicketForm);
    const [lastTicketId, setLastTicketId] = useState("");
    const [eventResult, setEventResult] = useState("尚未送出事件。");
    const [ticketResult, setTicketResult] = useState("尚未操作工單。");
    const [toast, setToast] = useState({ message: "", type: "" });
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [isSubmittingEvent, setIsSubmittingEvent] = useState(false);
    const [isAssigningTicket, setIsAssigningTicket] = useState(false);
    const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

    useEffect(() => {
        refreshAll({ silent: false }).catch((error) => {
            showToast("前端初始化失敗", "error");
            setEventResult(`初始化失敗\n${normalizeErrorMessage(error)}`);
        });
    }, []);

    useEffect(() => {
        if (!toast.message) {
            return undefined;
        }

        const timer = window.setTimeout(() => {
            setToast({ message: "", type: "" });
        }, 3200);

        return () => window.clearTimeout(timer);
    }, [toast]);

    const sourceRows = useMemo(() => {
        const sourceMap = new Map();
        events.forEach((event) => {
            const current = sourceMap.get(event.source) || {
                source: event.source,
                count: 0,
                highestSeverity: event.severity,
                latestEventType: event.eventType,
                latestOccurredAt: event.occurredAt
            };
            current.count += 1;
            current.latestEventType = event.eventType;
            current.latestOccurredAt = event.occurredAt;
            current.highestSeverity = compareSeverity(current.highestSeverity, event.severity) >= 0
                ? current.highestSeverity
                : event.severity;
            sourceMap.set(event.source, current);
        });

        return [...sourceMap.values()].sort((left, right) => {
            if (right.count !== left.count) {
                return right.count - left.count;
            }
            return left.source.localeCompare(right.source);
        });
    }, [events]);

    function showToast(message, type = "") {
        setToast({ message, type });
    }

    async function refreshAll(options = {}) {
        const { silent = true } = options;
        if (!silent) {
            setIsRefreshing(true);
        }

        try {
            const [nextSummary, nextTickets, nextEvents] = await Promise.all([
                apiFetch("/api/dashboard/summary"),
                apiFetch("/api/tickets?page=0&size=20&sort=createdAt,desc"),
                apiFetch("/api/events?page=0&size=20&sort=occurredAt,desc")
            ]);

            const safeTickets = Array.isArray(nextTickets?.content) ? nextTickets.content : [];
            const safeEvents = Array.isArray(nextEvents?.content) ? nextEvents.content : [];

            setSummary(nextSummary);
            setTickets(safeTickets);
            setEvents(safeEvents);
            setLastRefreshText(new Date().toLocaleTimeString("zh-TW", { hour12: false }));
            setTicketForm((current) => {
                const hasSelectedTicket = safeTickets.some((ticket) => String(ticket.id) === current.ticketId);
                if (hasSelectedTicket || !safeTickets.length) {
                    return current;
                }
                return { ...current, ticketId: String(safeTickets[0].id) };
            });
        } finally {
            if (!silent) {
                setIsRefreshing(false);
            }
        }
    }

    function handleEventFormChange(event) {
        const { name, value } = event.target;
        setEventForm((current) => ({ ...current, [name]: value }));
    }

    function handleTicketFormChange(event) {
        const { name, value } = event.target;
        setTicketForm((current) => ({ ...current, [name]: value }));
    }

    async function handleEventSubmit(submitEvent) {
        submitEvent.preventDefault();
        setIsSubmittingEvent(true);

        try {
            const headers = {};
            if (eventForm.idempotencyKey.trim()) {
                headers["Idempotency-Key"] = eventForm.idempotencyKey.trim();
            }

            const result = await apiFetch("/api/events", {
                method: "POST",
                headers,
                body: JSON.stringify({
                    source: eventForm.source,
                    eventType: eventForm.eventType,
                    businessKey: eventForm.businessKey,
                    severity: eventForm.severity,
                    message: eventForm.message,
                    payload: eventForm.payload || null
                })
            });

            if (result.ticketId) {
                setLastTicketId(String(result.ticketId));
                setTicketForm((current) => ({ ...current, ticketId: String(result.ticketId) }));
            }
            setEventResult(stringifyResult("事件接收結果", result));
            showToast(result.message || "事件已送出");

            try {
                await refreshAll();
            } catch (refreshError) {
                showToast(`事件已處理，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
            }
        } catch (error) {
            setEventResult(`事件送出失敗\n${normalizeErrorMessage(error)}`);
            showToast("事件送出失敗", "error");
        } finally {
            setIsSubmittingEvent(false);
        }
    }

    async function handleAssignSubmit(assignEvent) {
        assignEvent.preventDefault();
        const targetTicketId = ticketForm.ticketId || lastTicketId;
        if (!targetTicketId) {
            showToast("請先選擇工單", "error");
            return;
        }

        setIsAssigningTicket(true);

        try {
            const result = await apiFetch(`/api/tickets/${encodeURIComponent(targetTicketId)}/assign`, {
                method: "PUT",
                body: JSON.stringify({ assignee: ticketForm.assignee })
            });

            setLastTicketId(String(targetTicketId));
            setTicketResult(stringifyResult("工單指派成功", result));
            showToast(`工單 ${result.ticketNo} 已指派給 ${ticketForm.assignee}`);

            try {
                await refreshAll();
            } catch (refreshError) {
                showToast(`指派已成功，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
            }
        } catch (error) {
            setTicketResult(`工單指派失敗\n${normalizeErrorMessage(error)}`);
            showToast("工單指派失敗", "error");
        } finally {
            setIsAssigningTicket(false);
        }
    }

    async function handleStatusUpdate(status) {
        const targetTicketId = ticketForm.ticketId || lastTicketId;
        if (!targetTicketId) {
            showToast("請先選擇工單", "error");
            return;
        }

        setIsUpdatingStatus(true);

        try {
            const result = await apiFetch(`/api/tickets/${encodeURIComponent(targetTicketId)}/status`, {
                method: "PUT",
                body: JSON.stringify({ status })
            });

            setLastTicketId(String(targetTicketId));
            setTicketResult(stringifyResult(`工單狀態已更新為 ${status}`, result));
            showToast(`工單 ${result.ticketNo} -> ${status}`);

            try {
                await refreshAll();
            } catch (refreshError) {
                showToast(`狀態已更新，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
            }
        } catch (error) {
            setTicketResult(`工單狀態更新失敗\n${normalizeErrorMessage(error)}`);
            showToast("工單狀態更新失敗", "error");
        } finally {
            setIsUpdatingStatus(false);
        }
    }

    const activeTicketId = ticketForm.ticketId || lastTicketId;
    const currentTicket = tickets.find((ticket) => String(ticket.id) === String(activeTicketId));
    const allowedStatuses = currentTicket ? getAllowedNextStatuses(currentTicket.status) : [];

    async function handleRefreshClick() {
        try {
            await refreshAll({ silent: false });
            showToast("資料已重新整理");
        } catch {
            showToast("重新整理失敗", "error");
        }
    }

    return (
        <>
            <div className="page-shell">
                <header className="hero">
                    <div className="hero-copy">
                        <p className="eyebrow">Smart Event Ticket System</p>
                        <h1>高併發事件接收與工單派發平台</h1>
                        <p className="hero-text">
                            這個 Dashboard 直接串接 Spring Boot API，展示事件接收、Redis 去重、工單派發與事件統計，模擬 payment-system、customer-service、monitoring-system 等來源的集中上報情境。
                        </p>
                        <div className="hero-actions">
                            <button className="primary-btn" disabled={isRefreshing} type="button" onClick={handleRefreshClick}>
                                {isRefreshing ? "整理中..." : "重新整理事件看板"}
                            </button>
                            <a className="ghost-link" href="/swagger-ui/index.html" rel="noreferrer" target="_blank">開啟 Swagger</a>
                        </div>
                    </div>
                    <SummaryCards lastRefreshText={lastRefreshText} summary={summary} />
                </header>
                <main className="content-grid">
                    <AlarmForm form={eventForm} isSubmitting={isSubmittingEvent} result={eventResult} sourceRows={sourceRows} onChange={handleEventFormChange} onSubmit={handleEventSubmit} />
                    <TicketControl allowedStatuses={allowedStatuses} form={ticketForm} isAssigning={isAssigningTicket} isUpdatingStatus={isUpdatingStatus} result={ticketResult} tickets={tickets} onAssign={handleAssignSubmit} onChange={handleTicketFormChange} onStatusUpdate={handleStatusUpdate} />
                    <EquipmentTable sources={sourceRows} />
                    <TicketTable tickets={tickets} />
                    <AlarmTimeline events={events} />
                </main>
            </div>
            <Toast toast={toast} />
        </>
    );
}

function compareSeverity(left, right) {
    const order = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };
    return (order[left] || 0) - (order[right] || 0);
}
