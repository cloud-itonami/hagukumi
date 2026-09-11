#!/usr/bin/env bash
# hagukumi 育み — run the constitutional-gate + agent test suites.
# Exits non-zero on any failure (deploy-gate friendly).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../.." && pwd)"
rc=0

BB_CP="20-actors:20-actors/kotodama/src:50-infra/etzhayyim-moyai-credit/src:70-tools/src:70-tools"

run_cljc() {
  local ns="$1"
  echo "==> hagukumi [cljc] $ns"
  ( cd "$REPO_ROOT" && kbb -cp "$BB_CP" -e \
    "(require (quote clojure.test) (quote $ns))(let [r (clojure.test/run-tests (quote $ns))](System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))" ) || rc=1
}

run_cljc "hagukumi.methods.test-charter-gates"
run_cljc "hagukumi.methods.test-agent"
run_cljc "hagukumi.cells.test-state-machines"

if [[ $rc -eq 0 ]]; then
  echo "==> hagukumi: ALL GREEN"
else
  echo "==> hagukumi: FAILURES (rc=$rc)" >&2
fi
exit $rc
