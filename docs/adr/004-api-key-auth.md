# ADR 004: API Key Authentication for Solo v1

## Status

Accepted

## Context

Stella is a **solo personal** app with one user and one phone. Full OAuth/JWT adds complexity without benefit for v1.

## Decision

- All `/api/v1/*` routes require header `X-Api-Key`
- Key stored in server `API_KEY` environment variable
- Android stores key in **EncryptedSharedPreferences** (entered via Settings)

## Consequences

**Positive**

- Minimal auth code on client and server
- Easy to test with curl

**Negative**

- Key compromise grants full API access — rotate key if leaked
- Not suitable for multi-user without redesign

## Upgrade path

- Add JWT issued after login when multi-user is needed
- Keep API key for device-to-server automation if desired

## Alternatives considered

- **JWT from day one:** Rejected for solo scope
- **mTLS:** Rejected; operational overhead on VM
