# audit-v9-full constitution

## Evidence before claims

No completion statement is accepted from an unverified report. Every PASS requires fresh command output, a file artifact, or an artifact-specific marker.

## Isolation

Unit verification must not depend on shared mutable state. Integration probes must use run-scoped fixtures. The dev database may only receive approved cleanup after backup and parity checks.

## Mutation safety

Every data mutation requires a run ID, backup, scoped manifest, cleanup, residue check, and parity re-assertion. Broad namespace deletes are forbidden.

## Content preservation

Missing or low-confidence content is preserved and classified; never fabricated or silently deleted.

## Design-system boundary

UI design consistency is audited separately from functional hardening. Dead CSS may be removed only when the usage gate proves no references.

## Status accounting

PARTIAL and BLOCKED remain explicit. A workaround may prove an alternate path but never closes the original defect.
