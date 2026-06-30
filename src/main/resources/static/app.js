const state = {
    lastTicketNo: ""
};

const summaryCards = document.getElementById("summaryCards");
const equipmentTableBody = document.getElementById("equipmentTableBody");
const ticketTableBody = document.getElementById("ticketTableBody");
const alarmList = document.getElementById("alarmList");
const alarmResult = document.getElementById("alarmResult");
const ticketResult = document.getElementById("ticketResult");
const equipmentCountPill = document.getElementById("equipmentCountPill");
const ticketCountPill = document.getElementById("ticketCountPill");
const alarmCountPill = document.getElementById("alarmCountPill");
const lastRefreshText = document.getElementById("lastRefreshText");
const toast = document.getElementById("toast");
const refreshAllBtn = document.getElementById("refreshAllBtn");
const alarmForm = document.getElementById("alarmForm");
const assignForm = document.getElementById("assignForm");
const equipmentSelect = document.getElementById("equipmentSelect");
const statusButtons = Array.from(document.querySelectorAll(".status-btn"));

const summaryDefinitions = [
    ["totalEquipments", "設備總數"],
    ["runningEquipments", "運轉中"],
    ["downEquipments", "異常停機"],
    ["maintenanceEquipments", "維修中"],
    ["openTickets", "未結工單"],
    ["inProgressTickets", "處理中工單"],
    ["highSeverityAlarms", "高嚴重度異常"]
];

async function apiFetch(path, options = {}) {
    const response = await fetch(path, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `${response.status} ${response.statusText}`);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function normalizeErrorMessage(error) {
    if (!error || !error.message) {
        return "Unknown error";
    }

    try {
        const parsed = JSON.parse(error.message);
        if (parsed.message) {
            return parsed.message;
        }
    } catch {
        // Keep original message when the payload is not JSON.
    }

    return error.message;
}

function renderSummary(summary) {
    summaryCards.innerHTML = summaryDefinitions.map(([key, label]) => `
        <article class="summary-card">
            <span class="summary-label">${label}</span>
            <span class="summary-value">${summary[key] ?? 0}</span>
        </article>
    `).join("");
}

function renderEquipments(equipments) {
    equipmentCountPill.textContent = `${equipments.length} 台設備`;
    equipmentTableBody.innerHTML = equipments.map((equipment) => `
        <tr>
            <td>${escapeHtml(equipment.equipmentId)}</td>
            <td>${escapeHtml(equipment.name)}</td>
            <td>${escapeHtml(equipment.factoryArea)}</td>
            <td><span class="badge ${equipment.status}">${equipment.status}</span></td>
        </tr>
    `).join("");

    const currentValue = equipmentSelect.value;
    equipmentSelect.innerHTML = [
        `<option value="">請選擇設備</option>`,
        ...equipments.map((equipment) => `
            <option value="${escapeHtml(equipment.equipmentId)}">
                ${escapeHtml(equipment.equipmentId)} | ${escapeHtml(equipment.name)}
            </option>
        `)
    ].join("");

    if (equipments.some((equipment) => equipment.equipmentId === currentValue)) {
        equipmentSelect.value = currentValue;
    } else if (!equipmentSelect.value && equipments.length > 0) {
        equipmentSelect.value = equipments[0].equipmentId;
    }
}

function renderTickets(tickets) {
    ticketCountPill.textContent = `${tickets.length} 筆工單`;
    ticketTableBody.innerHTML = tickets.map((ticket) => `
        <tr>
            <td>${escapeHtml(ticket.ticketNo)}</td>
            <td>${escapeHtml(ticket.equipmentId)}</td>
            <td>${escapeHtml(ticket.alarmCode)}</td>
            <td>${escapeHtml(ticket.priority)}</td>
            <td><span class="badge ${ticket.status}">${ticket.status}</span></td>
            <td>${escapeHtml(ticket.assignee || "-")}</td>
        </tr>
    `).join("");

    syncStatusButtons(tickets);
}

function renderAlarms(alarms) {
    alarmCountPill.textContent = `${alarms.length} 筆事件`;
    alarmList.innerHTML = alarms.map((alarm) => `
        <article class="alarm-item">
            <div class="alarm-topline">
                <strong>${escapeHtml(alarm.equipmentId)} / ${escapeHtml(alarm.alarmCode)}</strong>
                <span class="badge ${alarm.severity === "CRITICAL" || alarm.severity === "HIGH" ? "DOWN" : "MAINTENANCE"}">${escapeHtml(alarm.severity)}</span>
            </div>
            <div>${escapeHtml(alarm.message)}</div>
            <div class="alarm-meta">發生時間：${formatDateTime(alarm.occurredAt)}</div>
        </article>
    `).join("");
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString("zh-TW", {
        hour12: false
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function showToast(message, type = "") {
    toast.textContent = message;
    toast.className = `toast ${type}`.trim();
    setTimeout(() => {
        toast.className = "toast hidden";
    }, 3200);
}

function setResultBox(element, title, payload) {
    element.textContent = `${title}\n${JSON.stringify(payload, null, 2)}`;
}

function getAllowedNextStatuses(currentStatus) {
    switch (currentStatus) {
        case "OPEN":
            return ["IN_PROGRESS"];
        case "IN_PROGRESS":
            return ["RESOLVED"];
        case "RESOLVED":
            return ["CLOSED"];
        default:
            return [];
    }
}

function syncStatusButtons(tickets = []) {
    const ticketNo = assignForm.elements.ticketNo.value || state.lastTicketNo;
    const currentTicket = tickets.find((ticket) => ticket.ticketNo === ticketNo);
    const allowedStatuses = currentTicket ? getAllowedNextStatuses(currentTicket.status) : [];

    statusButtons.forEach((button) => {
        button.disabled = !allowedStatuses.includes(button.dataset.status);
    });
}

async function refreshAll() {
    const [summary, equipments, tickets, alarms] = await Promise.all([
        apiFetch("/api/dashboard/summary"),
        apiFetch("/api/equipments"),
        apiFetch("/api/tickets"),
        apiFetch("/api/alarms")
    ]);

    renderSummary(summary);
    renderEquipments(equipments);
    renderTickets(Array.isArray(tickets) ? tickets : [tickets]);
    renderAlarms(Array.isArray(alarms) ? alarms : [alarms]);
    lastRefreshText.textContent = new Date().toLocaleTimeString("zh-TW", { hour12: false });
}

alarmForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(alarmForm);
    const payload = Object.fromEntries(formData.entries());

    try {
        const result = await apiFetch("/api/alarms", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        state.lastTicketNo = result.ticketNo;
        assignForm.elements.ticketNo.value = result.ticketNo;
        syncStatusButtons([]);
        setResultBox(alarmResult, "異常上報成功", result);
        showToast(`已建立工單 ${result.ticketNo}`);

        try {
            await refreshAll();
        } catch (refreshError) {
            showToast(`異常已建立，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
        }
    } catch (error) {
        setResultBox(alarmResult, "異常上報失敗", normalizeErrorMessage(error));
        showToast("異常上報失敗", "error");
    }
});

assignForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(assignForm);
    const payload = Object.fromEntries(formData.entries());

    try {
        const result = await apiFetch(`/api/tickets/${encodeURIComponent(payload.ticketNo)}/assign`, {
            method: "PUT",
            body: JSON.stringify({ assignee: payload.assignee })
        });
        state.lastTicketNo = payload.ticketNo;
        syncStatusButtons([]);
        setResultBox(ticketResult, "工單指派成功", result);
        showToast(`工單 ${payload.ticketNo} 已指派給 ${payload.assignee}`);

        try {
            await refreshAll();
        } catch (refreshError) {
            showToast(`指派已成功，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
        }
    } catch (error) {
        setResultBox(ticketResult, "工單指派失敗", normalizeErrorMessage(error));
        showToast("工單指派失敗", "error");
    }
});

document.querySelectorAll(".status-btn").forEach((button) => {
    button.addEventListener("click", async () => {
        const ticketNo = assignForm.elements.ticketNo.value || state.lastTicketNo;
        if (!ticketNo) {
            showToast("請先輸入或建立工單編號", "error");
            return;
        }

        try {
            const result = await apiFetch(`/api/tickets/${encodeURIComponent(ticketNo)}/status`, {
                method: "PUT",
                body: JSON.stringify({ status: button.dataset.status })
            });
            state.lastTicketNo = ticketNo;
            setResultBox(ticketResult, `工單狀態已更新為 ${button.dataset.status}`, result);
            showToast(`工單 ${ticketNo} -> ${button.dataset.status}`);

            try {
                await refreshAll();
            } catch (refreshError) {
                showToast(`狀態已更新，但畫面刷新失敗：${normalizeErrorMessage(refreshError)}`, "error");
            }
        } catch (error) {
            setResultBox(ticketResult, "工單狀態更新失敗", normalizeErrorMessage(error));
            showToast("工單狀態更新失敗", "error");
        }
    });
});

assignForm.elements.ticketNo.addEventListener("input", async () => {
    try {
        const tickets = await apiFetch("/api/tickets");
        syncStatusButtons(Array.isArray(tickets) ? tickets : [tickets]);
    } catch {
        syncStatusButtons([]);
    }
});

refreshAllBtn.addEventListener("click", async () => {
    try {
        await refreshAll();
        showToast("資料已重新整理");
    } catch (error) {
        showToast("重新整理失敗", "error");
    }
});

refreshAll().catch((error) => {
    showToast("前端初始化失敗", "error");
    setResultBox(alarmResult, "初始化失敗", normalizeErrorMessage(error));
});
