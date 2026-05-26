# ADR 008: Finances ledger (single-entry)

## Status

Accepted — 2026-05-25

## Context

Stella needs lightweight personal cash-flow tracking (ingress, egress, debts) aligned with the offline-first Android client and MongoDB Atlas backend. Full double-entry accounting is out of scope for a solo Life OS focused on speed.

## Decision

1. **Single-entry ledger** — Each `Transaction` is one ingress or egress row. No journal entries, accounts payable/receivable sub-ledgers beyond explicit `Debt` documents.

2. **Room local truth** — Android writes to Room first with `needsSync = true`. UI reads local `Flow` queries; no network on write.

3. **MongoDB authoritative backup** — Server stores the same UUID-keyed documents. Bulk sync via `POST /sync/push` and `GET /sync/pull` (ADR 003). REST `/finances/*` endpoints support summary queries and server-side penalty creation.

4. **No userId (v1)** — Solo API key scopes all finance data implicitly (ADR 004).

5. **Penalties** — Skip penalties create egress transactions with `category = Penalty` and optional `linkedTaskId`. Stripe charging remains deferred (ADR 006); the ledger records amounts only.

## Consequences

- Fast logging UX; no accounting correctness guarantees beyond LWW sync.
- Month summaries computed by aggregating transaction `date` in client or via `GET /finances/summary`.
- Future multi-device: same LWW rules as other entities; optional conflict UI if needed.

## Related

- [data-model.md](../data-model.md) — Transaction, Debt fields
- [adr/003-offline-first-sync.md](003-offline-first-sync.md)
- [adr/006-penalties-deferred.md](006-penalties-deferred.md)
