import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
http.setResponseCallback(http.expectedStatuses(200, 201, 409, 429));

export const duplicateResponses = new Counter("duplicate_responses");
export const replayResponses = new Counter("replay_responses");
export const rateLimitedResponses = new Counter("rate_limited_responses");
export const acceptedResponses = new Counter("accepted_responses");
export const scenarioChecks = new Rate("scenario_checks");

export function numberEnv(name, fallback) {
    const value = __ENV[name];
    if (!value) {
        return fallback;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
}

export function durationEnv(name, fallback) {
    return __ENV[name] || fallback;
}

export function buildThresholds(routeThresholds) {
    return {
        http_req_failed: ["rate<0.02"],
        http_req_duration: ["p(95)<800", "p(99)<1500"],
        dropped_iterations: ["count<1"],
        scenario_checks: ["rate>0.99"],
        ...routeThresholds
    };
}

export function postEvent({ source, businessKey, idempotencyKey, route, severity = "HIGH" }) {
    const payload = JSON.stringify({
        source,
        eventType: "TRANSACTION_ERROR",
        businessKey,
        severity,
        message: `Transaction failed during ${route} validation`,
        payload: JSON.stringify({ businessKey, route })
    });

    const response = http.post(`${BASE_URL}/api/events`, payload, {
        headers: {
            "Content-Type": "application/json",
            "Idempotency-Key": idempotencyKey
        },
        tags: { route }
    });

    recordResponse(response);
    const ok = check(response, {
        "status is expected": (res) => [200, 201, 409, 429].includes(res.status)
    });
    scenarioChecks.add(ok);
    return response;
}

export function recordResponse(response) {
    if (response.status === 429) {
        rateLimitedResponses.add(1);
        return;
    }

    let body = null;
    try {
        body = response.json();
    } catch (error) {
        body = null;
    }

    if (response.status === 201) {
        acceptedResponses.add(1);
        return;
    }

    if (body && body.duplicated === true) {
        duplicateResponses.add(1);
        return;
    }

    if (response.status === 200) {
        replayResponses.add(1);
    }
}

export function replayConsistency() {
    postEvent({
        source: "payment-system",
        businessKey: `REPLAY-${__VU}`,
        idempotencyKey: `idem-replay-${__VU}`,
        route: "replay"
    });
    sleep(0.2);
}

export function duplicateSuppression() {
    postEvent({
        source: "duplicate-source",
        businessKey: `DUPLICATE-${__ITER % 20}`,
        idempotencyKey: `idem-duplicate-${__VU}-${__ITER}`,
        route: "duplicate"
    });
}

export function burstRateLimit() {
    postEvent({
        source: "burst-source",
        businessKey: `BURST-${__VU}-${__ITER}`,
        idempotencyKey: `idem-burst-${__VU}-${__ITER}`,
        route: "burst"
    });
}

export function acceptedOnly() {
    const acceptedSourceShards = numberEnv("ACCEPTED_SOURCE_SHARDS", 20);
    postEvent({
        source: `checkout-system-${(__VU + __ITER) % acceptedSourceShards}`, 
        businessKey: `ACCEPT-${__VU}-${__ITER}`,
        idempotencyKey: `idem-accept-${__VU}-${__ITER}`,
        route: "accepted"
    });
}

export function mixedHighThroughput() {
    const mode = __ITER % 4;
    if (mode === 0) {
        replayConsistency();
        return;
    }
    if (mode === 1) {
        duplicateSuppression();
        return;
    }
    if (mode === 2) {
        burstRateLimit();
        return;
    }

    acceptedOnly();
}

function metricValue(data, metricName, stat) {
    const metric = data.metrics[metricName];
    if (!metric || !metric.values) {
        return "n/a";
    }
    const value = metric.values[stat];
    if (value === undefined || value === null) {
        return "n/a";
    }
    return typeof value === "number" ? value.toFixed(2) : String(value);
}

function countValue(data, metricName) {
    const metric = data.metrics[metricName];
    if (!metric || !metric.values || metric.values.count === undefined) {
        return "0";
    }
    return String(metric.values.count);
}

function rateValue(data, metricName) {
    const metric = data.metrics[metricName];
    if (!metric || !metric.values || metric.values.rate === undefined) {
        return "n/a";
    }
    return `${(metric.values.rate * 100).toFixed(2)}%`;
}

function formatSummaryText(suiteName, data) {
    return [
        `suite: ${suiteName}`,
        `requests: ${countValue(data, "http_reqs")}`,
        `throughput_per_sec: ${metricValue(data, "http_reqs", "rate")}`,
        `http_req_failed: ${rateValue(data, "http_req_failed")}`,
        `p95_ms: ${metricValue(data, "http_req_duration", "p(95)")}`,
        `p99_ms: ${metricValue(data, "http_req_duration", "p(99)")}`,
        `dropped_iterations: ${countValue(data, "dropped_iterations")}`,
        `accepted_responses: ${countValue(data, "accepted_responses")}`,
        `duplicate_responses: ${countValue(data, "duplicate_responses")}`,
        `replay_responses: ${countValue(data, "replay_responses")}`,
        `rate_limited_responses: ${countValue(data, "rate_limited_responses")}`,
        `scenario_checks: ${rateValue(data, "scenario_checks")}`
    ].join("\n");
}

export function buildSummaryHandler(suiteName) {
    return function handleSummary(data) {
        const timestamp = new Date().toISOString().replace(/[.:]/g, "-");
        const basePath = `results/${suiteName}-${timestamp}`;
        const text = formatSummaryText(suiteName, data);
        return {
            stdout: `${text}\n`,
            [`${basePath}.txt`]: `${text}\n`,
            [`${basePath}.json`]: JSON.stringify(data, null, 2)
        };
    };
}

