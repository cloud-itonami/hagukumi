(ns hagukumi.cells.meal-delivery.state-machine
  "MealDeliveryCell — hagukumi R0 scaffold per ADR-2605261030.

  R0 scaffold. mitsuho-sourced meal delivery. Aggregate-only deliveryAttestation
  (no recipient PII; per ADR-2605181200 30-day rotating pseudonym DID). Sukoyaka
  cold-chain (yakushi inheritance) at R2+.

  Faithful cljc port of cells/meal_delivery/cell.py (R0 raising stub).
  The real constitutional gate logic lives in hagukumi.methods.agent.")

(defn solve
  "R0 scaffold: raises ex-info mirroring Python RuntimeError.
  mitsuho-sourced meal delivery to adherents (cold-chain L4 transport)."
  [_state]
  (throw
   (ex-info
    "hagukumi R0 scaffold: meal_delivery cell not activated. Requires ADR-2605261030 Council ratify + mitsuho R2 foodLotAttestation production + Sukoyaka cold-chain R2+ deploy."
    {:cell   :meal-delivery
     :status :r0-scaffold
     :adr    "ADR-2605261030"})))
