# Product Requirements Document: Device status and alert handling

**Status:** Draft (approval pending)<br>
**Version:** 0.1<br>
**Date:** 2026-10-20<br>
**Information sources:** Stakeholder request; no user interviews or usage data supplied.

## Overview

### Problem statement

Platform operators need to register smart devices, understand their latest connection status, inspect telemetry, and track device-reported alerts. The scope comes from stakeholder input; no operator interviews or usage baseline were supplied.

### Solution summary

Provide a multi-tenant IoT management capability with a platform-provided MQTT Broker and operator workflows for device status, telemetry, and alert handling.

### Target users

| User / segment | Need and context | Evidence / status |
| --- | --- | --- |
| Platform operator | Register devices and monitor connectivity and alerts. | User request / Confirmed direction |
| Tenant operator | Manage devices and alerts within a tenant. | Inferred from multi-tenancy / Proposed |

### Evidence and assumptions

- **Evidence:** Stakeholder request identifies platform operators, device onboarding, telemetry, and device-reported alerts.
- **Assumptions:** Tenant self-service and credential lifecycle needs remain unvalidated.

## Goals & Success Metrics

### Goals

- **G-1:** Operators can identify devices that need attention.
- **G-2:** Operators can track reported alerts to a disposition.

### Success metrics

| Goal | Metric | Current baseline | Target | Timeline / status |
| --- | --- | --- | --- | --- |
| G-1 | Time to identify an offline device | TBD | TBD | TBD / Proposed |
| G-2 | Time and proportion of alerts confirmed, assigned, and closed | TBD | TBD | TBD / Proposed |

### Non-goals

- Selecting a cloud provider, Broker implementation, storage technology, or API shape; define those in canonical design documents.

## User Stories

| ID | User story | Priority | Goal / source |
| --- | --- | --- | --- |
| US-1 | As a platform operator, I want to register a device and see whether it connects, so I can manage device onboarding. | Must (proposed) | G-1 / stakeholder request |
| US-2 | As an authorized operator, I want to inspect device telemetry, so I can understand recent device behavior. | Must (proposed) | G-1 / stakeholder request |
| US-3 | As an authorized operator, I want to confirm, assign, and close a device-reported alert, so its handling can be tracked. | Must (proposed) | G-2 / stakeholder request |

## Scope

### In scope / product direction

- Tenant-scoped device registration and connection status.
- MQTT connectivity through a platform-provided Broker using device key credentials.
- Device telemetry viewing and device-reported alert handling.

### Out of scope

The first release batch is not yet selected. Proposed first-release exclusions are listed below; these are not permanent product non-goals.

### Future considerations

Capabilities proposed for later phases are shown as Deferred in the phase table.

**Current release / delivery batch:** TBD; the PRD review must select the first customer-usable capabilities.

### Release phases and delivery batches

| Phase / batch | User outcome and capability summary | Dependencies | Scope status |
| --- | --- | --- | --- |
| Phase 1: Device access and visibility | Device registration, MQTT authentication, connection state, recent telemetry. | Tenant authorization and credential lifecycle are prerequisites. | Proposed |
| Phase 2: Alert handling and operations | Device-reported alert handling; search, telemetry history, and device lifecycle candidates. | Builds on phase 1; release batch remains undecided. | Proposed |
| Phase 3: Scale and governance | Audit, bulk operations, telemetry governance, and tenant self-service candidates. | Include based on pilot scale and team needs. | Proposed |
| Phase 4: Alert automation | Rule alerts and notification escalation. | Requires stable telemetry models and alert handling. | Deferred |
| Phase 5: Remote operations | Remote commands and OTA. | Requires validated device support and safety controls. | Deferred |

**Proposed first-release exclusions**
- Rule-generated alerts, notification escalation, remote commands, firmware OTA, automated workflows, and AI analysis.

Phases describe capability dependencies and can be combined into a release batch; a roadmap phase is not itself a delivery commitment.

## Solution Design

### Functional requirements

The requirements follow the proposed phase order in Scope. Phase order does not establish release dates or commitments.

### Phase 1: Device access and visibility

| ID | Requirement | Priority | Related goal / story | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- |
| FR-1 | An authorized operator can register a device under a tenant and inspect its connection status. | Must (proposed) | G-1 / US-1 | Stakeholder input | Proposed; workflow open |
| FR-2 | The platform provides an MQTT Broker and authenticates connecting devices with key-based credentials. | Must (proposed) | US-1 | Stakeholder input | Proposed; details open |
| FR-3 | An authorized operator can view telemetry reported by an accessible device. | Must (proposed) | US-2 | Stakeholder input | Proposed; history and retention open |
| FR-4 | Operators can securely provision and revoke device credentials. | Should (proposed) | US-1 | Product proposal | Proposed; first-release scope open |

### Phase 2: Alert handling

| ID | Requirement | Priority | Related goal / story | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- |
| FR-5 | An authorized operator can confirm, assign, and close an alert reported by a device. | Must (proposed) | G-2 / US-3 | Stakeholder input | Proposed; lifecycle open |

### Phase 3: Scale and governance (candidate)

| ID | Requirement | Priority | Related goal / story | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- |
| FR-6 | Operators can rotate device credentials and revoke compromised credentials. | Should (proposed) | G-1 / US-1 | Product proposal | Proposed; validate the credential recovery workflow |
| FR-7 | Operators can bulk-register devices and inspect failed records. | Could (proposed) | G-1 / US-1 | Product proposal | Proposed; depends on validated onboarding fields |

### Phase 4: Alert automation (deferred)

| ID | Requirement | Priority | Related goal / story | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- |
| FR-8 | Authorized users can define telemetry rules that create alerts. | Could (proposed) | G-2 / US-3 | Product proposal | Deferred pending validation of rule value and data model |
| FR-9 | Authorized users can configure notification retries or escalation and inspect delivery outcomes. | Could (proposed) | G-2 / US-3 | Product proposal | Deferred; outside the first delivery |

### Phase 5: Remote operations (deferred)

| ID | Requirement | Priority | Related goal / story | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- |
| FR-10 | Authorized operators can issue controlled remote commands and inspect execution results. | Could (proposed) | G-1 / US-1 | Product proposal | Deferred pending device and safety validation |
| FR-11 | Operators can manage firmware and run staged OTA updates with failure reporting. | Could (proposed) | G-1 / US-1 | Product proposal | Deferred pending integrity, compatibility, and recovery validation |

### User experience

- **Device onboarding:** Operator registers a device, the device connects to the platform Broker with its credential, and the operator sees connection status.
- **Alert handling:** Device reports an alert; an authorized operator confirms awareness, assigns responsibility, and closes the alert when handling is complete.

### Edge cases

| Scenario | Expected product behavior | Related requirement |
| --- | --- | --- |
| A device credential is revoked. | The device can no longer authenticate; recovery and reprovisioning policy remains to be decided. | FR-2 / FR-4 / FR-6 |
| A device repeats an alert report. | Deduplication and repeated-report behavior remain an open product decision. | FR-5 |

## Technical Considerations

### Constraints and non-functional requirements

| ID | Quality area | Requirement / measure | Status / source |
| --- | --- | --- | --- |
| NFR-1 | Tenant isolation | Device, telemetry, credential, and alert access is scoped to an authorized tenant. | Proposed product constraint |
| NFR-2 | Broker availability and scale | Capacity, availability, and recovery targets are TBD pending pilot scale. | Open |

### Integration points

- **Integration:** Platform-provided MQTT Broker; specific protocol version and message contract are pending design.

### Data requirements

- **Data needs:** Latest connection information and device telemetry; retention, history range, and export needs are TBD.

## Dependencies & Risks

### Dependencies

| Dependency | Owner | Status | Impact if delayed |
| --- | --- | --- | --- |
| No external dependency confirmed | N/A | None identified | No known schedule impact; revisit after delivery batch selection |

### Risks and assumptions

| Item | Likelihood / impact | Mitigation or validation |
| --- | --- | --- |
| Device key lifecycle is undefined. | TBD / High | Decide provisioning and revocation before implementation / Product and Security |
| Pilot device and message volume are unknown. | TBD / TBD | Define a representative pilot / Product and Operations |
| Tenant operators may need self-service operations. | TBD / TBD | Confirm target user and duties / Product |

## Open Questions

| Question | Affected requirement(s) | Impact | Owner / needed before |
| --- | --- | --- | --- |
| Who registers devices and manages them within each tenant? | FR-1, FR-3, FR-5 | Defines roles and onboarding workflow. | Product / scope approval |
| What are the credential format, provisioning, rotation, and revocation rules? | FR-2, FR-4, FR-6 | Defines device authentication and recovery. | Product and Security / design |
| What MQTT version, TLS policy, topic structure, QoS, and payload contract are required? | FR-2, FR-3, FR-5 | Defines device integration behavior. | Product and Engineering / design |
| What determines offline status and telemetry freshness/retention? | FR-1, FR-3 | Defines monitoring expectations and data costs. | Product / before spec |
| How do confirm, assign, close, repeat reports, and reopening interact? | FR-5 | Defines alert workflow and acceptance boundaries. | Product / before spec |

## Appendix

### Related documents

- User research / inputs: stakeholder request; no separate research artifact
- Domain design / API: To be established after scope approval
- Feature specs: To be created after PRD approval

### Supporting material (optional)

- None supplied.

### Revision history

| Version | Date | Change | Approval |
| --- | --- | --- | --- |
| 0.1 | 2026-10-20 | Initial draft based on stakeholder input. | Pending |
