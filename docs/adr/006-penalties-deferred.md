# ADR 006: Financial Penalties Deferred

## Status

Accepted (deferred)

## Context

[Initial.md](../Initial.md) mentions Beeminder-style Stripe charges for skipped tasks. This adds payment compliance, refund flows, and user trust considerations.

## Decision

**Do not implement** Stripe or anti-charity penalties in Phases 1–3.

Document as optional future module:

- Stripe Checkout or PaymentIntent for fixed penalty amount
- Webhook for payment confirmation
- `LifeLog` type `PENALTY_CHARGED`

## Consequences

**Positive**

- Faster delivery of core enforcement UX
- No PCI/payment scope in v1

**Negative**

- Less extreme accountability until implemented

## Alternatives considered

- **Include in Phase 3:** Rejected; scope and compliance risk
- **Never implement:** Not decided; remains optional future
