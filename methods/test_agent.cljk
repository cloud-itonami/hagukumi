(ns hagukumi.methods.test-agent
  "hagukumi 育み — agent gate tests (offline, no kotoba host, no network, no LLM).
  ADR-2605261030. 1:1 port of py/test_agent.py — expected values copied verbatim."
  (:require [clojure.test :refer [deftest is]]
            [hagukumi.methods.agent :as agent]))

;; ── G1/G3 — consent window ────────────────────────────────────────────────────

(deftest test-consent-window-valid
  ;; consent within session window accepted (G1/G3)
  (is (true? (:ok (agent/verify-consent-window
                   "2026-05-20T10:00:00Z"
                   "2026-05-20T08:00:00Z"
                   "2026-05-20T12:00:00Z")))))

(deftest test-consent-window-outside
  ;; consent outside session window rejected (G1/G3)
  (is (false? (:ok (agent/verify-consent-window
                    "2026-05-20T15:00:00Z"
                    "2026-05-20T08:00:00Z"
                    "2026-05-20T12:00:00Z")))))

;; ── G4 — caregiver vetting ────────────────────────────────────────────────────

(deftest test-caregiver-attested-ok
  ;; Council-attested caregiver accepted (G4)
  (let [registry {"did:web:hagukumi.etzhayyim.com:caregiver:alice"
                  {"councilApprovalDate" "2026-05-15"}}]
    (is (true? (:ok (agent/verify-caregiver-attested
                     "did:web:hagukumi.etzhayyim.com:caregiver:alice"
                     registry))))))

(deftest test-caregiver-not-in-registry
  ;; caregiver not in registry rejected (G4)
  (is (false? (:ok (agent/verify-caregiver-attested
                    "did:web:hagukumi.etzhayyim.com:caregiver:unknown"
                    {})))))

(deftest test-caregiver-no-council-approval
  ;; caregiver without Council approval rejected (G4)
  (let [registry {"did:web:hagukumi.etzhayyim.com:caregiver:bob" {}}]
    (is (false? (:ok (agent/verify-caregiver-attested
                      "did:web:hagukumi.etzhayyim.com:caregiver:bob"
                      registry))))))

;; ── G5 — emergency escalation ─────────────────────────────────────────────────

(deftest test-emergency-escalation-triggered
  ;; emergency keyword triggers mitate escalation (G5)
  (is (true? (:escalate (agent/check-mitate-emergency-keywords
                          "child fever 39C breathing difficulty"
                          ["fever" "breathing"])))))

(deftest test-emergency-escalation-not-triggered
  ;; normal session no escalation (G5)
  (is (false? (:escalate (agent/check-mitate-emergency-keywords
                           "routine play and rest"
                           ["fever"])))))

;; ── G10 — caregiver work cap ──────────────────────────────────────────────────

(deftest test-work-cap-under-limit
  ;; caregiver within work cap accepted (G10)
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 8.0 "hours_since_last_session" 24.0}}]
    (is (true? (:ok (agent/check-caregiver-work-cap "did:caregiver:alice" work-log 4.0))))))

(deftest test-work-cap-over-cumulative
  ;; caregiver over 12h cumulative rejected (G10)
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 10.0 "hours_since_last_session" 24.0}}]
    (is (false? (:ok (agent/check-caregiver-work-cap "did:caregiver:alice" work-log 4.0))))))

(deftest test-work-cap-insufficient-recovery
  ;; caregiver insufficient recovery rejected (G10)
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 4.0 "hours_since_last_session" 6.0}}]
    (is (false? (:ok (agent/check-caregiver-work-cap "did:caregiver:alice" work-log 2.0))))))

;; ── G15 — encrypted payload ───────────────────────────────────────────────────

(deftest test-encrypted-payload-valid
  ;; valid encrypted payload CID accepted (G15)
  (is (true? (:ok (agent/verify-encrypted-payload "ipfs://QmXChaCha20Encrypted")))))

(deftest test-encrypted-payload-missing
  ;; missing encrypted payload rejected (G15)
  (is (false? (:ok (agent/verify-encrypted-payload "")))))

;; ── G16 — pseudonym rotation ──────────────────────────────────────────────────

(deftest test-pseudonym-rotation-needed
  ;; pseudonym rotation at >30 days (G16)
  (let [result (agent/rotate-pseudonym-did
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-04" 31)]
    (is (true? (:rotate result)))))

(deftest test-pseudonym-rotation-not-needed
  ;; pseudonym rotation not needed at 15 days (G16)
  (let [result (agent/rotate-pseudonym-did
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05" 15)]
    (is (false? (:rotate result)))))

;; ── G17 — guardian consent ────────────────────────────────────────────────────

(deftest test-guardian-consent-child-ok
  ;; child <14 with guardian consent accepted (G17)
  (is (true? (:ok (agent/verify-guardian-consent-for-child 8 "did:web:parent:bob")))))

(deftest test-guardian-consent-child-missing
  ;; child <14 without guardian consent rejected (G17)
  (is (false? (:ok (agent/verify-guardian-consent-for-child 8 "")))))

(deftest test-guardian-consent-adult-not-required
  ;; adult >=14 guardian consent not required (G17)
  (is (true? (:ok (agent/verify-guardian-consent-for-child 18 "")))))

;; ── care session orchestration ────────────────────────────────────────────────

(deftest test-care-session-all-gates-pass
  ;; care session with all gates pass
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 4.0 "hours_since_last_session" 24.0}}
        result (agent/record-care-session
                "test-001"
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                "did:caregiver:alice"
                "2026-05-20T08:00:00Z"
                "2026-05-20T12:00:00Z"
                "ipfs://QmXChaCha20PolyEncrypted"
                "ipfs://QmConsentRecord0001"
                "child-daily-care"
                :caregiver-registry registry
                :consent-timestamp-iso "2026-05-20T10:00:00Z"
                :emergency-keywords ["fever" "injury"]
                :session-description "routine play"
                :work-log work-log
                :recipient-age 8
                :guardian-did "did:web:parent:bob")]
    (is (some? (get result ":careSessionAttestation/id")))))

(deftest test-care-session-blocked-no-encryption
  ;; care session blocked without encryption (G15)
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        result (agent/record-care-session
                "test-002"
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                "did:caregiver:alice"
                "2026-05-20T08:00:00Z"
                "2026-05-20T12:00:00Z"
                ""
                "ipfs://QmConsentRecord0001"
                "child-daily-care"
                :caregiver-registry registry
                :recipient-age 8
                :guardian-did "did:web:parent:bob")]
    (is (true? (:blocked result)))))

(deftest test-care-session-blocked-unauthenticated-caregiver
  ;; care session blocked without caregiver attestation (G4)
  (let [result (agent/record-care-session
                "test-003"
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                "did:caregiver:unknown"
                "2026-05-20T08:00:00Z"
                "2026-05-20T12:00:00Z"
                "ipfs://QmXChaCha20PolyEncrypted"
                "ipfs://QmConsentRecord0001"
                "child-daily-care"
                :caregiver-registry {})]
    (is (true? (:blocked result)))))

(deftest test-care-session-blocked-child-no-guardian
  ;; care session blocked child <14 without guardian (G17)
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        result (agent/record-care-session
                "test-004"
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                "did:caregiver:alice"
                "2026-05-20T08:00:00Z"
                "2026-05-20T12:00:00Z"
                "ipfs://QmXChaCha20PolyEncrypted"
                "ipfs://QmConsentRecord0001"
                "child-daily-care"
                :caregiver-registry registry
                :recipient-age 10
                :guardian-did "")]
    (is (true? (:blocked result)))))

;; ── G13/G14 — settlement / tithe ─────────────────────────────────────────────

(deftest test-settlement-tithe-split
  ;; 10% tithe + stops at intent (G13/G14)
  (let [s (agent/build-settlement-intent 10000000)]
    (is (= 1000000 (get s "titheMinor")))
    (is (= "intent" (get s "state")))
    (is (= "usdc-base-l2" (get s "rail")))))

(deftest test-settlement-executed-with-sig
  ;; settlement executes only with caregiver signature (G14)
  (let [s (agent/build-settlement-intent 5000000 "0xsig")]
    (is (= "executed" (get s "state")))))
