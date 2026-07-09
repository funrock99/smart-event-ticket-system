import {
    buildSummaryHandler,
    buildThresholds,
    burstRateLimit,
    duplicateSuppression,
    durationEnv,
    numberEnv,
    replayConsistency
} from "./shared.js";

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
            startRate: numberEnv("BURST_START_RATE", 20),
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
        }
    },
    thresholds: buildThresholds({
        "http_req_duration{route:replay}": ["p(95)<300"],
        "http_req_duration{route:duplicate}": ["p(95)<300"],
        "http_req_duration{route:burst}": ["p(95)<500"]
    })
};

export { replayConsistency, duplicateSuppression, burstRateLimit };
export const handleSummary = buildSummaryHandler("02-redis-fast-path");
