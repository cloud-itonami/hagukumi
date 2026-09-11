(ns hagukumi.methods.test-charter-gates
  "hagukumi — constitutional-gate conformance tests (manifest + central lexicons).
  Substrate-native Clojure (ADR-2606160842). 1:1 port of the pruned methods/test_charter_gates.py."
  (:require [clojure.test :refer [deftest is run-tests]]
            [clojure.set :as set]
            [cheshire.core :as json]))

(def ^:private here (.getParentFile (java.io.File. ^String *file*)))      ;; methods/
(def ^:private actor-dir (.getParentFile here))                          ;; hagukumi/
(def ^:private actor-name (.getName actor-dir))
(def ^:private root (.. actor-dir getParentFile getParentFile))          ;; 20-actors → ROOT
(def ^:private lexdir (java.io.File. root (str "00-contracts/lexicons/com/etzhayyim/" actor-name)))
(defn- manifest [] (json/parse-string (slurp (java.io.File. actor-dir "manifest.jsonld"))))
(defn- lex [name] (json/parse-string (slurp (java.io.File. lexdir (str name ".json")))))

(def ^:private cells
  #{"child_daily_care" "elder_companionship" "chronic_continuity" "respite_support"})
(def ^:private age-buckets
  #{"under-14-guardian-consent" "14-17-co-consent"
    "adult-self-consent" "elder-self-consent-with-capacity-attestation"})

(defn- collect [doc attr]
  (let [acc (atom {})]
    (letfn [(walk [x parent]
              (cond (map? x) (do (when (and (string? parent) (contains? x attr))
                                   (swap! acc assoc parent (get x attr)))
                                 (doseq [[k v] x] (walk v k)))
                    (sequential? x) (doseq [v x] (walk v parent))))]
      (walk doc nil)) @acc))
(defn- known [doc field] (some-> (get (collect doc "knownValues") field) set))
(defn- required-union [doc]
  (let [acc (atom #{})]
    (letfn [(walk [x] (cond (map? x) (do (when (sequential? (get x "required")) (swap! acc into (get x "required")))
                                         (doseq [v (vals x)] (walk v)))
                            (sequential? x) (doseq [v x] (walk v))))]
      (walk doc)) @acc))

;; ── full gate set ──
(deftest test-all-14-gates-declared
  (is (= (set (keys (get-in (manifest) ["constitutionalGates" "gates"])))
         (set (map #(str "G" %) (range 1 15))))))

;; ── G3/G2 — per-session consent + encrypted envelope (no recording) ──
(deftest test-g3-consent-g2-encrypted
  (let [req (required-union (lex "careSessionAttestation"))]
    (doseq [field ["consentRecordCid" "encryptedPayloadCid" "careRecipientDidOrPseudonym"]]
      (is (contains? req field)))))

;; ── G4 — Council-vetted caregiver (registry + training cert) ──
(deftest test-g4-caregiver-vetting
  (is (contains? (required-union (lex "careSessionAttestation")) "caregiverDid"))
  (let [cav (required-union (lex "caregiverAttestation"))]
    (doseq [field ["councilVettingAttestations" "trainingCertCid" "specializations"]]
      (is (contains? cav field)))))

;; ── consent: age-bucket discriminator (guardian for minors, capacity for elders) ──
(deftest test-consent-age-bucket
  (let [doc (lex "consentRecord")
        req (required-union doc)]
    (doseq [field ["careRecipientAgeBucket" "validUntil" "consentTimestamp"]]
      (is (contains? req field)))
    (is (= age-buckets (known doc "careRecipientAgeBucket")))))

;; ── care cell scope is bounded to child/elder care ──
(deftest test-care-cell-scope
  (is (= cells (known (lex "careSessionAttestation") "cellName"))))

;; ── G14 — multi-generational cohort ratio audited ──
(deftest test-g14-cohort-ratio-audit
  (let [scopes (known (lex "silenCareReview") "scope")]
    (is (or (contains? scopes "cohort-ratio-quarterly-audit")
            (contains? scopes "g14-multi-gen-cohort-ratio-audit")))))
