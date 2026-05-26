> **Note:** This file is the original brainstorm. For current architecture, stack, API, and implementation plans, use the [documentation index](README.md). PostgreSQL references here are superseded by MongoDB Atlas per [stack.md](stack.md) and [adr/002-mongodb-atlas.md](adr/002-mongodb-atlas.md).

### Part 1: The "Aggressive" Flow (Solving Your Routine)

To fix the "morning mess" and "evening rush," the app needs to force a routine through **high friction**. 

**1. The "Hostage" Morning Routine (Stealing from *Alarmy* & *AppBlock*)**
*   **The Feature:** When your morning alarm goes off, the app locks your phone using Android's `SYSTEM_ALERT_WINDOW` (draw over other apps). You cannot access Instagram, email, or browser. 
*   **The Unlock Condition:** To unlock your phone for the day, you must:
    1. Scan an NFC tag or Barcode in your bathroom (forces you out of bed).
    2. Fill out your **Daily Intent**: Plan at least 3 tasks for today (existing or new); add more if you want.
    3. Block out your calendar for those tasks.
*   **Result:** Your morning is instantly structured before you are allowed to consume any dopamine.

**2. Daytime Harassment (Stealing from *Due* & *Beeminder*)**
*   **The Feature:** If you scheduled "Code backend" at 11:00 AM, at 11:00 AM the app takes over your screen. You have 5 minutes to hit **"Start Focus Session."**
*   **The Consequence:** If you ignore it, the app starts playing an annoying alarm sound every 2 minutes (like the *Due* app). If you want to take it to the extreme, integrate the Stripe API and charge yourself $5 for every task you skip without a valid reason, donating it to an anti-charity (like *Beeminder* does).

**3. The Visual Habit Grid (Stealing from *GitHub* & *Everyday.app*)**
*   **The Feature:** A beautifully brutal "Don't Break the Chain" (Seinfeld strategy) visual sheet. 
*   **The UI:** Use a grid matrix. Columns are days of the week, Rows are your habits (e.g., "Drink Water", "Read 10 pages", "Workout").
*   **The UX:** It turns green when done, red when missed. No gray areas. Seeing a sea of green is highly addictive, and seeing a red block ruins the aesthetic, pushing you to complete it. You review and check this off during the Evening Wrap-up.

---

### Part 2: The Tech Stack & Architecture

Since you are the sole developer, you need architectures that scale well but don't slow you down.

#### 1. Android App (Kotlin) - Architecture Blueprint
For a modern, robust Android app, you should use **Clean Architecture + MVVM (or MVI) + Jetpack Compose**. 

*   **UI Layer:** **Jetpack Compose**. It is perfect for building the complex visual Habit Grid quickly. Use a `LazyVerticalGrid` to map out your habit tracking sheet.
*   **Presentation Pattern:** **MVVM (Model-View-ViewModel)**. The ViewModel holds the state of your day (e.g., `isMorningRoutineComplete`), and Compose observes this state. If `isMorningRoutineComplete == false`, Compose draws the lock screen.
*   **Dependency Injection:** **Hilt**. It’s the Android standard and will make managing your API clients and local databases much easier.
*   **Local Database (Offline-first):** **Room**. Your life record shouldn't break if you lose internet. Save intents and habits locally to Room, then sync to the backend.
*   **Async/Reactivity:** **Kotlin Coroutines & Flow**. Use Flows to observe habit check-ins in real-time.

**Crucial Android APIs for "Aggressiveness":**
*   `SYSTEM_ALERT_WINDOW`: Allows you to draw your app over other apps (The Hostage screen).
*   `Foreground Services`: Required to keep your "Focus Timers" running without Android killing the app to save battery.
*   `AlarmManager` + `USE_EXACT_ALARM`: Android has battery optimizations that delay notifications. For an aggressive app, you need exact alarms to trigger the check-ins *exactly* at 11:00 AM.
*   `DeviceAdminReceiver` (Optional): If you *really* want to prevent yourself from uninstalling the app when you get mad at it, make it a device admin.

#### 2. Backend (Node.js) - Clean Architecture
To match your requirement for Node.js with Clean Architecture, I highly recommend using **NestJS**, or setting up **Express with a strict layered folder structure**.

*   **Framework:** **NestJS** natively enforces Clean Architecture concepts (Controllers, Services/Use Cases, Modules) using TypeScript. It is highly opinionated, which prevents spaghetti code.
*   **Architecture Layers:**
    *   *Domain/Entities:* Core logic (e.g., `User`, `Task`, `Habit`, `LifeLog`).
    *   *Use Cases/Services:* The business rules (e.g., `CalculateConsistencyScore`, `PenalizeUserForMissedTask`).
    *   *Controllers:* Express/NestJS routes (REST or GraphQL).
    *   *Adapters/Infrastructure:* Database connections, external AI APIs.
*   **Database:** **PostgreSQL** using **Prisma ORM**. Prisma generates type-safe queries and is a joy to use with TypeScript/Node.
*   **AI Integration Layer:** Add a module that runs on a Cron Job. At 8:00 PM, the Node server sends your daily logs to the OpenAI API, asks it to evaluate your day based on your strict goals, and generates an aggressive, coaching push notification sent to your phone.

---

### Part 3: Step-by-Step Execution Plan

Don't try to build the whole Life OS at once. Build it in layers:

*   **Phase 1: The Habit & Intent Core (Weeks 1-2)**
    *   *Node:* Setup PostgreSQL, Prisma, and basic CRUD APIs for Tasks and Habits.
    *   *Android:* Build the MVVM + Compose UI. Create the Daily Visual Habit Sheet and the Todo list.
*   **Phase 2: The Morning Hostage & Evening Review (Weeks 3-4)**
    *   *Android:* Implement the `SYSTEM_ALERT_WINDOW` logic. Build the morning screen that forces you to plan at least 3 tasks for the day.
    *   *Node:* Add an endpoint that saves the "Evening Review" text/voice note.
*   **Phase 3: The Aggressive Assistant (Weeks 5-6)**
    *   *Android:* Implement Exact Alarms and full-screen popups for scheduled tasks.
    *   *Node:* Integrate Firebase Cloud Messaging (FCM) to send remote push notifications from the server.
