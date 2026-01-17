# Monitoring Guide

## Quick Access

| Tool | URL | Credentials |
|------|-----|-------------|
| **Prometheus** | http://localhost:9090 | None required |
| **Grafana** | http://localhost:3000 | Username: `admin`<br>Password: `admin` |

---

## Viewing Metrics in Prometheus

1. Open http://localhost:9090
2. Click **Status** → **Targets** to verify services are **UP**
3. Use the search bar to query metrics:
   - `http_server_requests_seconds_count` - HTTP requests
   - `jvm_memory_used_bytes` - JVM memory
   - `kafka_consumer_fetch_manager_records_consumed_total` - Kafka messages
4. Click **Graph** tab to visualize data

---

## Creating Dashboard in Grafana

1. Navigate to http://localhost:3000 and login
2. Click **☰** → **Connections** → **Data sources** → **Add data source**
3. Select **Prometheus** and set URL to `http://prometheus:9090`
4. Click **Save & Test**
5. Click **☰** → **Dashboards** → **New** → **Import**
6. Click **Upload JSON file** and select `/grafana-dashboard-export/processing_service_metadata_topic_monitoring.json`
7. Select **Prometheus** as data source and click **Import**

