# Radiation Monitoring System

Radiation Monitoring System is a Java Swing desktop application for managing radiation data collected across multiple monitoring stations, built with Maven and PostgreSQL.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 11 or later |
| Maven | 3.6 or later (or use IntelliJ's built-in Maven) |
| PostgreSQL | 13 or later |

---

## 1 – Database Setup

1. Open pgAdmin → right-click **Databases** → **Create** → name it `radiation_monitor_db`
2. Right-click the new database → **Query Tool**
3. Open `DatabaseSetup.sql`, paste the contents and press **F5** to run

This creates all tables and inserts 5 sample stations and 14 sample readings.

---

## 2 – Configure the Database Connection

Edit the constants at the top of `src/main/java/util/DatabaseManager.java`:

```java
private static final String URL      = "jdbc:postgresql://localhost:5432/radiation_monitor_db";
private static final String USER     = "postgres";
private static final String PASSWORD = "your_password";
```

---

## 3 – Build & Run

**From IntelliJ:**
- Open the project → IntelliJ detects `pom.xml` automatically
- Run `Main.java` with the green ▶ button

**Fat-jar (via IntelliJ Maven panel):**
- Maven panel → Lifecycle → double-click **package**
- Then run: `java -jar target/radiation-monitor-1.0-SNAPSHOT.jar`

---

## 4 – Application Features

| Feature | Description |
|---|---|
| **View stations** | All monitoring stations shown in the top table |
| **View readings** | Click a station to load its readings |
| **Add reading** | Click **+ Add** to open the form dialog |
| **Edit reading** | Select a row + **✎ Edit**, or double-click |
| **Delete reading** | Select a row + **✕ Delete** (confirmation dialog) |
| **Search stations** | Type a keyword in the top search bar |
| **Filter readings** | Type a keyword in the bottom filter bar |
| **Refresh** | **↺ Refresh** reloads all stations from the database |
| **Column sorting** | Click any column header to sort |
| **Color-coded rows** | Green = Normal, Yellow = Warning, Red = Critical |

---

## Project Structure

```
src/main/java/
├── Main.java
├── model/
│   ├── Entity.java                 – abstract base class
│   ├── MonitoringStation.java      – parent entity
│   └── RadiationReading.java       – child entity
├── repository/
│   ├── Repository.java             – generic CRUD interface
│   ├── StationRepository.java      – station-specific interface
│   ├── ReadingRepository.java      – reading-specific interface
│   └── impl/
│       ├── StationRepositoryImpl.java  – JDBC implementation
│       └── ReadingRepositoryImpl.java  – JDBC implementation
├── validator/
│   ├── Validator.java              – generic validator interface
│   ├── ValidationException.java
│   ├── StationValidator.java
│   └── ReadingValidator.java
├── service/
│   ├── IdentifiableService.java    – abstract generic service
│   ├── ServiceException.java
│   ├── StationService.java
│   └── ReadingService.java
├── ui/
│   ├── MainFrame.java              – main application window
│   └── ReadingFormDialog.java      – add/edit dialog
└── util/
    └── DatabaseManager.java        – JDBC connection factory
```
