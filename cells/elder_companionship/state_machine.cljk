(ns hagukumi.cells.elder-companionship.state-machine
  "ElderCompanionshipCell — hagukumi R0 scaffold per ADR-2605261030.

  R0 scaffold. G6 elder autonomy invariant (no override except immediate safety
  threat) + mitate G5 emergency keyword fail-safe routing.

  Faithful cljc port of cells/elder_companionship/cell.py (R0 raising stub).
  The real constitutional gate logic lives in hagukumi.methods.agent.")

(defn solve
  "R0 scaffold: raises ex-info mirroring Python RuntimeError.
  Daily companion presence (conversation, gentle ADL assist, symptom screening)."
  [_state]
  (throw
   (ex-info
    "hagukumi R0 scaffold: elder_companionship cell not activated. Requires ADR-2605261030 Council ratify + >=1 geriatrician on Council medical advisory + mitate G5 emergency-keyword lexicon production-deployed."
    {:cell   :elder-companionship
     :status :r0-scaffold
     :adr    "ADR-2605261030"})))
