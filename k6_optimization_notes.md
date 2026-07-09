# Smart Event Ticket System：k6 測試結果優化建議

> Repo: `funrock99/smart-event-ticket-system`  
> 主題：根據最新一輪 k6 clean retest，整理目前高併發瓶頸、已完成優化，以及下一步仍需要處理的項目。

---

## 1. 最新測試結果摘要

本輪正式採用的 clean retest 結果如下：

| 指標 | 結果 | 判斷 |
|---|---:|---|
| Requests | `1659` | 測試成功完成，但總量低於原先目標 |
| Throughput | `89.66 req/s` | 未穩定打到高併發目標 |
| `http_req_failed` | `2.59%` | 未達 `< 2%` |
| `p95` | `9.49s` | 明顯未達標 |
| `p99` | `11.08s` | 明顯未達標 |
| `dropped_iterations` | `1596` | 測試工具與系統都已接近飽和 |
| `scenario_checks` | `97.40%` | 低於 `> 99%` 目標 |

### 本輪結論

目前系統還不能把這組 mixed high-throughput 壓測結果拿來當作「高併發已達標」的結論。

主要原因不是單一問題，而是三件事同時出現：

- 錯誤率已高於既定門檻。
- tail latency 非常高，代表同步重路徑仍明顯卡住。
- `dropped_iterations` 很高，代表實際送壓沒有完整跟上設定速率。

也就是說，現在的瓶頸已經不是只有 tail latency，而是整體吞吐、VU 飽和、以及重路徑處理成本都需要一起看。

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

## 4. 目前瓶頸判讀

### 4.1 accepted path 還是最重

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

因此在 mixed 測試裡，真正拖慢 p95 / p99 的主因仍然是 accepted path 的同步交易成本。

### 4.2 burst 場景把 VU 撐滿

最新 clean retest 出現：

- `Insufficient VUs`
- `320 active VUs`
- `dropped_iterations=1596`

這代表目前的 burst rate limit 場景不只是驗證 rate limit，也在暴露整體壓測組合的資源上限。

### 4.3 duplicate path 優化是有效但不夠

duplicate path 已從「Redis 命中後還要再查一次 DB」變成「先回傳 cached ticket id」。

這是正確方向，但 mixed 測試仍失敗，表示主要瓶頸沒有被完全轉移掉。換句話說，這輪優化是必要的，但不足以讓整體高併發結果達標。

---

## 5. 下一輪回測建議順序

建議不要直接再把 mixed 壓力拉高，先分開驗證：

1. 先跑 `01-baseline-accepted.js`
2. 再跑 `02-redis-fast-path.js`
3. 最後跑 `03-mixed-production-like.js`

目的很直接：

- 如果 accepted baseline 已經很慢，問題就在同步 DB 路徑
- 如果 Redis fast path 很穩，但 mixed 還是失敗，問題就在 accepted path 佔比與 transaction 壓力
- 如果三支都失敗，就不是單一路徑問題，而是整體資源配置還不夠

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

### 尚未完成

- [ ] mixed high-throughput 達到 `http_req_failed < 2%`
- [ ] mixed high-throughput 達到 `p95 < 800ms`
- [ ] mixed high-throughput 達到 `p99 < 1500ms`
- [ ] `dropped_iterations = 0`
- [ ] 能穩定宣稱已達成高併發需求

---

## 7. 如何解讀目前這份結果

如果要對外說明，建議說法應該是：

```text
系統已完成高併發事件接收入口的保護層設計，包含 Redis rate limiting、idempotency、deduplication 與 dashboard cache；並建立多場景 k6 壓測腳本做 route-level 量測。

在最新一輪 mixed high-throughput retest 中，duplicate path 快取與 loadtest profile 已完成，但整體結果仍顯示 accepted path 的同步交易成本與 burst 壓力下的 VU 飽和仍是主要瓶頸，因此目前屬於「已建立完整壓測與優化方法、但高併發目標尚未完全達標」的狀態。
```

這樣是準確的，不會把結果講過頭。

---

## 8. References

- Grafana k6 Thresholds documentation: https://grafana.com/docs/k6/latest/using-k6/thresholds/
- Grafana k6 Dropped iterations documentation: https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/dropped-iterations/
- Grafana k6 Ramping arrival rate executor documentation: https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/ramping-arrival-rate/
- Project README: https://github.com/funrock99/smart-event-ticket-system

