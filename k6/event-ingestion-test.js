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

export default function () {
    const businessKey = `TXN-${__VU}-${__ITER % 5}`;
    const payload = JSON.stringify({
        source: "payment-system",
        eventType: "TRANSACTION_ERROR",
        businessKey,
        severity: "HIGH",
        message: "Transaction failed during k6 validation",
        payload: JSON.stringify({ businessKey })
    });

    const response = http.post(`${BASE_URL}/api/events`, payload, {
        headers: {
            "Content-Type": "application/json",
            "Idempotency-Key": `idem-${__VU}-${__ITER % 3}`
        }
    });

    check(response, {
        "status is expected": (res) => [200, 201, 409, 429].includes(res.status)
    });

    sleep(1);
}
