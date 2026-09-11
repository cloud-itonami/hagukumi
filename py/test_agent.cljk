#!/usr/bin/env bb
;; Clojure port of py/test_agent.py (hagukumi care actor — 23 tests).
(ns hagukumi.py.test-agent
  "hagukumi 育み test harness. Verifies constitutional gates of ADR-2605261030:
    G1/G3  consent verification (per-session window)
    G4     caregiver on-chain vetting
    G5     emergency escalation to mitate (keyword detection)
    G10    caregiver work cap (12h cumulative / 12h recovery)
    G15    PII encrypted-payload gate
    G16    pseudonym rotation (30-day threshold)
    G17    guardian consent for <14 care recipients
    G13/G14 USDC + TitheRouter 10% settlement, stops at :intent"
  (:require [clojure.test :refer [deftest is run-tests]]
            [hagukumi.py.agent :as agent]))

;; ── G1/G3 consent window ──────────────────────────────────────────────────────
(deftest test-consent-window-valid
  (is (= true (:ok (agent/verify_consent_window
                    "2026-05-20T10:00:00Z"
                    "2026-05-20T08:00:00Z"
                    "2026-05-20T12:00:00Z")))))

(deftest test-consent-window-outside
  (is (= false (:ok (agent/verify_consent_window
                     "2026-05-20T15:00:00Z"
                     "2026-05-20T08:00:00Z"
                     "2026-05-20T12:00:00Z")))))

;; ── G4 caregiver vetting ──────────────────────────────────────────────────────
(deftest test-caregiver-attested-ok
  (let [registry {"did:web:hagukumi.etzhayyim.com:caregiver:alice"
                  {"councilApprovalDate" "2026-05-15"}}]
    (is (= true (:ok (agent/verify_caregiver_attested
                      "did:web:hagukumi.etzhayyim.com:caregiver:alice"
                      registry))))))

(deftest test-caregiver-not-in-registry
  (let [registry {}]
    (is (= false (:ok (agent/verify_caregiver_attested
                       "did:web:hagukumi.etzhayyim.com:caregiver:unknown"
                       registry))))))

(deftest test-caregiver-no-council-approval
  (let [registry {"did:web:hagukumi.etzhayyim.com:caregiver:bob" {}}]
    (is (= false (:ok (agent/verify_caregiver_attested
                       "did:web:hagukumi.etzhayyim.com:caregiver:bob"
                       registry))))))

;; ── G5 emergency escalation ───────────────────────────────────────────────────
(deftest test-emergency-escalation-triggered
  (is (= true (:escalate (agent/check_mitate_emergency_keywords
                          "child fever 39C breathing difficulty"
                          ["fever" "breathing"])))))

(deftest test-emergency-escalation-not-triggered
  (is (= false (:escalate (agent/check_mitate_emergency_keywords
                            "routine play and rest"
                            ["fever"])))))

;; ── G10 caregiver work cap ────────────────────────────────────────────────────
(deftest test-work-cap-under-limit
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 8.0 "hours_since_last_session" 24.0}}]
    (is (= true (:ok (agent/check_caregiver_work_cap
                      "did:caregiver:alice" work-log 4.0))))))

(deftest test-work-cap-over-cumulative
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 10.0 "hours_since_last_session" 24.0}}]
    (is (= false (:ok (agent/check_caregiver_work_cap
                       "did:caregiver:alice" work-log 4.0))))))

(deftest test-work-cap-insufficient-recovery
  (let [work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 4.0 "hours_since_last_session" 6.0}}]
    (is (= false (:ok (agent/check_caregiver_work_cap
                       "did:caregiver:alice" work-log 2.0))))))

;; ── G15 encrypted payload ─────────────────────────────────────────────────────
(deftest test-encrypted-payload-valid
  (is (= true (:ok (agent/verify_encrypted_payload "ipfs://QmXChaCha20Encrypted")))))

(deftest test-encrypted-payload-missing
  (is (= false (:ok (agent/verify_encrypted_payload "")))))

;; ── G16 pseudonym rotation ────────────────────────────────────────────────────
(deftest test-pseudonym-rotation-needed
  (let [result (agent/rotate_pseudonym_did
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-04"
                31)]
    (is (= true (:rotate result)))))

(deftest test-pseudonym-rotation-not-needed
  (let [result (agent/rotate_pseudonym_did
                "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                15)]
    (is (= false (:rotate result)))))

;; ── G17 guardian consent for <14 ─────────────────────────────────────────────
(deftest test-guardian-consent-child-ok
  (is (= true (:ok (agent/verify_guardian_consent_for_child 8 "did:web:parent:bob")))))

(deftest test-guardian-consent-child-missing
  (is (= false (:ok (agent/verify_guardian_consent_for_child 8 "")))))

(deftest test-guardian-consent-adult-not-required
  (is (= true (:ok (agent/verify_guardian_consent_for_child 18 "")))))

;; ── record_care_session integration ──────────────────────────────────────────
(deftest test-care-session-all-gates-pass
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        work-log {"did:caregiver:alice"
                  {"cumulative_hours_24h" 4.0 "hours_since_last_session" 24.0}}
        result   (agent/record_care_session
                  "test-001"
                  "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                  "did:caregiver:alice"
                  "2026-05-20T08:00:00Z"
                  "2026-05-20T12:00:00Z"
                  "ipfs://QmXChaCha20PolyEncrypted"
                  "ipfs://QmConsentRecord0001"
                  "child-daily-care"
                  :caregiver-registry  registry
                  :consent-timestamp-iso "2026-05-20T10:00:00Z"
                  :emergency-keywords  ["fever" "injury"]
                  :session-description "routine play"
                  :work-log            work-log
                  :recipient-age       8
                  :guardian-did        "did:web:parent:bob")]
    (is (some? (get result ":careSessionAttestation/id")))))

(deftest test-care-session-blocked-no-encryption
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        result   (agent/record_care_session
                  "test-002"
                  "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                  "did:caregiver:alice"
                  "2026-05-20T08:00:00Z"
                  "2026-05-20T12:00:00Z"
                  ""
                  "ipfs://QmConsentRecord0001"
                  "child-daily-care"
                  :caregiver-registry registry
                  :recipient-age      8
                  :guardian-did       "did:web:parent:bob")]
    (is (= true (:blocked result)))))

(deftest test-care-session-blocked-unauthenticated-caregiver
  (let [registry {}
        result   (agent/record_care_session
                  "test-003"
                  "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                  "did:caregiver:unknown"
                  "2026-05-20T08:00:00Z"
                  "2026-05-20T12:00:00Z"
                  "ipfs://QmXChaCha20PolyEncrypted"
                  "ipfs://QmConsentRecord0001"
                  "child-daily-care"
                  :caregiver-registry registry)]
    (is (= true (:blocked result)))))

(deftest test-care-session-blocked-child-no-guardian
  (let [registry {"did:caregiver:alice" {"councilApprovalDate" "2026-05-15"}}
        result   (agent/record_care_session
                  "test-004"
                  "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:2026-05"
                  "did:caregiver:alice"
                  "2026-05-20T08:00:00Z"
                  "2026-05-20T12:00:00Z"
                  "ipfs://QmXChaCha20PolyEncrypted"
                  "ipfs://QmConsentRecord0001"
                  "child-daily-care"
                  :caregiver-registry registry
                  :recipient-age      10
                  :guardian-did       "")]
    (is (= true (:blocked result)))))

;; ── G13/G14 settlement ────────────────────────────────────────────────────────
(deftest test-settlement-tithe-split
  ;; 10% tithe + stops at intent (G13/G14)
  (let [s (agent/build_settlement_intent 10000000)]
    (is (= 1000000 (:titheMinor s)))
    (is (= "intent" (:state s)))
    (is (= "usdc-base-l2" (:rail s)))))

(deftest test-settlement-executed-with-sig
  ;; settlement executes only with caregiver signature (G14)
  ;; NOTE: agent.py build_settlement_intent returns state "executed" when caregiver_sig_ref
  ;; is provided — this is hagukumi R0 behaviour. We port to the ACTUAL impl behaviour.
  (let [s (agent/build_settlement_intent 5000000 "0xsig")]
    (is (= "executed" (:state s)))))

;; ── runner ────────────────────────────────────────────────────────────────────
(when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (clojure.test/run-tests 'hagukumi.py.test-agent)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
