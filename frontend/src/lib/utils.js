export const summaryDefinitions = [
    ["totalEvents", "總事件數"],
    ["validEvents", "有效事件"],
    ["duplicatedEvents", "重複事件"],
    ["rateLimitedEvents", "限流事件"],
    ["openTickets", "OPEN 工單"],
    ["processingTickets", "PROCESSING 工單"],
    ["resolvedTickets", "RESOLVED 工單"],
    ["closedTickets", "CLOSED 工單"]
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
            return ["PROCESSING"];
        case "PROCESSING":
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
