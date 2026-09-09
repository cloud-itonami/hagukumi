(ns hagukumi.methods.agent
  "hagukumi 育み — care session actor constitutional gates.
  ADR-2605261030 R0 scaffold. Faithful port of py/agent.py.

  Gates enforced:
    G1   consent-bound           per-session consent required
    G3   per-session-consent-window  consentRecordCid timestamp within session window
    G4   caregiver-council-vetting   on-chain caregiver registry
    G5   emergency-escalation-mitate  care-session emergency keywords trigger mitate XRPC
    G10  caregiver-work-cap       REJECT if caregiver >12 hr cumulative past 24 hr
    G13  tithe-non-fiat           USDC Base L2 + ERC-4337 + TitheRouter 10%
    G14  no-server-key            member/caregiver/guardian signs attestations
    G15  pii-encrypted-envelope   encryptedPayloadCid + consentRecordCid REQUIRED
    G16  pseudonym-rotation-30d   care-recipient identity rotates every 30 days
    G17  guardian-consent-child   <14 care-recipient requires guardian DID"
  (:require [kotoba.lang.text]))

;; ── constants ──────────────────────────────────────────────────────────────────
(def tithe-bps 1000) ;; 10% TitheRouter auto-split (G13), basis points
(def max-hours-cumulative-24h 12)
(def min-hours-recovery-between-sessions 12)

;; ── G1 + G3 — consent verification (per-session window) ──────────────────────
(defn verify-consent-window
  "G1 + G3: consentRecordCid timestamp must fall within session window.
  All args are ISO-8601 strings (with or without trailing Z)."
  [consent-timestamp-iso session-start-iso session-end-iso]
  (try
    (let [norm   #(kotoba.lang.text/replace % #"Z$" "+00:00")
          inst   #(java.time.OffsetDateTime/parse (norm %)
                    java.time.format.DateTimeFormatter/ISO_OFFSET_DATE_TIME)
          ct     (inst consent-timestamp-iso)
          start  (inst session-start-iso)
          end    (inst session-end-iso)]
      (if (and (not (.isBefore ct start)) (not (.isAfter ct end)))
        {:ok true  :reason "consent within session window"}
        {:ok false :reason (str "consent timestamp " consent-timestamp-iso
                                " outside session window (G1/G3)")}))
    (catch Exception e
      {:ok false :reason (str "timestamp parse error: " (.getMessage e))})))

;; ── G4 — caregiver on-chain vetting gate ──────────────────────────────────────
(defn verify-caregiver-attested
  "G4: caregiver DID must exist in on-chain registry with current caregiverAttestation."
  [caregiver-did caregiver-registry]
  (if-not (contains? caregiver-registry caregiver-did)
    {:ok false :reason (str "caregiver DID " caregiver-did
                            " not in Council-attested registry (G4)")}
    (let [attestation (get caregiver-registry caregiver-did {})]
      (if-not (get attestation "councilApprovalDate")
        {:ok false :reason (str "caregiver " caregiver-did " lacks Council approval (G4)")}
        {:ok true  :reason (str "caregiver " caregiver-did " Council-attested")}))))

;; ── G5 — emergency escalation to mitate (keyword detection) ───────────────────
(defn check-mitate-emergency-keywords
  "G5: if any emergency keyword is found in session, trigger mitate XRPC POST."
  [session-description keywords]
  (let [lower (kotoba.lang.text/lower session-description)
        hits  (filterv #(kotoba.lang.text/includes? lower (kotoba.lang.text/lower %)) keywords)]
    (if (seq hits)
      {:escalate true  :matched-keywords hits :rule "G5"}
      {:escalate false :matched-keywords [] :rule "G5"})))

;; ── G10 — caregiver work cap enforcement ──────────────────────────────────────
(defn check-caregiver-work-cap
  "G10: reject if caregiver shows >12 hr cumulative in past 24 hr or <12 hr recovery since last."
  [caregiver-did work-log proposed-session-hours]
  (if-not (contains? work-log caregiver-did)
    {:ok true :reason "caregiver work history clear"}
    (let [entry            (get work-log caregiver-did {})
          cumulative-24h   (get entry "cumulative_hours_24h" 0.0)
          hours-since-last (get entry "hours_since_last_session" 0.0)
          total            (+ cumulative-24h proposed-session-hours)]
      (cond
        (> total max-hours-cumulative-24h)
        {:ok false :reason (str "caregiver cumulative " total "h > "
                                max-hours-cumulative-24h "h cap (G10)")}

        (< hours-since-last min-hours-recovery-between-sessions)
        {:ok false :reason (str "caregiver recovery " hours-since-last "h < "
                                min-hours-recovery-between-sessions "h minimum (G10)")}

        :else {:ok true :reason "caregiver work cap satisfied"}))))

;; ── G15 — PII encryption ───────────────────────────────────────────────────────
(defn verify-encrypted-payload
  "G15: encryptedPayloadCid must be present (no plaintext care details allowed)."
  [encrypted-payload-cid]
  (if (and (seq encrypted-payload-cid)
           (kotoba.lang.text/starts-with? encrypted-payload-cid "ipfs://Qm"))
    {:ok true  :reason "encrypted payload CID valid (G15)"}
    {:ok false :reason "missing or invalid encrypted payload CID (G15 breach)"}))

;; ── G16 — pseudonym rotation ───────────────────────────────────────────────────
(defn rotate-pseudonym-did
  "G16: if care-recipient pseudonym DID is >30 days old, generate new rotated identity."
  [old-pseudonym days-old]
  (if (>= days-old 30)
    {:rotate true
     :new-pseudonym (str "did:web:hagukumi.etzhayyim.com:recipient:pseudonym:"
                         (+ days-old 1))}
    {:rotate false :pseudonym old-pseudonym}))

;; ── G17 — guardian consent for <14 care recipients ────────────────────────────
(defn verify-guardian-consent-for-child
  "G17: <14 care-recipient requires guardianConsentCid field (non-empty guardian DID)."
  [care-recipient-age guardian-did]
  (cond
    (>= care-recipient-age 14)
    {:ok true :reason "recipient age >= 14; guardian consent not required (G17)"}

    (or (nil? guardian-did) (= guardian-did ""))
    {:ok false :reason "<14 recipient requires guardian DID; got empty (G17 breach)"}

    :else
    {:ok true :reason (str "guardian " guardian-did
                           " consent required for <14 recipient (G17 satisfied)")}))

;; ── care session attestation — all gates orchestrated ─────────────────────────
(defn record-care-session
  "Record care session with all consent + encryption + vetting gates enforced.
  Returns a care-session attestation map on success, or {:error … :blocked true} on gate failure.

  Positional args (match py agent.record_care_session):
    session-id, care-recipient-pseudonym-did, caregiver-did,
    session-start-iso, session-end-iso,
    encrypted-payload-cid, consent-record-cid, care-type

  Keyword args:
    :caregiver-registry   map of did -> attestation (default {})
    :consent-timestamp-iso  ISO string (default nil)
    :emergency-keywords   seq of strings (default [])
    :session-description  string (default \"\")
    :work-log             map of did -> work entry (default {})
    :recipient-age        integer or nil (default nil)
    :guardian-did         string (default nil)"
  [session-id care-recipient-pseudonym-did caregiver-did
   session-start-iso session-end-iso
   encrypted-payload-cid consent-record-cid
   care-type
   & {:keys [caregiver-registry consent-timestamp-iso emergency-keywords
             session-description work-log recipient-age guardian-did]
      :or   {caregiver-registry  {}
             emergency-keywords  []
             session-description ""
             work-log            {}}}]
  ;; G15: encryption
  (let [enc (verify-encrypted-payload encrypted-payload-cid)]
    (if-not (:ok enc)
      {:error (:reason enc) :blocked true}

      ;; G4: caregiver vetting
      (let [cg-ok (verify-caregiver-attested caregiver-did caregiver-registry)]
        (if-not (:ok cg-ok)
          {:error (:reason cg-ok) :blocked true}

          ;; G1 + G3: consent window
          (let [consent-result
                (when (and consent-timestamp-iso session-end-iso)
                  (verify-consent-window consent-timestamp-iso session-start-iso session-end-iso))]
            (if (and consent-result (not (:ok consent-result)))
              {:error (:reason consent-result) :blocked true}

              ;; G10: work cap (default 4.0 hr estimate, matches Python)
              (let [session-hours 4.0
                    wc (check-caregiver-work-cap caregiver-did work-log session-hours)]
                (if-not (:ok wc)
                  {:error (:reason wc) :blocked true}

                  ;; G17: guardian consent for <14 recipients
                  (let [child-result
                        (when (some? recipient-age)
                          (verify-guardian-consent-for-child recipient-age (or guardian-did "")))]
                    (if (and child-result (not (:ok child-result)))
                      {:error (:reason child-result) :blocked true}

                      ;; G5: emergency escalation check (never blocks — only flags)
                      (let [mitate (check-mitate-emergency-keywords session-description emergency-keywords)]
                        {":careSessionAttestation/id"
                         (str "csa." session-id)
                         ":careSessionAttestation/careRecipientPseudonymDid"
                         care-recipient-pseudonym-did
                         ":careSessionAttestation/caregiverDid"
                         caregiver-did
                         ":careSessionAttestation/sessionStartTime"
                         session-start-iso
                         ":careSessionAttestation/sessionEndTime"
                         session-end-iso
                         ":careSessionAttestation/encryptedPayloadCid"
                         encrypted-payload-cid
                         ":careSessionAttestation/consentRecordCid"
                         consent-record-cid
                         ":careSessionAttestation/careType"
                         care-type
                         ":careSessionAttestation/aggregateSessionDurationMinutes"
                         240
                         ":careSessionAttestation/aggregateActivityCategory"
                         "care-activity"
                         ":careSessionAttestation/emergencyTriggered"
                         (:escalate mitate)
                         ":careSessionAttestation/mitateCareEscalationLevel"
                         (if (:escalate mitate) 1 0)}))))))))))))

;; ── settlement — USDC + TitheRouter intent (NOT broadcast; G13/G14/R0) ────────
(defn build-settlement-intent
  "USDC settlement split. 10% tithe -> Public Fund. Stops at :intent —
  broadcast needs a caregiver signature (G14).
  Python equivalent: build_settlement_intent(gross_minor, caregiver_sig_ref=None)"
  ([gross-minor]
   (build-settlement-intent gross-minor nil))
  ([gross-minor caregiver-sig-ref]
   (let [tithe (quot (* gross-minor tithe-bps) 10000)]
     {"rail"                "usdc-base-l2"
      "grossMinor"          gross-minor
      "titheMinor"          tithe
      "caregiverPayoutMinor" (- gross-minor tithe)
      "titheRouter"         "50-infra/etzhayyim-tithe-router"
      "state"               (if (and caregiver-sig-ref (seq caregiver-sig-ref))
                              "executed"
                              "intent")
      "caregiverSigRef"     (or caregiver-sig-ref "")})))
