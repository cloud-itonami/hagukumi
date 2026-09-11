# hagukumi (育み) — Maturity Ledger

`/loop` の進捗台帳。各イテレーションで **1項目** だけ成熟度を上げ、ここに記録する。
honest framing (G8): できていないことは「未」と明記する。

- Actor: `did:web:etzhayyim.com:hagukumi` · ADR-2605261030 · **R0 scaffold**
- 不変条件(全イテレーション厳守): R0 では cell 非実行(import時 RuntimeError) ·
  PII平文禁止(G6) · 録画/監視なし(G2/N7) · 人間-in-loop(G9) · Murakumo-only(G7) ·
  **eligibility/benefit 判定なし + 非provider 境界** · G8 非捏造 · G14 verified-only ·
  コミットはユーザー明示時のみ

## イテレーション記録

### iter-1 (2026-06-02)
**care-support registry の fail-closed 監査固定 + 検証ワークフロー。** `registry/programs.seed.json`(172件 / 32管轄 / 全件 `unverified-seed`)に対し `70-tools/scripts/audit/test_hagukumi_registry_seed.py`(8 invariants: parse+非空 / programId一意 / 全件 unverified-seed(G14) / accessUrl+provenance+lastVerified 非空 http(s) / >=12管轄 / careKind taxonomy / 全 notes に「no eligibility/benefit determination」+「NOT a licensed care provider」境界 / freshnessWindowDays integer)を新規作成し green。`registry/VERIFICATION.md`(G14 三層 human/Council チェックリスト; eligibility・amounts はここで判定しない=authority で確認 を foreground; per-jurisdiction official-source provenance fail-closed; honest: **0 verified**)を新規作成。

### 2026-06-17 (loop) — manifest+lexicon charter-gate test (構造ゲート pin)
新設 `methods/test_charter_gates.cljk`(**6 tests green**)で manifest G1–G14 + 4 lexicon のケアゲートを固定: G3/G2 careSession が consentRecordCid + encryptedPayloadCid 必須(per-session consent + 録画なし暗号化)/ G4 caregiverAttestation が councilVettingAttestations + trainingCertCid 必須(Council vetting)/ consentRecord が careRecipientAgeBucket{under-14-guardian-consent, 14-17-co-consent, adult-self-consent, elder-self-consent-with-capacity-attestation} + validUntil 必須 / cellName={child_daily_care, elder_companionship, chronic_continuity, respite_support} / G14 silenCareReview に cohort-ratio audit scope。`run_tests.sh` 新設。working-tree edits only。

> **2026-06-17 substrate-native migration (ADR-2606160842):** the charter-gate test above was ported Python→Clojure (`methods/test_charter_gates.py` → `methods/test_charter_gates.cljk`, ns `hagukumi.methods.test-charter-gates`, reads the lexicons via cheshire/edn) and the Python was pruned. Run via `./run_tests.sh` (now `exec bb`) or `bb run test:charter` (all 34 charter suites; 244 tests / 924 assertions green). Assertions unchanged (1:1 port).
