import http from "k6/http";
import { check, sleep } from "k6";

// Caminho de leitura: preco (com cache Caffeine atras) e health. /ledger/verify fica de fora
// de proposito - e rate-limited (60/min) porque recomputa a cadeia inteira, entao sob carga so
// mediria 429, nao throughput. Rode com a app de pe:
//   docker compose up -d
//   k6 run k6/load-test.js
// BASE_URL sobrescreve o alvo: k6 run -e BASE_URL=http://host:8080 k6/load-test.js

const BASE = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  stages: [
    { duration: "30s", target: 50 },
    { duration: "1m", target: 50 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200", "p(99)<500"],
  },
};

export default function () {
  const responses = http.batch([
    ["GET", `${BASE}/api/prices/bitcoin`],
    ["GET", `${BASE}/actuator/health`],
  ]);
  responses.forEach((r) => check(r, { "status 200": (x) => x.status === 200 }));
  sleep(1);
}
