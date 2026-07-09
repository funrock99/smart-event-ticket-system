import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
http.setResponseCallback(http.expectedStatuses(200, 201, 409, 429));

const duplicateResponses = new Counter("duplicate_responses");
const replayResponses = new Counter("replay_responses");
const rateLimitedResponses = new Counter("rate_limited_responses");
const acceptedResponses = new Counter("accepted_responses");
const scenarioChecks = new Rate("scenario_checks");

function numberEnv(name, fallback) {
    const value = __ENV[name];
    if (!value) {
        return fallback;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function durationEnv(name, fallback) {
    return __ENV[name] || fallback;
}

export const options = {
    scenarios: {
        replay_consistency: {
            executor: "constant-vus",
            exec: "replayConsistency",
            vus: numberEnv("REPLAY_VUS", 40),
            duration: durationEnv("REPLAY_DURATION", "45s"),
            gracefulStop: "10s",
            tags: { scenario_type: "replay" }
        },
        duplicate_suppression: {
            executor: "constant-arrival-rate",
            exec: "duplicateSuppression",
            rate: numberEnv("DUPLICATE_RATE", 80),
            timeUnit: "1s",
            duration: durationEnv("DUPLICATE_DURATION", "45s"),
            preAllocatedVUs: numberEnv("DUPLICATE_PRE_VUS", 80),
            maxVUs: numberEnv("DUPLICATE_MAX_VUS", 240),
            tags: { scenario_type: "duplicate" }
        },
        burst_rate_limit: {
            executor: "ramping-arrival-rate",
            exec: "burstRateLimit",
            startRate: 20,
            timeUnit: "1s",
            preAllocatedVUs: numberEnv("BURST_PRE_VUS", 120),
            maxVUs: numberEnv("BURST_MAX_VUS", 320),
            stages: [
                { target: numberEnv("BURST_STAGE_ONE", 80), duration: durationEnv("BURST_STAGE_ONE_DURATION", "20s") },
                { target: numberEnv("BURST_STAGE_TWO", 160), duration: durationEnv("BURST_STAGE_TWO_DURATION", "20s") },
                { target: numberEnv("BURST_STAGE_THREE", 240), duration: durationEnv("BURST_STAGE_THREE_DURATION", "20s") },
                { target: 0, duration: durationEnv("BURST_RAMP_DOWN_DURATION", "10s") }
            ],
            tags: { scenario_type: "burst" }
        },
        mixed_high_throughput: {
            executor: "constant-arrival-rate",
            exec: "mixedHighThroughput",
            rate: numberEnv("MIXED_RATE", 120),
            timeUnit: "1s",
            duration: durationEnv("MIXED_DURATION", "60s"),
            preAllocatedVUs: numberEnv("MIXED_PRE_VUS", 120),
            maxVUs: numberEnv("MIXED_MAX_VUS", 360),
            tags: { scenario_type: "mixed" }
        }
    },
    thresholds: {
        http_req_failed: ["rate<0.02"],
        http_req_duration: ["p(95)<800", "p(99)<1500"],
        scenario_checks: ["rate>0.99"]
    }
};

function postEvent({ source, businessKey, idempotencyKey, route, severity = "HIGH" }) {
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

function recordResponse(response) {
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

    if (body?.duplicated === true) {
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

    postEvent({
        source: "checkout-system",
        businessKey: `ACCEPT-${__VU}-${__ITER}`,
        idempotencyKey: `idem-accept-${__VU}-${__ITER}`,
        route: "accepted"
    });
}
