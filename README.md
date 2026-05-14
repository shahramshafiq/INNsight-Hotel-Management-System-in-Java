# INNsight — AI-Driven Hotel Management System
**Your Digital Concierge**

> Developed by: Shahram Shafiq (24i-2541) · Sara Nasir (24i-0719) · Taha Jabbar (24i-0816)

---

## Overview

INNsight is a desktop hotel management system that combines a JavaFX UI with an AI-powered chatbot (Botpress) for natural-language hotel operations. Staff can manage rooms, reservations, and guests manually through the dashboard, or let the AI handle it through conversation.

The codebase strictly follows **GRASP patterns** (Information Expert, Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variation) and a **layered architecture** (UI → Controller → Model/Repository → Database).

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 21 |
| Backend API | Java HTTP Server (port 8080) |
| Database | SQLite |
| AI Chatbot | Botpress (embedded via WebView) |
| Tunnel | ngrok (connects Botpress to local API) |
| Build | Maven |

---

## Architecture

```
JavaFX Desktop App
        ↕  (REST calls)
Java HTTP API  →  port 8080
        ↕
SQLite Database  →  C:\Users\<you>\INNsight\innsight.db
        ↑
    ngrok tunnel
        ↑
Botpress AI Cloud
```

The desktop app starts the API server internally on launch. ngrok exposes port 8080 to the internet so Botpress can write back to your local database.

---

## Features

### Dashboard
- Live stat cards: occupancy %, active bookings, available rooms, total guests, total revenue
- AI banner with quick access to the chatbot
- Recent reservations table

### Room Management
- Add, edit, delete rooms
- Room types: Single, Double, Suite, Deluxe, Penthouse
- Status tracking: Available, Booked, Maintenance
- Color-coded status badges

### Reservations
- Card grid layout — each booking displayed as a "boarding pass" style card
- Shows: guest avatar, guest name, booking ID, room number, CHECK IN → nights → CHECK OUT dates, total cost, payment badge, status badge
- Create new bookings (new or existing customer)
- Modify check-in / check-out dates via per-card Modify button
- Cancel bookings via per-card Cancel button (auto-disabled for already-cancelled reservations)
- Status badges: Confirmed, Pending, Cancelled
- Payment status: Paid / Unpaid

### Customer Directory
- Card grid layout — each guest displayed as a profile card with color-coded avatar
- Shows: initials avatar (color unique per guest), full name, email (with tooltip), phone
- Add, edit, delete guest profiles

### AI Chat (Botpress)
- Embedded chatbot inside the app
- Supported via natural language:
  - Book a room
  - Check available rooms
  - Cancel a reservation
  - Modify reservation dates
- Returns reservation ID on successful booking

### UI
- **Login splash screen** — on launch, the company logo fades in (28% opacity, centred) and holds for 5 seconds before the login card slides up
- Fullscreen on every launch
- Dark navy sidebar with amber active navigation highlight
- 4 color themes: Dark, Ocean, Sunset, Void (switch via colored dots in sidebar)
- Card grid layouts for Rooms, Reservations, and Customers — no tables
- Smooth page transitions (300 ms slide-in) and staggered card entrance animations
- Hover effects: scale + accent border color on all cards
- Shake animation on wrong login attempt
- Rotate animation on refresh buttons

---

## Default Login

| Field | Value |
|---|---|
| Username | admin |
| Password | admin123 |

---

## Running the App (Normal Use)

### Step 1 — Launch the App

Double-click the **INNsight** shortcut on the Desktop.

The app opens fullscreen. Log in with `admin` / `admin123`.

> The database is created automatically at:
> `C:\Users\<your-username>\INNsight\innsight.db`
> It persists between sessions and survives rebuilds.

---

### Step 2 — Start ngrok (Required for AI Chat)

Open CMD and navigate to where ngrok is saved:

```cmd
cd C:\Users\ssg79\OneDrive\Desktop
ngrok.exe http 8080
```

ngrok will display something like:
```
Forwarding   https://abc-xyz-123.ngrok-free.dev -> http://localhost:8080
```

Copy that `https://` URL.

---

### Step 3 — Update Botpress with the New ngrok URL

> ngrok gives a new URL every session, so this step is required each time before a demo.

1. Go to your Botpress dashboard and open the INNsight bot
2. Open the **bookRoom** workflow → find the Execute Code card → replace the old URL with the new ngrok URL
3. Do the same for **cancelReservation** and **modifyReservation** workflows
4. Click **Publish** in Botpress

---

### Step 4 — Use the AI Chat

In the app, click **AI Chat** in the sidebar. The Botpress chatbot loads and is ready. Example prompts:

- *"I want to book a room"*
- *"What rooms are available?"*
- *"Cancel reservation 5"*
- *"Change my check-out date to December 20"*

---

## Rebuilding the Desktop App (After Code Changes)

Follow these steps every time you update the source code and want to regenerate the `.exe`.

---

### Step 1 — Build the JAR in IntelliJ

Open the Maven panel on the right side of IntelliJ:
- Maven → Lifecycle → **clean** — double-click, wait for `BUILD SUCCESS`
- Maven → Lifecycle → **package** — double-click, wait for `BUILD SUCCESS`

---

### Step 2 — Copy Dependencies

In the Maven panel:
- Maven → Plugins → dependency → **dependency:copy-dependencies** — double-click, wait for `BUILD SUCCESS`

This puts all `.jar` files into `target\libs\`

---

### Step 3 — Copy Main JAR into libs

In the IntelliJ terminal (bottom of IntelliJ):

```
copy target\hotel_ai-1.0-SNAPSHOT.jar target\libs\
```

---

### Step 4 — Close the App if it is Running

If INNsight is currently open, close it completely before the next step. If it won't close normally:

- Press `Ctrl + Alt + Delete` → Task Manager
- Find **INNsight** → End Task
- Find any **Java** process → End Task

This is important. If the app is running, the next step will fail because Windows locks the files.

---

### Step 5 — Delete the Old Build Output

In the IntelliJ terminal:

```
Remove-Item -Recurse -Force -Path "INNsight_App"
```

Confirm it is gone:

```
Test-Path INNsight_App
```

Should say `False`. If it still says `True`, the app is still running — go back to Step 4.

---

### Step 6 — Run jpackage

```
jpackage --input target\libs --main-jar hotel_ai-1.0-SNAPSHOT.jar --main-class com.hotel.hotel_ai.Main --name INNsight --app-version 1.0 --type app-image --dest INNsight_App
```

Wait for it to finish with no errors.

---

### Step 7 — Recreate the Desktop Shortcut

- Open File Explorer and go to `INNsight_App\INNsight\`
- Right-click `INNsight.exe` → **Send to** → **Desktop (create shortcut)**
- Go to the Desktop, delete the old INNsight shortcut
- Rename the new shortcut to `INNsight`

Double-click it — the updated app opens fullscreen.

---

## Project Structure

```
INNsight/
├── src/main/java/com/hotel/hotel_ai/
│   ├── Main.java                            ← Entry point
│   ├── ui/
│   │   └── ChatbotUI.java                   ← All UI screens and logic (GRASP: Indirection)
│   ├── controller/
│   │   ├── IAIService.java                  ← AI service interface (GRASP: Protected Variation)
│   │   ├── AIEngine.java                    ← Botpress integration (GRASP: Indirection)
│   │   ├── RoomController.java              ← Room use-case handler (GRASP: Controller)
│   │   ├── ReservationController.java       ← Booking use-case handler (GRASP: Controller, Creator)
│   │   ├── CustomerController.java          ← Guest use-case handler (GRASP: Controller)
│   │   └── ApiServer.java                   ← REST API port 8080 (GRASP: Controller)
│   ├── model/
│   │   ├── BaseEntity.java                  ← Abstract base for all entities (GRASP: Polymorphism)
│   │   ├── Room.java                        ← (GRASP: Information Expert)
│   │   ├── Reservation.java                 ← (GRASP: Information Expert, Creator)
│   │   └── Customer.java                    ← (GRASP: Information Expert)
│   └── repository/
│       ├── IRepository.java                 ← Generic CRUD interface (GRASP: Protected Variation)
│       ├── IRoomRepository.java             ← Room-specific interface, extends IRepository<Room>
│       ├── ICustomerRepository.java         ← Customer-specific interface, extends IRepository<Customer>
│       ├── IReservationRepository.java      ← Reservation-specific interface, extends IRepository<Reservation>
│       ├── DatabaseManager.java             ← SQLite Singleton (GRASP: Pure Fabrication)
│       ├── RoomRepository.java              ← implements IRoomRepository
│       ├── ReservationRepository.java       ← implements IReservationRepository
│       └── CustomerRepository.java          ← implements ICustomerRepository
├── src/main/resources/
│   ├── styles.css                           ← All UI styling
│   ├── logo_main.png                        ← Company logo
│   └── logo2.png
├── INNsight.ico                             ← App icon for the .exe
├── pom.xml
└── README.md
```

---

## API Endpoints

The app exposes these endpoints internally on port 8080, used by Botpress via ngrok:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/rooms` | All rooms |
| GET | `/api/rooms/available` | Available rooms only |
| GET | `/api/reservations` | All reservations |
| POST | `/api/reservations` | Create reservation |
| POST | `/api/reservations/cancel` | Cancel reservation |
| POST | `/api/reservations/modify` | Modify reservation dates |
| GET | `/api/customers` | All customers |
| GET | `/api/dashboard` | Dashboard stats |

---

## UI Themes

Click the colored dots at the bottom of the sidebar to switch themes:

| Dot color | Theme | Accent |
|---|---|---|
| Amber | Dark | Deep navy + amber gold |
| Cyan | Ocean | Deep blue + cyan |
| Rose | Sunset | Deep crimson + rose |
| Purple | Void | Deep purple + violet |

---

## Demo Checklist

Run through this before every evaluation or demo:

- [ ] Close INNsight if it is open
- [ ] Open CMD → `cd C:\Users\ssg79\OneDrive\Desktop` → `ngrok.exe http 8080`
- [ ] Copy ngrok URL → update Execute Code cards in bookRoom, cancelReservation, modifyReservation → Publish
- [ ] Launch INNsight shortcut from Desktop — app opens fullscreen
- [ ] Splash screen: logo appears centered for 5 seconds, then login card slides up
- [ ] Login: `admin` / `admin123`
- [ ] Show Dashboard — stat cards, AI banner, recent reservations
- [ ] Show Rooms — card grid, add a room, check status badges, hover effects
- [ ] Show Reservations — boarding-pass card grid, create a booking, modify/cancel per card
- [ ] Show Customers — profile card grid, add a guest, color-coded avatars
- [ ] Show AI Chat — book a room via chatbot, show reservation ID returned
- [ ] Demo cancel or modify via chatbot
- [ ] Show theme switcher — click the colored dots in the sidebar (Dark / Ocean / Sunset / Void)

---

## Troubleshooting

**App won't open / shortcut broken**
- The `INNsight_App\INNsight\` folder may have moved. Recreate the shortcut pointing to `INNsight.exe` inside that folder.

**AI Chat shows blank page**
- ngrok is not running. Start it and update Botpress.

**AI Chat shows "Error loading shareable webchat"**
- The app now handles this automatically. It silently retries up to 3 times on load, and checks every 15 seconds during use. If the overlay does appear, click **Retry** — the chatbot will reload. As a last resort, click "Open in browser" in the overlay.

**Botpress books a room but database does not update**
- ngrok URL has changed. Update the Execute Code cards in all three Botpress workflows and republish.

**Can't delete INNsight_App folder**
- The app is still running. Open Task Manager, end the INNsight and Java processes, then retry.

**App opens in a small window instead of fullscreen**
- This should not happen. If it does, click the maximize button — the app will remember nothing between launches other than the database.

**jpackage command not found**
- Make sure you are using JDK 21 and that the JDK `bin` folder is in your system PATH.
