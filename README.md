# 🚌 RideEasy — Real-Time Bus Tracking & Crowd Management System

> **Final Year Android + AI Project** | Chennai, Tamil Nadu, India
> Developer: **Pradeesh** | Routes: **99A** (Tambaram → Sholinganallur) · **119** (Guindy → Sholinganallur)

---

## 📖 Overview

RideEasy is a full-stack Android system that lets passengers see **live bus locations**, **real-time crowd levels**, and **seat availability** — all on their phone. Conductors manage ticketing and GPS from a dedicated app, while an AI-powered Camera App counts passengers automatically using **YOLOv8** on-device.

```
┌─────────────────┐     REST/Socket.IO     ┌──────────────────────┐
│  Conductor App  │ ──── tickets + GPS ──▶ │  Node.js Backend     │
│ (RideEasyCon.)  │                        │  (Kali Linux VM)     │
└─────────────────┘                        │  MongoDB + Firebase  │
                                           └──────────┬───────────┘
┌─────────────────┐     live crowd data              │
│  Camera App     │ ──── entry/exit count ──▶        │
│ (YOLOv8 TFLite) │                        ┌──────────▼───────────┐
└─────────────────┘                        │  Firebase Realtime   │
                                           │  Database            │
┌─────────────────┐     real-time push     └──────────┬───────────┘
│  Passenger App  │ ◀──── live updates ───────────────┘
│  (RideEasy)     │
└─────────────────┘
```

---

## 📱 Apps

### 1. RideEasy — Passenger App (`/RideEasy`)
The main passenger-facing app.

| Feature | Description |
|---------|-------------|
| 🗺️ Live Map | Real-time bus location on Google Maps |
| 👥 Crowd Status | FREE / MODERATE / CROWDED with % bar |
| 💺 Seat Info | Available seats + standing passenger count |
| 🔍 Bus Search | Filter by bus number, route, or plate |
| 🎫 Ticket Booking | Fare calculated by stop difference |
| ⭐ Smart Recommendation | Suggests less-crowded alternatives |

### 2. RideEasyConductor — Conductor App (`/RideEasyConductor`)
Used by bus conductors during their shift.

| Feature | Description |
|---------|-------------|
| 🔐 Login | JWT-authenticated conductor login |
| 🎫 Ticket Issue | Issue tickets with from/to stop selection |
| 📍 Live GPS | Sends real-time bus location every 5 sec |
| 👤 Alighting | Record passengers alighting at stops |
| 📊 Dashboard | Live passenger count, revenue, crowd status |
| 🔄 Shift End | Resets bus to offline on logout |

### 3. RideEasyCameraApp — AI Counter App (`/RideEasyCameraApp`)
Mounted at the bus door to count passengers automatically.

| Feature | Description |
|---------|-------------|
| 🤖 YOLOv8 | On-device person detection (TFLite) |
| ➡️ Line Crossing | Virtual line at 50% frame height |
| ⬆️⬇️ Entry/Exit | Centroid-based direction detection |
| 📡 Auto Sync | Pushes count to backend every 2 seconds |
| 🔐 Auth | JWT-authenticated via conductor credentials |

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Android | Java, Android SDK (API 26+) |
| UI | Material Components 3, Custom Dark Theme |
| Camera | CameraX 1.3.4 |
| AI | TensorFlow Lite 2.14 (YOLOv8n) |
| Maps | Google Maps SDK for Android |
| Networking | OkHttp3 |
| Real-time | Firebase Realtime Database, Socket.IO |
| Auth | JWT (JSON Web Tokens) |
| Backend | Node.js + Express |
| Database | MongoDB + Firebase |
| Server OS | Kali Linux (Hyper-V VM) |

---

## 🗺️ Bus Routes

### Route 99A — Tambaram → Sholinganallur
`Tambaram → Perungalathur → Guduvanchery → Urapakkam → Vandalur → Mudichur → Pammal → Pallavaram → Chromepet → Tambaram East → St. Thomas Mount → Meenambakkam → Tirusulam → Pazhavanthangal → Nanganallur → Alandur → St. Thomas Mount → Guindy → Velachery → Perungudi → Sholinganallur`

### Route 119 — Guindy → Sholinganallur
`Guindy → Velachery → Perungudi → Sholinganallur`

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio (Hedgehog or later)
- Android phone with API 26+
- Node.js backend running on your local network
- Firebase project with Realtime Database enabled

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/RideEasy.git
cd RideEasy
```

### 2. Configure Server IP
Update `AppConfig.java` in **each app** with your machine's WiFi IP:
```java
// RideEasy/app/src/main/java/com/rideeasy/passenger/AppConfig.java
// RideEasyConductor/app/src/main/java/com/rideeasy/conductor/AppConfig.java
// RideEasyCameraApp/app/src/main/java/com/rideeasy/camera/AppConfig.java

public static final String SERVER_URL = "http://YOUR_WIFI_IP:3000";
```
> Run `ipconfig` on Windows to find your WiFi IPv4 address.

### 3. Add Firebase Config
Place your `google-services.json` from Firebase Console into:
```
RideEasy/app/google-services.json
RideEasyConductor/app/google-services.json
```

### 4. Add YOLOv8 Model
Place the model into:
```
RideEasyCameraApp/app/src/main/assets/yolov8n_float32.tflite
```
Download from: [Ultralytics YOLOv8](https://github.com/ultralytics/assets/releases)

### 5. Start the Backend
```bash
# On your Kali Linux server
cd backend
npm install
node seed.js      # seed initial bus/conductor data
node server.js    # start the API server
```

### 6. Build & Run
Open each project folder in Android Studio separately:
- `RideEasyConductor/` → Run on conductor's phone
- `RideEasy/` → Run on passenger's phone
- `RideEasyCameraApp/` → Run on phone mounted at bus door

---

## 🔑 Test Credentials

| Role | ID | Password |
|------|----|----------|
| Conductor 1 | `CON01` | `password123` |
| Conductor 2 | `CON02` | `password123` |

---

## 🚀 How to Run End-to-End

1. **Start backend** server on Kali Linux
2. **Conductor App** → Login → Start GPS → Issue tickets
3. **Camera App** → Login with Conductor ID → Point at door → Counts passengers
4. **Passenger App** → Open app → See live bus with crowd status → Book ticket

---

## 📂 Project Structure

```
AndroidStudioProjects/
├── RideEasy/                    # Passenger App
│   └── app/src/main/java/com/rideeasy/passenger/
│       ├── AppConfig.java       # Server URL & route constants
│       ├── HomeActivity.java    # Live bus list screen
│       ├── SearchActivity.java  # Bus search
│       ├── BusResultActivity.java # Bus detail + crowd info
│       ├── MapActivity.java     # Live map view
│       ├── BookingActivity.java # Ticket booking
│       └── BusCardAdapter.java  # RecyclerView adapter
│
├── RideEasyConductor/           # Conductor App
│   └── app/src/main/java/com/rideeasy/conductor/
│       ├── AppConfig.java       # Server URL
│       ├── LoginActivity.java   # JWT login
│       ├── MainActivity.java    # Dashboard + ticketing + GPS
│       └── SplashActivity.java  # Splash screen
│
├── RideEasyCameraApp/           # AI Camera App
│   └── app/src/main/java/com/rideeasy/camera/
│       ├── AppConfig.java       # Server URL & detection config
│       ├── MainActivity.java    # Login + CameraX + detection
│       ├── YoloDetector.java    # YOLOv8 TFLite inference
│       └── OverlayView.java     # Bounding box + line overlay
│
└── README.md                    # This file
```

---

## 📸 Features Preview

### Passenger App
- Premium dark UI with glassmorphism cards
- Live crowd percentage bar (green/yellow/red)
- Seat count + standing passenger indicator
- Smart bus recommendation banner

### Conductor App
- JWT-secured login with session persistence
- One-tap ticket issuance with fare display
- Real-time GPS broadcasting every 5 seconds
- Alighting passenger recording

### Camera App
- Live YOLOv8 bounding boxes on camera preview
- Virtual counting line with entry/exit indicators
- Real-time in-bus passenger count overlay

---

## 🛠️ Known Limitations

- Backend must run on the **same local network** as the phones
- The `.tflite` model must be manually added to `assets/` (not committed to git due to size)
- `google-services.json` is not committed for security reasons

---

## 📄 License

This project was developed as a **Final Year Academic Project** at [Your College Name].
All rights reserved © 2026 Pradeesh.
