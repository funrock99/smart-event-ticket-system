import { useEffect, useState } from "react";
import { apiFetch } from "./api/client";
import AlarmForm from "./components/AlarmForm";
import AlarmTimeline from "./components/AlarmTimeline";
import EquipmentTable from "./components/EquipmentTable";
import SummaryCards from "./components/SummaryCards";
import TicketControl from "./components/TicketControl";
import TicketTable from "./components/TicketTable";
import Toast from "./components/Toast";
import { createInitialSummary, getAllowedNextStatuses, normalizeErrorMessage, stringifyResult } from "./lib/utils";

const initialAlarmForm = {
    equipmentId: "",
    alarmCode: "TEMP_HIGH",
    severity: "HIGH",
    message: "Temperature exceeded threshold"
};

const initialTicketForm = {
    ticketNo: "",
    assignee: "Dennis"
};

export default function App() {
    const [summary, setSummary] = useState(createInitialSummary);
    const [equipments, setEquipments] = useState([]);
    const [tickets, setTickets] = useState([]);
    const [alarms, setAlarms] = useState([]);
    const [lastRefreshText, setLastRefreshText] = useState("尚未更新");
    const [alarmForm, setAlarmForm] = useState(initialAlarmForm);
    const [ticketForm, setTicketForm] = useState(initialTicketForm);
    const [lastTicketNo, setLastTicketNo] = useState("");
    const [alarmResult, setAlarmResult] = useState("尚未送出異常事件。");
    const [ticketResult, setTicketResult] = useState("尚未操作工單。");
    const [toast, setToast] = useState({ message: "", type: "" });
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [isSubmittingAlarm, setIsSubmittingAlarm] = useState(false);
    const [isAssigningTicket, setIsAssigningTicket] = useState(false);
    const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

    useEffect(() => {
        refreshAll({ silent: false }).catch((error) => {
            showToast("前端初始化失敗", "error");
            setAlarmResult(`初始化失敗\n${normalizeErrorMessage(error)}`);
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

    function showToast(message, type = "") {
        setToast({ message, type });
    }

    async function refreshAll(options = {}) {
        const { silent = true } = options;
        if (!silent) {
            setIsRefreshing(true);
        }

        try {
            const [nextSummary, nextEquipments, nextTickets, nextAlarms] = await Promise.all([
                apiFetch("/api/dashboard/summary"),
                apiFetch("/api/equipments"),
                apiFetch("/api/tickets"),
                apiFetch("/api/alarms")
            ]);

            const safeEquipments = Array.isArray(nextEquipments) ? nextEquipments : [nextEquipments];
            const safeTickets = Array.isArray(nextTickets) ? nextTickets : [nextTickets];
            const safeAlarms = Array.isArray(nextAlarms) ? nextAlarms : [nextAlarms];

            setSummary(nextSummary);
            setEquipments(safeEquipments);
            setTickets(safeTickets);
            setAlarms(safeAlarms);
            setLastRefreshText(new Date().toLocaleTimeString("zh-TW", { hour12: false }));
            setAlarmForm((current) => {
                const hasSelectedEquipment = safeEquipments.some((equipment) => equipment.equipmentId === current.equipmentId);
                if (hasSelectedEquipment) {
                    return current;
                }

                return {
                    ...current,
                    equipmentId: safeEquipments[0]?.equipmentId || ""
                };
            });
        } finally {
            if (!silent) {
                setIsRefreshing(false);
            }
        }
    }

    function handleAlarmFormChange(event) {
        const { name, value } = event.target;
        setAlarmForm((current) => ({ ...current, [name]: value }));
    }

    function handleTicketFormChange(event) {
        const { name, value } = event.target;
        setTicketForm((current) => ({ ...current, [name]: value }));
    }

    async function handleAlarmSubmit(event) {
        event.preventDefault();
        setIsSubmittingAlarm(true);

        try {
            const result = await apiFetch("/api/alarms", {
                method: "POST",
                body: JSON.stringify(alarmForm)
            });

            setLastTicketNo(result.ticketNo);
            setTicketForm((current) => ({ ...current, ticketNo: result.ticketNo }));
            setAlarmResult(stringifyResult("異常上報成功", result));
            showToast(`已建立工單 ${result.ticketNo}`);

            try {
                await refreshAll();
            } catch (refreshError) {
                showToast(`異常已建立，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
            }
        } catch (error) {
            setAlarmResult(`異常上報失敗\n${normalizeErrorMessage(error)}`);
            showToast("異常上報失敗", "error");
        } finally {
            setIsSubmittingAlarm(false);
        }
    }

    async function handleAssignSubmit(event) {
        event.preventDefault();
        setIsAssigningTicket(true);

        try {
            const result = await apiFetch(`/api/tickets/${encodeURIComponent(ticketForm.ticketNo)}/assign`, {
                method: "PUT",
                body: JSON.stringify({ assignee: ticketForm.assignee })
            });

            setLastTicketNo(ticketForm.ticketNo);
            setTicketResult(stringifyResult("工單指派成功", result));
            showToast(`工單 ${ticketForm.ticketNo} 已指派給 ${ticketForm.assignee}`);

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
        const targetTicketNo = ticketForm.ticketNo || lastTicketNo;
        if (!targetTicketNo) {
            showToast("請先輸入或建立工單編號", "error");
            return;
        }

        setIsUpdatingStatus(true);

        try {
            const result = await apiFetch(`/api/tickets/${encodeURIComponent(targetTicketNo)}/status`, {
                method: "PUT",
                body: JSON.stringify({ status })
            });

            setLastTicketNo(targetTicketNo);
            setTicketResult(stringifyResult(`工單狀態已更新為 ${status}`, result));
            showToast(`工單 ${targetTicketNo} -> ${status}`);

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

    const activeTicketNo = ticketForm.ticketNo || lastTicketNo;
    const currentTicket = tickets.find((ticket) => ticket.ticketNo === activeTicketNo);
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
                        <p className="eyebrow">Smart Maintenance Ticket System</p>
                        <h1>設備異常到維修工單的完整 Demo 前端</h1>
                        <p className="hero-text">
                            這個頁面直接串接 Spring Boot API，展示設備狀態、異常上報、工單處理與 Dashboard 統計。
                        </p>
                        <div className="hero-actions">
                            <button className="primary-btn" disabled={isRefreshing} type="button" onClick={handleRefreshClick}>
                                {isRefreshing ? "整理中..." : "重新整理全系統"}
                            </button>
                            <a className="ghost-link" href="/swagger-ui/index.html" rel="noreferrer" target="_blank">開啟 Swagger</a>
                        </div>
                    </div>
                    <SummaryCards lastRefreshText={lastRefreshText} summary={summary} />
                </header>
                <main className="content-grid">
                    <AlarmForm equipments={equipments} form={alarmForm} isSubmitting={isSubmittingAlarm} result={alarmResult} onChange={handleAlarmFormChange} onSubmit={handleAlarmSubmit} />
                    <TicketControl allowedStatuses={allowedStatuses} form={ticketForm} isAssigning={isAssigningTicket} isUpdatingStatus={isUpdatingStatus} result={ticketResult} onAssign={handleAssignSubmit} onChange={handleTicketFormChange} onStatusUpdate={handleStatusUpdate} />
                    <EquipmentTable equipments={equipments} />
                    <TicketTable tickets={tickets} />
                    <AlarmTimeline alarms={alarms} />
                </main>
            </div>
            <Toast toast={toast} />
        </>
    );
}