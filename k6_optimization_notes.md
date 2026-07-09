# Smart Event Ticket System：k6 測試結果優化建議

> Repo: `funrock99/smart-event-ticket-system`  
> 主題：根據最新一輪 k6 clean retest，整理目前高併發表現、已完成優化，以及下一步仍可持續驗證的項目。

---

## 1. 最新測試結果摘要

本輪正式採用的 clean retest 結果如下：

| 指標 | 結果 | 判斷 |
|---|---:|---|
| Requests | `1644` | 測試成功完成 |
| Throughput | `109.59 req/s` | 在本輪 mixed retest 下表現穩定 |
| `http_req_failed` | `0.00%` | 達成 `< 2%` |
| `p95` | `120.84ms` | 明顯優於目標 |
| `p99` | `threshold passed (< 1500ms)` | 結果檔未輸出精確值，但門檻已通過 |
| `dropped_iterations` | `0` | 實際送壓有跟上設定速率 |
| `scenario_checks` | `100.00%` | 達成 `> 99%` 目標 |

### 本輪結論

目前這組 mixed high-throughput clean retest 可以支持「在本機 Docker 與非高規格硬體環境下，系統已具備穩定高併發入口保護能力」的結論。

這輪結果代表先前最明顯的問題已被壓下來：

- 錯誤率已降到 `0.00%`。
- p95 已降到 `120.84ms`，不再出現先前的嚴重 tail latency。
- `dropped_iterations` 已回到 `0`，代表目前腳本與系統都能跟上這輪送壓。

不過這仍然是「縮短版 mixed 流量」與「目前這組壓測速率」下的正式結果，因此比較準確的說法是：這一輪已達標，但若要再往更高 throughput、長時穩定性、或更嚴苛硬體條件推進，仍需要下一輪回測驗證。

---

## 2. 這一輪已完成的優化

### 2.1 後端邏輯優化

已完成並通過 `mvn test` 的調整：

- duplicate path 新增 dedup ticket id 快取，優先直接回傳 cached `ticketId`
- duplicate path 只有 cache miss 才回退查 `findLatestByEvent(...)`
- dashboard summary eviction 改成節流，避免高頻事件下反覆清 cache
- accepted path 建單成功後會把 `ticketId` 回寫到 dedup cache，供後續 duplicate path 快取命中

### 2.2 壓測設定優化

已完成：

- k6 拆成 3 支腳本
- 加入 route-level thresholds
- 加入 `dropped_iterations` threshold
- 加入 `handleSummary()`，每次壓測自動輸出 `.txt` 與 `.json` 到 `k6/results/`
- 保留 `k6/event-ingestion-test.js` 作為相容入口

### 2.3 執行環境優化

已完成：

- 新增 `loadtest` profile
- load test 關閉 SQL log 與 SQL format log
- 調整 HikariCP 連線池到較適合壓測的設定
- `docker-compose.yml` 已改成使用 `docker-postgres,loadtest`

---

## 3. 新的 k6 腳本分工

```text
k6/
├── 01-baseline-accepted.js
├── 02-redis-fast-path.js
├── 03-mixed-production-like.js
├── event-ingestion-test.js
└── shared.js
```

### `01-baseline-accepted.js`

用途：

- 只測 accepted path
- 觀察同步 DB 寫入與 transaction latency

重點門檻：

- `http_req_duration{route:accepted}`
- `dropped_iterations`

### `02-redis-fast-path.js`

用途：

- 測 replay
- 測 duplicate
- 測 burst rate limit
- 驗證 Redis 保護層是否能穩定接住快路徑

重點門檻：

- `http_req_duration{route:replay}`
- `http_req_duration{route:duplicate}`
- `http_req_duration{route:burst}`

### `03-mixed-production-like.js`

用途：

- 模擬 accepted / replay / duplicate / burst 混合流量
- 用來判斷整體高併發穩定性

這支腳本才是目前最接近「能不能對外講高併發能力」的主要依據。

---

## 4. 目前結果判讀

### 4.1 accepted path 仍是最重的同步路徑

即使 duplicate path 已做快取優化，accepted request 仍需要同步完成：

```text
Rate limit
Idempotency
Deduplication
Insert AlarmEvent
Create MaintenanceTicket
Insert TicketStatusHistory
Mark idempotency completed
Evict dashboard summary cache
```

不過從最新 mixed retest 來看，accepted path 雖然仍是架構上最重的同步路徑，但已沒有把整體 p95 明顯拖垮。

### 4.2 burst 場景這一輪沒有再把 VU 撐滿

先前較差那輪 clean retest 曾出現：

- `Insufficient VUs`
- `320 active VUs`
- `dropped_iterations=1596`

但最新正式 mixed retest 已經回到：

- `dropped_iterations=0`
- `http_req_failed=0.00%`
- `scenario_checks=100.00%`

這代表目前的 burst rate limit 場景在這輪設定下已不再暴露出明顯的資源飽和問題。

### 4.3 duplicate path 優化是有效的

duplicate path 已從「Redis 命中後還要再查一次 DB」變成「先回傳 cached ticket id」。

這是正確方向，而且從最新 mixed retest 已通過全域門檻來看，這輪優化確實對整體穩定性有幫助。

---

## 5. 下一輪回測建議順序

如果要繼續往上推吞吐或拉長測試時間，建議仍先分開驗證：

1. 先跑 `01-baseline-accepted.js`
2. 再跑 `02-redis-fast-path.js`
3. 最後跑 `03-mixed-production-like.js`

目的很直接：

- 如果 accepted baseline 開始變慢，問題會回到同步 DB 路徑
- 如果 Redis fast path 很穩，但更高壓 mixed 開始退化，問題多半在 accepted path 佔比與 transaction 壓力
- 如果三支都退化，就不是單一路徑問題，而是整體資源配置還不夠

---

## 6. 目前完成度判斷

### 已完成

- [x] k6 拆三支腳本
- [x] route-level thresholds
- [x] 壓測結果落檔
- [x] duplicate path ticket id 快取
- [x] dashboard eviction 節流
- [x] loadtest profile
- [x] HikariCP / SQL logging 第一輪優化
- [x] clean retest 一輪
- [x] mixed high-throughput 達到 `http_req_failed < 2%`
- [x] mixed high-throughput 達到 `p95 < 800ms`
- [x] mixed high-throughput 通過 `p99 < 1500ms` threshold
- [x] `dropped_iterations = 0`

### 尚未完成

- [ ] 更長時間 mixed 壓測驗證
- [ ] 更高 throughput 設定下的 mixed 壓測驗證
- [ ] 更高規格或更接近正式環境拓樸下的回測
- [ ] 補上可直接引用精確 `p99` 的 summary 輸出

---

## 7. 如何解讀目前這份結果

如果要對外說明，建議說法應該是：

```text
系統已完成高併發事件接收入口的保護層設計，包含 Redis rate limiting、idempotency、deduplication 與 dashboard cache；並建立多場景 k6 壓測腳本做 route-level 量測。

在最新一輪正式 mixed high-throughput retest 中，本機 Docker 環境下以約 `109.59 req/s` 完成 `1644` requests，並達成 `http_req_failed=0.00%`、`p95=120.84ms`、`dropped_iterations=0`、`scenario_checks=100.00%`。這表示目前系統已具備穩定的高併發入口保護能力；若要再往更高吞吐或更長時穩定性宣稱，則需補做更高壓回測。
```

這樣的表述比較準確，也能和目前 repo 內正式結果摘要保持一致。

---

## 8. References

- Grafana k6 Thresholds documentation: https://grafana.com/docs/k6/latest/using-k6/thresholds/
- Grafana k6 Dropped iterations documentation: https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/dropped-iterations/
- Grafana k6 Ramping arrival rate executor documentation: https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/ramping-arrival-rate/
- Project README: https://github.com/funrock99/smart-event-ticket-system
