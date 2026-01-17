import random
import uuid
from datetime import datetime, timezone
from locust import HttpUser, task, between

DEVICES = [
    {
        "id": "1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d",
        "reading_name": "temperature",
        "value_func": lambda: round(random.uniform(10.0, 35.0), 2)
    },
    {
        "id": "b2c3d4e5-f6e5-4d3c-2b1a-0fedcba98765",
        "reading_name": "brightness",
        "value_func": lambda: round(random.uniform(0.0, 200.0), 2)
    },
    {
        "id": "c3d4e5f6-a1b2-3c4d-5e6f-789012345678",
        "reading_name": "motion",
        "value_func": lambda: random.choice([0, 1])
    },
    {
        "id": "a7b8c9d0-e1f2-3a4b-5c6d-7e8f01234567",
        "reading_name": "low_power_output",
        "value_func": lambda: random.choice([500, 99999])
    }
]


class SensorIngestUser(HttpUser):
    wait_time = between(0.2, 1.0)

    @task(5)  # weight so this runs more often
    def ingest_known_device(self):
        device = random.choice(DEVICES)
        timestamp = datetime.now(timezone.utc).isoformat()

        payload = {
            "deviceId": device["id"],
            "timestamp": timestamp,
            "readings": [
                {device["reading_name"]: device["value_func"]()}
            ]
        }

        headers = {
            "accept": "text/plain",
            "Content-Type": "application/json"
        }

        self.client.post("/ingest", json=payload, headers=headers)

    @task(1)  # unknown device runs less often by default
    def ingest_unknown_device(self):
        timestamp = datetime.now(timezone.utc).isoformat()

        unknown_device_id = str(uuid.uuid4())  # generates unknown device
        noise_value = round(random.uniform(0.0, 120.0), 2)

        payload = {
            "deviceId": unknown_device_id,
            "timestamp": timestamp,
            "readings": [
                {"noise": noise_value}
            ]
        }

        headers = {
            "accept": "text/plain",
            "Content-Type": "application/json"
        }

        self.client.post("/ingest", json=payload, headers=headers)
