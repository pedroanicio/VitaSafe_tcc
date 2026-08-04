# Elder Health Monitor

A mobile application and IoT solution for monitoring the health of elderly individuals in real time. The system uses an **ESP32-based device** to collect vital signs and send them to the **Firebase Realtime Database**, which are then visualized in an **Android app**.

## 🩺 Features

### Android App (Kotlin + Jetpack Compose)
- User authentication via Firebase (email & password)
- Two user roles: **Caregiver** and **Doctor**
- List of assigned elderly patients
- Dashboard with:
  - Heart rate (BPM)
  - Oxygen saturation (SpO2)
  - Body temperature
  - Historical graph (BPM throughout the day)
- Patient registration form
- Local alert sound on fall detection

### ESP32 Device
- Collects:
  - Heart rate (MAX30102 sensor)
  - Oxygen level (MAX30102)
  - Temperature (MLX90614)
- Sends data to Firebase via Wi-Fi
- Bluetooth provisioning available (under development)

---

## 🛠️ Technologies Used

| Layer             | Technology                          |
|------------------|--------------------------------------|
| Mobile App       | Kotlin, Jetpack Compose              |
| Firebase         | Realtime Database, Authentication    |
| IoT Device       | ESP32 (C++), MAX30102, MPU6050, MLX90614       |
| Communication    | Wi-Fi (HTTP + Firebase SDK), BLE     |

---

## 🔧 Setup Instructions

### 1. Firebase Configuration
- Create a Firebase project.
- Enable **Authentication (Email/Password)**.
- Create a **Realtime Database** with the following rules (temporarily, for dev):

```json
{
  "rules": {
    ".read": "now < 1752721200000",
    ".write": "now < 1752721200000"
  }
}
