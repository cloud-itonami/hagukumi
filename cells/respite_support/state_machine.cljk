(ns hagukumi.cells.respite-support.state-machine
  "RespiteSupportCell — hagukumi R0 scaffold per ADR-2605261030.

  R0 scaffold. Time-limited (8-24 hr) substitute caregiver for primary family
  caregiver. G10 (caregiver work cap) + G14 (multi-gen ratio) enforced.

  Faithful cljc port of cells/respite_support/cell.py (R0 raising stub).
  The real constitutional gate logic lives in hagukumi.methods.agent.")

(defn solve
  "R0 scaffold: raises ex-info mirroring Python RuntimeError.
  Time-limited caregiver-substitute for primary family caregiver."
  [_state]
  (throw
   (ex-info
    "hagukumi R0 scaffold: respite_support cell not activated. Requires ADR-2605261030 Council ratify + caregiver onboarding pipeline (training + background + Council Lv6+ >=3 vetting)."
    {:cell   :respite-support
     :status :r0-scaffold
     :adr    "ADR-2605261030"})))
