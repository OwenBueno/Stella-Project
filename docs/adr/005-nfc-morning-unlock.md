# ADR 005: NFC-Only Morning Unlock

## Status

Accepted

## Context

Morning hostage flow must force the user out of bed. NFC tag in the bathroom is the physical proof of location.

## Decision

- Production morning unlock requires scan of **enrolled NFC tag**
- Tag id stored during Settings enrollment
- **No manual skip** in production builds

## Consequences

**Positive**

- Strong physical friction; aligns with product vision
- Simple verification (id match)

**Negative**

- Lost tag blocks morning unlock until re-enrollment
- Devices without NFC cannot use app (acceptable — `uses-feature nfc required=true`)

## Mitigations

- Document spare tag in deployment/onboarding
- Settings flow to register replacement tag
- Debug builds may expose dev bypass (not shipped)

## Alternatives considered

- **NFC with manual override:** Rejected for production
- **Skip NFC in v1:** Rejected by operator
