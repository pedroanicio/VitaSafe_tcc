# VitaSafe

### Intelligent IoT-Based System for Real-Time Anomaly Detection in Elderly Healthcare

VitaSafe is an intelligent healthcare monitoring system developed as a Final Year Project (TCC) at the Federal Institute of Education, Science and Technology of Minas Gerais (IFMG). The project combines **Internet of Things (IoT), embedded systems, cloud computing, mobile applications, and Machine Learning** to enable continuous monitoring of vital signs in elderly individuals, particularly in long-term care environments.

The system collects physiological data through an ESP32-based device, transmits the measurements to the cloud, processes the collected data using a Machine Learning model for anomaly detection, and provides a mobile interface for real-time monitoring.

> **Note:** VitaSafe is an academic research prototype and is not intended to replace professional medical diagnosis or clinical decision-making.

---

## Overview

Continuous monitoring of vital signs can help caregivers and healthcare professionals identify potentially relevant physiological changes more quickly.

VitaSafe was designed to explore the technical feasibility of integrating:

- Embedded hardware and biomedical sensors
- Wireless IoT communication
- Cloud-based data storage
- Mobile health monitoring
- Machine Learning for anomaly detection

The system focuses on three main physiological parameters:

- **Heart rate (BPM)**
- **Blood oxygen saturation (SpO₂)**
- **Body temperature**

The collected data is transmitted to the cloud and made available through a mobile application for visualization and monitoring.

---

## System Architecture

The VitaSafe architecture integrates data acquisition, cloud storage, Machine Learning processing, and mobile visualization.

```text
┌──────────────────────────────┐
│          IoT Device          │
│                              │
│  ESP32                       │
│  ├── MAX30102                │
│  ├── MLX90614                │
│  └── MPU6050                 │
│                              │
│  Vital Signs Collection      │
└──────────────┬───────────────┘
               │
               │ Wi-Fi
               ▼
┌──────────────────────────────┐
│        Cloud Layer           │
│                              │
│  Firebase Realtime Database  │
│                              │
│  Data Storage & Synchroniz.  │
└──────────────┬───────────────┘
               │
               ├─────────────────────┐
               │                     │
               ▼                     ▼
┌────────────────────────┐  ┌────────────────────────┐
│   Machine Learning     │  │      Mobile App        │
│                        │  │                        │
│  Data Processing       │  │  Kotlin                │
│  Feature Extraction   │  │  Jetpack Compose       │
│  Random Forest         │  │  Firebase              │
│  Anomaly Detection     │  │  Real-Time Monitoring  │
└────────────────────────┘  └────────────────────────┘

The ESP32 collects physiological measurements and transmits them through Wi-Fi to Firebase Realtime Database. The data can then be accessed by the mobile application and processed by the Machine Learning pipeline for anomaly detection.

