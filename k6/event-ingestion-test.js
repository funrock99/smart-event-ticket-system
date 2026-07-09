import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        event_ingestion: {
            executor: "constant-vus",
            vus: 20,
            duration: "30s"
        }
    },
    thresholds: {
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<1000"]
    }
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
http.setResponseCallback(http.expectedStatuses(200, 201, 409, 429));

export default function () {
    const route = __ITER % 3;
    let businessKey;
    let idempotencyKey;
    let source = "payment-system";

    if (route === 0) {
        businessKey = `REPLAY-${__VU}`;
        idempotencyKey = `idem-replay-${__VU}`;
    } else if (route === 1) {
        businessKey = `DUPLICATE-${__VU}`;
        idempotencyKey = `idem-duplicate-${__VU}-${__ITER}`;
    } else {
        businessKey = `RATE-${__VU}-${__ITER}`;
        idempotencyKey = `idem-rate-${__VU}-${__ITER}`;
        source = "burst-source";
    }

    const payload = JSON.stringify({
        source,
        eventType: "TRANSACTION_ERROR",
        businessKey,
        severity: "HIGH",
        message: "Transaction failed during k6 validation",
        payload: JSON.stringify({ businessKey, route })
    });

    const response = http.post(`${BASE_URL}/api/events`, payload, {
        headers: {
            "Content-Type": "application/json",
            "Idempotency-Key": idempotencyKey
        }
    });

    check(response, {
        "status is expected": (res) => [200, 201, 409, 429].includes(res.status)
    });

    sleep(1);
}
