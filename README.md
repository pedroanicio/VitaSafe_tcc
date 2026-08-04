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

```
The ESP32 collects physiological measurements and transmits them through Wi-Fi to Firebase Realtime Database. The data can then be accessed by the mobile application and processed by the Machine Learning pipeline for anomaly detection.

## Hardware

The prototype uses low-cost embedded hardware and biomedical sensors.

| Component | Purpose |
|---|---|
| **ESP32** | Main microcontroller and wireless communication |
| **MAX30102** | Heart rate and SpO₂ measurement |
| **MLX90614** | Non-contact temperature measurement |
| **MPU6050** | Motion sensing used during system development |

The ESP32 was selected due to its integrated Wi-Fi connectivity, low cost, and flexibility for embedded system development.

## Software

### Mobile Application

The Android application was developed using:

- **Kotlin**
- **Jetpack Compose**
- **Firebase Authentication**
- **Firebase Realtime Database**

The application provides an interface for authenticated users to monitor physiological measurements and patient information.

The monitoring interface includes information such as:

- **Current heart rate**
- **SpO₂**
- **Body temperature**
- **Historical measurements**
- **Patient information**
