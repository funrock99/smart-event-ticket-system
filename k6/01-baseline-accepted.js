import { acceptedOnly, buildSummaryHandler, buildThresholds, durationEnv, numberEnv } from "./shared.js";

export const options = {
    scenarios: {
        accepted_baseline: {
            executor: "constant-arrival-rate",
            exec: "acceptedOnly",
            rate: numberEnv("ACCEPTED_RATE", 60),
            timeUnit: "1s",
            duration: durationEnv("ACCEPTED_DURATION", "45s"),
            preAllocatedVUs: numberEnv("ACCEPTED_PRE_VUS", 80),
            maxVUs: numberEnv("ACCEPTED_MAX_VUS", 240),
            tags: { scenario_type: "accepted" }
        }
    },
    thresholds: buildThresholds({
        "http_req_duration{route:accepted}": ["p(95)<1200", "p(99)<2000"]
    })
};

export { acceptedOnly };
export const handleSummary = buildSummaryHandler("01-baseline-accepted");
