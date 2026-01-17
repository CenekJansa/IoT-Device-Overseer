---
marp: true
footer: Autumn 2025
theme: default
---

# PV217 Service Oriented Architecture
## QuarkIoT
###### Authors: Kristián Oravec, Čeněk Jansa, Adam Mikulášek

---
<!-- paginate: true -->

# Intro
 The goal of this project is to design and implement a distributed microservices-based platform using Quarkus for collecting, storing, and analyzing data from IoT devices and sensors.
The system should be able to receive real-time data streams (e.g., temperature, humidity, light intensity, motion, etc.) from a set of IoT devices, process and persist the data efficiently to keep historical data, and expose REST APIs for data access, analytics, and visualization.

---

# Tech Stack
Programming language: Java 21
Framework: Quarkus 3.29.1
Message broker: Kafka 4.1.1
Database: PostgreSQL 16
Cache: Redis 7
Monitoring: Prometheus 3.0.0, Grafana 9.5.7
Containerization: Docker 28.5.0, Docker Compose 2.40.3
Other: locust 2.42.6, adminer, Kafka UI

---

# Device Management Service
 - Registers and manages IoT devices and their rules.

---

# Data Ingestion Service
 - Collects incoming sensor data
 - Validates against device management service if sensor is registered
 - Sends data to be processed by different service.

---

# Data Processing Service
 - Subscribes to event streams of incoming data.
 - Enriches the data with metadata from device management service.
 - Evaluates whether measurements are within healthy range
 - Sends result data to analytics service.

---

# Analytics Service
 - Subscribes to processed data event stream. 
 - Persists the data.
 - Analytics, and visualization

---

# Live demo
