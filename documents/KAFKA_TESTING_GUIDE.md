# Kafka Testing Guide

This guide provides step-by-step instructions for testing the Kafka-based data flow in the QuarkIoT platform.

---

## Prerequisites

- All services running via `docker compose up`
- Kafka broker accessible at `localhost:9092` (from host) or `broker:9092` (from containers)
- Ingestion service running on port `8082`

---

## 1. REST API Request to Ingestion

Send sensor data to the ingestion service:

```bash
curl -X POST http://localhost:8082/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d",
    "timestamp": "2025-11-30T10:00:00Z",
    "readings": [
      {
        "temperature": 25.5,
        "humidity": 60.0,
        "pressure": 1013.25
      }
    ]
  }'
```

**Expected response:** `ok`

**Note:** Make sure to use a valid `deviceId` that exists in the device-management-service database.

---

## 2. Listen to Topic: `sensor-ingest`

Monitor incoming sensor data from the ingestion service:

```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic sensor-ingest \
  --from-beginning
```

**Expected output format:**
```json
{"deviceId":"550e8400-e29b-41d4-a716-446655440000","timestamp":"2025-11-30T10:00:00Z","readings":[{"temperature":25.5,"humidity":60.0,"pressure":1013.25}]}
```

**Press `Ctrl+C` to stop listening.**

---

## 3. Listen to Topic: `metadata-batch-requests`

Monitor metadata batch requests sent from the processing service to the device-management service:

```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic metadata-batch-requests \
  --from-beginning
```

**Expected output format:**
```json
{"batchId":"uuid-1234-5678-90ab-cdef","deviceIds":["550e8400-e29b-41d4-a716-446655440000","660e8400-e29b-41d4-a716-446655440001"]}
```

**What this shows:**
- The processing service batches incoming sensor data
- It requests metadata for all unique device IDs in the batch
- Each request has a unique `batchId` for correlation

**Press `Ctrl+C` to stop listening.**

---

## 4. Listen to Topic: `metadata-batch-responses`

Monitor metadata batch responses sent from the device-management service back to the processing service:

```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic metadata-batch-responses \
  --from-beginning
```

**Expected output format:**
```json
{
  "batchId":"uuid-1234-5678-90ab-cdef",
  "metadata":{
    "550e8400-e29b-41d4-a716-446655440000":{
      "location":{"latitude":49.1951,"longitude":16.6068},
      "deviceName":"Sensor-A1",
      "deviceType":"TemperatureSensor",
      "deviceStatus":"active",
      "rules":[
        {"rule_name":"temperature","from":15.0,"to":30.0},
        {"rule_name":"humidity","from":40.0,"to":80.0}
      ]
    }
  }
}
```

**What this shows:**
- Device metadata including location, name, type, and status
- Safety rules defining acceptable value ranges for each metric
- The same `batchId` used to correlate with the request

**Press `Ctrl+C` to stop listening.**

---

## 5. Listen to Topic: `processedData`

Monitor processed and enriched sensor data with safety analysis:

```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic processedData \
  --from-beginning
```

**Expected output format:**
```json
{
  "deviceId":"550e8400-e29b-41d4-a716-446655440000",
  "timestamp":1701342000000,
  "metrics":[
    {"metricName":"temperature","value":25.5,"isViolatingSafety":false},
    {"metricName":"humidity","value":60.0,"isViolatingSafety":false},
    {"metricName":"pressure","value":1013.25,"isViolatingSafety":false}
  ]
}
```

**What this shows:**
- Processed sensor data with enriched metadata
- Each metric includes a `isViolatingSafety` flag
- `isViolatingSafety: true` means the value is outside the safe range defined in device rules
- `isViolatingSafety: false` means the value is within acceptable limits

**Press `Ctrl+C` to stop listening.**

---

## Complete End-to-End Test Flow

Follow these steps to test the entire data pipeline:

### Step 1: Start Listening to All Topics

Open **5 separate terminal windows** and run these commands:

**Terminal 1 - sensor-ingest:**
```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic sensor-ingest \
  --from-beginning
```

**Terminal 2 - metadata-batch-requests:**
```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic metadata-batch-requests \
  --from-beginning
```

**Terminal 3 - metadata-batch-responses:**
```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic metadata-batch-responses \
  --from-beginning
```

**Terminal 4 - processedData:**
```bash
docker exec -it broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic processedData \
  --from-beginning
```

### Step 2: Create a Device

**Terminal 5 - Send requests:**
```bash
curl -X POST http://localhost:8081/devices \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Temperature Sensor A1",
    "type": "TemperatureSensor",
    "status": "ACTIVE",
    "location": "Building A, Floor 1",
    "description": "Main temperature sensor"
  }'
```

**Save the returned device `id` (UUID).**

### Step 3: Send Sensor Data

Replace `<DEVICE_ID>` with the UUID from Step 2:

```bash
curl -X POST http://localhost:8082/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "<DEVICE_ID>",
    "timestamp": "2025-11-30T10:00:00Z",
    "readings": [
      {
        "temperature": 25.5,
        "humidity": 60.0
      }
    ]
  }'
```

### Step 4: Observe the Flow

Watch the messages flow through all terminals:

1. **Terminal 1** shows the raw sensor data arriving at `sensor-ingest`
2. **Terminal 2** shows the processing service requesting device metadata
3. **Terminal 3** shows the device-management service responding with metadata
4. **Terminal 4** shows the final processed data with safety analysis

---

## Additional Useful Commands

### List All Kafka Topics

```bash
docker exec -it broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

**Expected output:**
```
metadata-batch-requests
metadata-batch-responses
processedData
sensor-ingest
```

### Describe a Specific Topic

```bash
docker exec -it broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic sensor-ingest
```

**Shows:** Partition count, replication factor, and configuration.

### Check Consumer Group Lag

```bash
docker exec -it broker /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --all-groups
```

**Shows:** How many messages each consumer group is behind.

### Delete a Topic (Caution!)

```bash
docker exec -it broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic sensor-ingest
```

**Warning:** This will delete all messages in the topic!

### Create a Topic Manually

```bash
docker exec -it broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic my-new-topic \
  --partitions 3 \
  --replication-factor 1
```

---

## Troubleshooting

### No Messages Appearing

1. **Check if services are running:**
   ```bash
   docker compose ps
   ```

2. **Check service logs:**
   ```bash
   docker compose logs ingestion-service
   docker compose logs processing-service
   ```

3. **Verify Kafka broker is healthy:**
   ```bash
   docker compose logs broker
   ```

### Consumer Not Receiving Messages

- Use `--from-beginning` flag to read all messages from the start
- Check if the topic exists: `docker exec -it broker /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list`

### Invalid JSON Format Error

Make sure your curl request uses the correct format:
- Field name is `deviceId` (not `sensorId`)
- Field name is `readings` (not `measurements`)
- `readings` is an **array** of objects, not a single object

**Correct:**
```json
{
  "deviceId": "...",
  "timestamp": "...",
  "readings": [{"temperature": 25.5}]
}
```

**Incorrect:**
```json
{
  "sensorId": "...",
  "timestamp": "...",
  "measurements": {"temperature": 25.5}
}
```

---

## Data Format Reference

For detailed information about all Kafka message formats, see:
- [processing-service/DATA_FORMATS.md](DATA_FORMATS.md)

---

**Happy testing! 🚀**

