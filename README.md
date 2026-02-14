# Service Management Tool (SMT) using Camunda

![Java](https://img.shields.io/badge/Java-17_LTS-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green)
![Camunda](https://img.shields.io/badge/Camunda_BPM-7.24-red)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Relational_DB-blue)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED)
![Render](https://img.shields.io/badge/Deployment-Render_Cloud-black)

> **Frankfurt University of Applied Sciences** > **Master of Engineering in Information Technology | WS 2025-26** > **Course:** Agile Development in Cloud Computing Environments  
> **Date:** 28.01.2026

## 📋 Table of Contents
1. [Introduction & Motivation](#-introduction--motivation)
2. [Team & Stakeholders](#-team--stakeholders)
3. [System Architecture](#-system-architecture)
4. [Technology Stack](#-technology-stack)
5. [Agile Development Process](#-agile-development-process)
6. [Key Features & Business Logic](#-key-features--business-logic)
7. [Database Design](#-database-design)
8. [End-to-End Workflow (Integration)](#-end-to-end-workflow-integration)
9. [Installation & Deployment](#-installation--deployment)
10. [Technology Comparison](#-technology-comparison)
11. [References](#-references)

---

## 📖 Introduction & Motivation

In the rapidly evolving landscape of IT, organizational agility is a necessity. Modern enterprises rely on external workforces (freelancers, consultants), but procurement often remains trapped in legacy processes—fragmented emails, manual spreadsheets, and compliance risks.

**The Service Management Tool (SMT)** is a middleware solution designed to digitize and orchestrate the end-to-end procurement lifecycle. 

### Core Objectives
* **Eliminate Silos:** Connect Project Managers, Legal, and Procurement in real-time.
* **Ensure Compliance:** Prevent hiring without valid frame agreements (via Group 2b integration).
* **Central Orchestration:** Use **Camunda BPM** as a state machine to manage long-running processes (Draft → Approval → Market → Order).
* **Interoperability:** Serve as an integration point for Workforce Management (Group 1b), Contract Management (Group 2b), External Providers (Group 4b), and Reporting (Group 5b).

---

## 👥 Team & Stakeholders

### Submitted By (Team 3b)
| Name | Student ID | Role |
| :--- | :--- | :--- |
| **Manoj Hanumanthu** | 1566325 | **Product Owner** (Backlog, BPMN Design, Requirements) |
| **Aman Basha Patel** | 1565430 | **Scrum Master** (Facilitator, Documentation, Blocker Removal) |
| **Muhammad Ahsan Ijaz** | 1566312 | **Tech Lead** (Architecture, Security, Database Design) |
| **Saquib Attar** | 1567041 | **DevOps / Testing** (Cloud Deployment, CI/CD, Postman) |

**Supervisor:** Dr. Patrick Wacht

### Integration Partners
* **Group 1b:** Workforce Management (Requirements Source)
* **Group 2b:** Contract Management (Legal Validation)
* **Group 4b:** External Providers (Offer Submission)
* **Group 5b:** Reporting & Analytics (Data Consumption)

---

## 🏗 System Architecture

The SMT follows a modular, Service-Oriented Architecture (SOA). It acts as a nexus between four distinct organizational units.

### Components
1.  **Authentication & RBAC:** Built on Spring Security. Distinguishes between:
    * `PM` (Project Manager): Initiates requests.
    * `PO` (Procurement Officer): Approves budgets and validates selections.
    * `RP` (Resource Planner): Finalizes logistics.
2.  **Process Orchestration Engine:** Embedded **Camunda BPM 7**. Persists state (e.g., "Waiting for External API") across server restarts.
3.  **Integration Layer (REST APIs):** Secure endpoints (`ExternalIntegrationController`) for data exchange with Groups 1b and 4b.
4.  **Provider Offer Management:** Logic for aggregating, scoring, and ranking incoming offers.
5.  **Reporting Interface:** Read-only API (`/api/reporting`) utilizing flattened DTOs for Group 5b.

### Architecture Layers
* **Controller Layer:** Entry point, UI rendering, REST endpoints.
* **Service Layer:** Business logic, transaction management.
* **Delegate Layer:** `JavaDelegates` connecting BPMN tasks to Java code (e.g., `CheckContractDelegate`).
* **Repository Layer:** Spring Data JPA abstractions.

---

## 💻 Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 17 LTS | Strong typing, reliable for financial transactions. |
| **Framework** | Spring Boot 3.2 | Web, Data JPA, Security. Convention-over-configuration. |
| **Workflow Engine** | Camunda BPM 7 | **Embedded**. Ensures transactional consistency between business data and process state. |
| **Database** | PostgreSQL | Single source of truth for business entities and Camunda internal tables (`ACT_RU_*`). |
| **Frontend** | Thymeleaf + Bootstrap 5 | Server-side rendering. Custom **"Aurora"** glass-morphism theme. |
| **Deployment** | Docker + Render | Containerized PaaS deployment using `eclipse-temurin:17-jdk-alpine`. |
| **Build Tool** | Maven | Dependency management. |
| **Testing** | Postman | End-to-End integration testing collections. |

---

## 🔄 Agile Development Process

The project was executed over **3 Sprints** using a Hybrid Agile (Scrum + Kanban) framework.

### Sprint Breakdown
* **Sprint 1 (Foundation):** Setup Spring Boot, PostgreSQL, RBAC, and basic "Draft" creation APIs.
* **Sprint 2 (The Workflow Engine):** Implemented BPMN modeling, `CheckContractDelegate`, `PublishToProvidersDelegate`, and fixed "NullValueException" errors in Camunda tasks.
* **Sprint 3 (UI & Cloud):** Developed "Aurora" dashboard, Reporting APIs, and deployed to Render.

### Key Practices
* **Daily Stand-ups:** 10:00 AM (15 mins). Focused on immediate blockers (e.g., JSON parsing issues).
* **Kanban Board:** Columns for To Do, In Progress, Verify, Done.
* **Retrospectives:** Addressed issues like "Optimistic Locking" (Fixed using `camunda:asyncBefore="true"`).

---

## ⚙️ Key Features & Business Logic

### 1. Orchestrated Workflow Management
A BPMN 2.0 compliant process enforces a standardized path:
`Draft` → `Contract Validation` → `PO Approval` → `Market Publication` → `Offer Evaluation` → `Final Order`.

### 2. Automated Contract Validation
Synchronous integration with Group 2b. Before market publication, the system validates the frame agreement. If invalid, the process terminates immediately.

### 3. "Aurora" Dashboard (UI/UX)
* **Glass-morphism Design:** Semi-transparent panels with "Nebula" gradient backgrounds.
* **Detail Modals:** "Eye" icon opens detailed JSON payloads (skills, rates) without cluttering the main table.
* **Role-Based Views:** UI elements utilize `sec:authorize` to hide actions based on the user (e.g., PM cannot see "Approve" buttons).

### 4. Decoupled Reporting
A Pull-based model exposes data via `/api/reporting`. Transforms raw transactional data into clean DTOs to avoid circular references and protect DB integrity.

---

## 🗄 Database Design

The schema is normalized and hosted on PostgreSQL.

* **`service_requests`:** Parent table. Stores Title, Budget, Skills, Status (ENUM), `internal_request_id`.
* **`provider_offers`:** Child table (Many-to-One). Stores Bids, Commercial/Technical Scores.
* **`service_orders`:** Final artifact (One-to-One). Created only upon successful process completion.
* **Camunda Tables:** `ACT_RU_EXECUTION` (Runtime state), `ACT_HI_PROCINST` (History/Audit Log).

> **Audit Log Proof:** We utilize `ACT_HI_ACTINST` as a "Black Box Recorder" to prove that the engine—not just Java code—is driving the logic.

---

## 🚀 End-to-End Workflow (Integration)

### Step 1: Request Initiation (Group 1b → SMT)
* Group 1b pushes a JSON payload via REST.
* Request appears in SMT Dashboard as **DRAFT**.

### Step 2: Approval & Publication (SMT → Group 2b → Group 4b)
* PM starts the process.
* **Automated:** `CheckContractDelegate` calls Group 2b API.
    * *Log:* `[API OUT] Validating Contract... [API IN] Eligible.`
* **Manual:** PO logs in and approves the budget.
* **Automated:** System publishes requirements to Group 4b.

### Step 3: Offer Submission (Group 4b → SMT)
* External providers submit candidates via API.
* PM views "Received Offers" in the UI, complete with calculated scores.

### Step 4: Selection & Order (SMT → Group 1b)
* PM selects an offer; PO validates it.
* System sends "Recommendation" to Group 1b.
* Group 1b sends "Acceptance" callback.
* RP confirms logistics.
* **Result:** Status updates to **COMPLETED**, Service Order generated.

---

## 🛠 Installation & Deployment

### Prerequisites
* Java 17
* Maven
* PostgreSQL
* Docker (Optional)

### Local Setup
1.  **Clone the Repo:**
    ```bash
    git clone [https://github.com/YOUR_REPO_NAME/smt-camunda.git](https://github.com/YOUR_REPO_NAME/smt-camunda.git)
    ```
2.  **Configure Database:**
    Edit `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/smt_db
    spring.datasource.username=postgres
    spring.datasource.password=yourpassword
    ```
3.  **Run Application:**
    ```bash
    mvn spring-boot:run
    ```
4.  **Access:**
    * Dashboard: `http://localhost:8080`
    * Camunda Cockpit: `http://localhost:8080/camunda`

### Cloud Deployment (Render)
The application is containerized using Docker.
**Dockerfile Strategy:** Uses `eclipse-temurin:17-jdk-alpine` to minimize image size.

**Environment Variables (Critical):**
When deploying to Render, ensure these variables are set to prevent "localhost" connection errors:
* `DB_URL`: Internal URL of the managed PostgreSQL instance.
* `MAIL_PASSWORD`: For notification services.

---

## ⚖️ Technology Comparison

Why we chose **Embedded Camunda** over alternatives:

| Approach | Architecture | Pros | Cons |
| :--- | :--- | :--- | :--- |
| **Team 3b (Us)** | **Spring Boot + Embedded Camunda** | **Transactional Consistency**, Java Delegate power, Full UI control. | Initial setup complexity. |
| **Team 3a** | React + Spring Boot (No Engine) | UI Freedom (React). | Complex state management ("Zombie" states), Security boilerplate. |
| **Team 3c** | Flowable (Dockerized) | Fast startup. | Tooling friction (Bruno vs Postman), limited customization. |
| **Team 3d** | Pega (Low-Code) | Rapid workflow modeling. | **Black-box debugging**, High cost/Lock-in, Over-engineered for simple CRUD. |

---

## 🔗 References

1.  **Camunda Blog:** "Moving from embedded to remote workflow engines" (Feb 2022). [Link](https://camunda.com/blog/2022/02/moving-from-embedded-to-remote-workflow-engines/)
2.  **Baeldung:** "Spring Boot + embedded Camunda" by M. Vollmer (2022). [Link](https://www.baeldung.com/spring-boot-embedded-camunda)
3.  **Documentation:** Camunda Platform 7.24 Manual. [Link](https://docs.camunda.org/manual/7.24/)
4.  **Visual Paradigm:** "From small teams to scaling agile". [Link](https://www.visual-paradigm.com/scrum/from-small-teams-to-scalingagile/1000)

---
*Frankfurt University of Applied Sciences | WS 2025-26*
