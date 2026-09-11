(ns hagukumi.cells.chronic-continuity.state-machine
  "ChronicContinuityCell — hagukumi R0 scaffold per ADR-2605261030.

  R0 scaffold. mitate-paired cross-actor cell. Post-diagnosis support
  (medication adherence reminder, lifestyle adjustment, mitate re-check
  scheduling). Non-prescriptive — never substitutes mitate or yakushi.

  Faithful cljc port of cells/chronic_continuity/cell.py (R0 raising stub).
  The real constitutional gate logic lives in hagukumi.methods.agent.")

(defn solve
  "R0 scaffold: raises ex-info mirroring Python RuntimeError.
  Post-mitate-diagnosis chronic-care continuity support."
  [_state]
  (throw
   (ex-info
    "hagukumi R0 scaffold: chronic_continuity cell not activated. Requires ADR-2605261030 Council ratify + mitate R1 cross-actor XRPC referral pathway production-deployed."
    {:cell   :chronic-continuity
     :status :r0-scaffold
     :adr    "ADR-2605261030"})))
