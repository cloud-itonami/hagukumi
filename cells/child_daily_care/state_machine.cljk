(ns hagukumi.cells.child-daily-care.state-machine
  "ChildDailyCareCell — hagukumi R0 scaffold per ADR-2605261030.

  R0 scaffold. Privacy invariant: all session output MUST use XChaCha20
  envelope per ADR-2605181100. G2 (no video recording firmware-level) + G3
  (per-session consent) + G4 (caregiver Council vetting) + G5 (cognitive load
  cap) + G9 (human-in-loop) + G14 (multi-gen ratio) enforcement.

  Faithful cljc port of cells/child_daily_care/cell.py (R0 raising stub).
  The real constitutional gate logic lives in hagukumi.methods.agent.")

(defn solve
  "R0 scaffold: raises ex-info mirroring Python RuntimeError.
  Caregiver-mediated child daily activities (play, learning prep, hygiene, meals)."
  [_state]
  (throw
   (ex-info
    "hagukumi R0 scaffold: child_daily_care cell not activated. Requires ADR-2605261030 Council ratify + >=1 pediatrician on Council medical advisory + ADR-2605181100 encrypted-record framework production-deployed (privacy invariant)."
    {:cell   :child-daily-care
     :status :r0-scaffold
     :adr    "ADR-2605261030"})))
