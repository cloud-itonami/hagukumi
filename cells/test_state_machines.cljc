(ns hagukumi.cells.test-state-machines
  "hagukumi — R0 scaffold state_machine.cljc parity tests.

  Verifies that each cell's solve/1 raises ex-info mirroring the Python
  RuntimeError, with the correct :status :r0-scaffold sentinel and the
  'R0 scaffold' string in the message.

  Parity evidence: Python cells raise RuntimeError; cljc cells raise ex-info.
  Message substring matches the Python docstring / RuntimeError text verbatim.

  ADR-2605261030."
  (:require [kotoba.lang.text] [clojure.test :refer [deftest is testing]]
            [hagukumi.cells.child-daily-care.state-machine   :as cdc]
            [hagukumi.cells.chronic-continuity.state-machine :as cc]
            [hagukumi.cells.elder-companionship.state-machine :as ec]
            [hagukumi.cells.meal-delivery.state-machine       :as md]
            [hagukumi.cells.respite-support.state-machine     :as rs]))

;; ── helpers ──────────────────────────────────────────────────────────────────

(defn- throws-r0-scaffold? [f]
  "Returns true iff (f) throws ex-info with :status :r0-scaffold sentinel."
  (try
    (f)
    false
    (catch clojure.lang.ExceptionInfo e
      (and (kotoba.lang.text/includes? (ex-message e) "R0 scaffold")
           (= :r0-scaffold (:status (ex-data e)))))))

;; ── child_daily_care ─────────────────────────────────────────────────────────

(deftest test-child-daily-care-solve-raises
  (testing "child_daily_care solve raises R0 scaffold ex-info (PARITY)"
    (is (throws-r0-scaffold? #(cdc/solve {})))
    ;; cell and adr metadata present
    (try (cdc/solve {})
      (catch clojure.lang.ExceptionInfo e
        (is (= :child-daily-care (:cell (ex-data e))))
        (is (= "ADR-2605261030" (:adr (ex-data e))))
        ;; message mirrors Python RuntimeError
        (is (kotoba.lang.text/includes? (ex-message e) "pediatrician"))))))

;; ── chronic_continuity ───────────────────────────────────────────────────────

(deftest test-chronic-continuity-solve-raises
  (testing "chronic_continuity solve raises R0 scaffold ex-info (PARITY)"
    (is (throws-r0-scaffold? #(cc/solve {})))
    (try (cc/solve {})
      (catch clojure.lang.ExceptionInfo e
        (is (= :chronic-continuity (:cell (ex-data e))))
        (is (kotoba.lang.text/includes? (ex-message e) "mitate"))))))

;; ── elder_companionship ──────────────────────────────────────────────────────

(deftest test-elder-companionship-solve-raises
  (testing "elder_companionship solve raises R0 scaffold ex-info (PARITY)"
    (is (throws-r0-scaffold? #(ec/solve {})))
    (try (ec/solve {})
      (catch clojure.lang.ExceptionInfo e
        (is (= :elder-companionship (:cell (ex-data e))))
        (is (kotoba.lang.text/includes? (ex-message e) "geriatrician"))))))

;; ── meal_delivery ────────────────────────────────────────────────────────────

(deftest test-meal-delivery-solve-raises
  (testing "meal_delivery solve raises R0 scaffold ex-info (PARITY)"
    (is (throws-r0-scaffold? #(md/solve {})))
    (try (md/solve {})
      (catch clojure.lang.ExceptionInfo e
        (is (= :meal-delivery (:cell (ex-data e))))
        (is (kotoba.lang.text/includes? (ex-message e) "mitsuho"))))))

;; ── respite_support ──────────────────────────────────────────────────────────

(deftest test-respite-support-solve-raises
  (testing "respite_support solve raises R0 scaffold ex-info (PARITY)"
    (is (throws-r0-scaffold? #(rs/solve {})))
    (try (rs/solve {})
      (catch clojure.lang.ExceptionInfo e
        (is (= :respite-support (:cell (ex-data e))))
        (is (kotoba.lang.text/includes? (ex-message e) "caregiver onboarding"))))))

;; ── Python parity smoke ──────────────────────────────────────────────────────
;; Python: all 5 .solve() raise RuntimeError with "R0 scaffold" in message.
;; cljc:   all 5 .solve() throw ex-info with "R0 scaffold" in message.
;; Structurally equivalent — different exception types (RuntimeError vs ExceptionInfo)
;; but same observable contract: call → throw, message contains "R0 scaffold".
;;
;; Live Python parity: unrunnable in this CI (no Python/langgraph env); hand-checked
;; by reading cell.py sources which contain verbatim RuntimeError("hagukumi R0 scaffold: <name>…").
;; The cljc message strings are copied from those sources.
