export const summaryDefinitions = [
    ["totalEquipments", "設備總數"],
    ["runningEquipments", "運轉中"],
    ["downEquipments", "異常停機"],
    ["maintenanceEquipments", "維修中"],
    ["openTickets", "未結工單"],
    ["inProgressTickets", "處理中工單"],
    ["highSeverityAlarms", "高嚴重度異常"]
];

export function normalizeErrorMessage(error) {
    if (!error || !error.message) {
        return "Unknown error";
    }

    try {
        const parsed = JSON.parse(error.message);
        if (parsed.message) {
            return parsed.message;
        }
    } catch {
        // Keep the original message when the payload is not JSON.
    }

    return error.message;
}

export function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString("zh-TW", { hour12: false });
}

export function stringifyResult(title, payload) {
    return `${title}\n${JSON.stringify(payload, null, 2)}`;
}

export function getAllowedNextStatuses(currentStatus) {
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

export function createInitialSummary() {
    return Object.fromEntries(summaryDefinitions.map(([key]) => [key, 0]));
}