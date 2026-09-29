# hagukumi (育み) — Care Tier-B Actor

**DID**: `did:web:etzhayyim.com:hagukumi`
**Namespace**: `com.etzhayyim.hagukumi.*`
**ADR**: ADR-2605261030 (R0 scaffold)
**Status**: R0 scaffold (2026-05-26) — all cells import-time RuntimeError
**Parent ADR**: ADR-2605261000 (Liberation Ladder — L4 Care Tier gate)

## Overview

Daily-living care actor for the Liberation Ladder L4 Care Tier — childcare (ages 2+) + eldercare + chronic-care continuity + meal delivery + respite support.

**R0 scope** complements mitate (diagnosis) + yakushi (drugs); hagukumi delivers the *daily care labor* that consumes the largest share of OECD subsistence time. Excludes medical procedures, hospice palliative, behavioral psych, surveillance, in-home recording, replacement of legal guardian, abuse investigation.

## Privacy Invariant (CRITICAL — constitutional)

Care substrate handles the most sensitive observations in the religious-corp ecosystem. **ADR-2605181100 XChaCha20 envelope is mandatory** for all `careSessionAttestation` records. Schema-level enforcement forbids plaintext content fields. Live video for telepresence permitted; **recording prohibited firmware-level (G2)**.

## Robotics Classes

| Class | Role | Lineage | Notes |
|---|---|---|---|
| Hitogata-A (R2+) | gentle humanoid | kuni-umi class-A | gentle subset only; G9 human-in-loop |
| Sukoyaka | cold-chain last-mile | yakushi inheritance | meal delivery |
| Yutori (ゆとり) (R2+) | companionship telepresence | new class | separate mech-design ADR (hanami precedent) |

## Pregel Cells (5, R0)

All cells import-time RuntimeError (privacy invariant — no plaintext data flow until R1 encrypted-record framework Council-attested production-ready).

| Cell | Murakumo node | Phase | Input → Output |
|---|---|---|---|
| `child_daily_care` | levi | child session | careRecipientDid, schedule → careSessionAttestation (encrypted) |
| `elder_companionship` | levi | elder session | careRecipientDid, schedule → careSessionAttestation (encrypted) |
| `chronic_continuity` | levi (mitate-paired) | post-diagnosis support | mitateReferralCid → continuitySessionAttestation (encrypted) |
| `meal_delivery` | simeon + dan | mitsuho-sourced delivery | mealManifest, route → deliveryAttestation (aggregate; no recipient PII) |
| `respite_support` | levi | caregiver substitute | primaryCaregiverDid, window → respiteSessionAttestation (encrypted) |

## Constitutional Gates (G1–G14)

See ADR-2605261030. **IMMUTABLE** per R0.

Key gates:
- **G2**: No video recording (firmware-level enforcement)
- **G3**: Per-session consent required (default-deny)
- **G4**: Caregiver Council vetting + child/elder-protection certification + ≥3 Council Lv6+ attestations
- **G5**: Child cognitive load cap (≤2 hr structured/session for under-6)
- **G6**: Elder autonomy invariant (no override except immediate safety)
- **G7**: No behavioral modification protocols
- **G9**: Human-in-loop required; no AI-only care
- **G10**: Caregiver 12-hr work cap + 12-hr recovery
- **G11**: Emergency escalation to mitate (G5 keyword fail-safe)
- **G13**: No addictive UX (no gamification/streaks/variable reward)
- **G14**: Multi-generational priority (≥40% under-18 + ≥30% over-65 + ≤30% adults)

## Non-Goals (N1–N10)

- N1: No medical procedures (mitate/yakushi)
- N2: No under-2 childcare (specialist actor TBD)
- N3: No hospice/palliative terminal (mitate N10 + specialist)
- N4: No behavioral psych intervention
- N5: No telemedicine (mitate)
- N6: No pharmaceutical dispensing (yakushi)
- N7: No in-home surveillance
- N8: No replacement of legal guardian
- N9: No genetic counseling
- N10: No abuse investigation (state mandatory-report only)

## Roadmap

| Phase | Timeline | Scope | L-gate |
|---|---|---|---|
| **R0** | 2026-05-26 | Scaffold | — |
| **R1** | post-Council | Caregiver onboarding + consent infra + chronic-continuity prompts (no in-home presence). ≤50 pairs. | future ADR |
| **R2** | post-R1 | Live care 5 community centers + ≤200 ceiling. Hitogata/Yutori R&D parallel. | **L4 eligibility** |
| **R3** | post-R2 | Community-scale 50 sites + ≤25,000 ceiling. Full mesh. | **L4→L5 required** |

## Lexicons (4, deferred to R1+)

```
com.etzhayyim.hagukumi.{
  caregiverAttestation
  careSessionAttestation       # encrypted XChaCha20 envelope MANDATORY
  consentRecord                # revocable, on-chain
  silenCareReview
}
```

## Integration

- **Cross-actor pair**: mitate (diagnosis ↔ chronic continuity ↔ emergency escalation)
- **Drug supply**: yakushi (chronic-continuity medication adherence)
- **Food supply**: mitsuho (meal delivery)
- **Energy**: hikari (R2+ site power)

## References

- `/90-docs/adr/2605261000-labor-liberation-transition-mechanism.md` — L4 gate
- `/90-docs/adr/2605261030-hagukumi-care-tier-b-actor-r0.md` — Master ADR
- `/90-docs/adr/2605181100-mst-encrypted-records-signal-keywrap.md` — privacy envelope
- `/90-docs/adr/2605260100-mitate-diagnostic-routing-charter.md` — cross-actor sibling
- `/AGENTS.md` — Religious-corp status table
