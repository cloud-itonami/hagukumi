(ns hagukumi.record-key-test
  "Record-key identity for the hagukumi actor boundary.

  `hagukumi.murakumo/safe-rkey` decides the rkey every `:mst/put-record`
  effect is written under, so it decides record *identity* in the MST. It had
  no test at all, and it was migrated from `clojure.string` to
  `kotoba.lang.text` in 8f33084 -- a rewrite of exactly the kind that can
  change a regex/blank edge without changing any caller.

  What is pinned here, and why each one can actually fail:

  - the `did:web:` strip is **anchored and single-shot**. Un-anchoring it is a
    one-character edit that leaves every ordinary input unchanged.
  - the output charset is **closed**. A widened character class still passes
    every happy-path example; only hostile input separates them.
  - **only the empty input becomes \"unknown\"**. Whitespace-only input becomes
    \"---\". This is the boundary case CLAUDE.md's fifth question asks for:
    `blank?` applied to the *original* rather than the *cleaned* string is
    green on every other input in this file.
  - **every branch of the rkey fallback chain is sanitized**, not just the
    explicit `:rkey` one. The chain has five sources and four of them had no
    test, so a caller-supplied `:tid` could have reached the MST unsanitized.

  Ground truth for each expectation below was read off the built namespace on
  main, not predicted from the source."
  (:require [clojure.test :refer [deftest is testing]]
            [hagukumi.murakumo :as m]))

;; The rkey grammar this actor targets: RFC 3986 unreserved characters.
(def ^:private closed-charset #"[A-Za-z0-9._~-]*")

(def ^:private hostile-inputs
  ["a b/c" "  " "日本語" "//" "?q=1&r=2" "a\tb" "a\nb" "%2F" "#frag"
   "did:web:x.com:y" "x-did:web:y" "did:web:did:web:x" "'; DROP --"
   "../../etc/passwd" "AT://did:plc:abc/app.bsky.feed.post/3k"])

(defn- closed? [s] (boolean (re-matches closed-charset s)))

;; ---------------------------------------------------------------- safe-rkey

(deftest safe-rkey-strips-the-did-web-prefix-only-at-the-start
  (testing "a leading did:web: is removed"
    (is (= "etzhayyim.com-hagukumi" (m/safe-rkey "did:web:etzhayyim.com:hagukumi")))
    (is (= "a" (m/safe-rkey "did:web:a"))))
  (testing "did:web: that is not at the start is sanitized, not stripped"
    (is (= "x-did-web-y" (m/safe-rkey "x-did:web:y"))))
  (testing "the strip happens once, so a second did:web: is sanitized"
    (is (= "did-web-x" (m/safe-rkey "did:web:did:web:x")))))

(deftest safe-rkey-output-charset-is-closed
  (doseq [in hostile-inputs]
    (let [out (m/safe-rkey in)]
      (is (closed? out)
          (str (pr-str in) " produced " (pr-str out)
               " which leaves the unreserved-character set")))))

(deftest safe-rkey-preserves-the-unreserved-characters
  (testing "the four non-alphanumeric characters the grammar allows survive"
    (is (= "~._-" (m/safe-rkey "~._-"))))
  (testing "alphanumerics survive"
    (is (= "aZ09" (m/safe-rkey "aZ09")))))

(deftest safe-rkey-maps-only-the-empty-input-to-unknown
  (testing "empty and nil are the only inputs that become \"unknown\""
    (is (= "unknown" (m/safe-rkey "")))
    (is (= "unknown" (m/safe-rkey nil))))
  (testing "whitespace-only input is sanitized, NOT collapsed to \"unknown\""
    ;; The boundary. `blank?` is applied to the cleaned string, and cleaning
    ;; has already turned the spaces into dashes, so it can never fire here.
    (is (= "---" (m/safe-rkey "   ")))
    (is (= "-" (m/safe-rkey " "))))
  (testing "a string made only of forbidden characters is dashes, not \"unknown\""
    (is (= "--" (m/safe-rkey "//")))))

(deftest safe-rkey-coerces-non-strings
  (is (= "42" (m/safe-rkey 42)))
  (is (= "-kw" (m/safe-rkey :kw)))
  (is (closed? (m/safe-rkey {:a 1}))))

;; ------------------------------------------------- rkey resolution in plans

(def ^:private single-collection-spec
  (first (filter #(= 1 (count (:collections %))) (vals m/cell-specs))))

(defn- rkeys [input] (mapv :rkey (m/records-for single-collection-spec input)))

(deftest records-for-resolves-the-rkey-in-declared-precedence-order
  (testing "keyword :rkey outranks every other source"
    (is (= ["kw"] (rkeys {:record {:rkey "kw" "rkey" "s" :tid "t"} :request-id "REQ"}))))
  (testing "string \"rkey\" outranks :tid and the request id"
    (is (= ["s"] (rkeys {:record {"rkey" "s" :tid "t"} :request-id "REQ"}))))
  (testing ":tid outranks the request id"
    (is (= ["t"] (rkeys {:record {:tid "t"} :request-id "REQ"}))))
  (testing "the request id outranks the generated default"
    (is (= ["REQ"] (rkeys {:request-id "REQ"}))))
  (testing "with no source at all, the default is <legacy-cell>-<index>"
    (is (= [(str (:legacy-cell single-collection-spec) "-0")] (rkeys {})))))

(deftest every-branch-of-the-rkey-chain-is-sanitized
  ;; The explicit :rkey branch was the only one under test. A :tid or a
  ;; request id arriving from a caller reaches the MST through the same
  ;; function and must be cleaned the same way.
  (doseq [hostile hostile-inputs
          [label input] [["keyword :rkey" {:record {:rkey hostile}}]
                         ["string \"rkey\"" {:record {"rkey" hostile}}]
                         [":tid" {:record {:tid hostile}}]
                         ["request-id" {:request-id hostile}]]]
    (let [[out] (rkeys input)]
      (is (closed? out)
          (str label " passed " (pr-str hostile) " through as " (pr-str out)))
      (is (= (m/safe-rkey hostile) out)
          (str label " did not agree with safe-rkey on " (pr-str hostile))))))

(deftest every-cell-produces-a-nonempty-closed-rkey-under-hostile-input
  (doseq [[cell-key spec] m/cell-specs
          hostile hostile-inputs]
    (doseq [{:keys [rkey]} (m/records-for spec {:request-id hostile})]
      (is (and (seq rkey) (closed? rkey))
          (str cell-key " produced " (pr-str rkey) " from " (pr-str hostile))))))

(deftest the-effect-and-the-record-it-writes-carry-the-same-rkey
  ;; cell-plan builds effects from the planned records; if that linkage were
  ;; broken the effect would write under a key nothing else refers to.
  (let [attest (into {} (map (fn [g] [g true]))
                     (distinct (mapcat :required-gates (vals m/cell-specs))))]
    (doseq [cell-key (keys m/cell-specs)]
      (let [{:keys [records effects]} (m/cell-plan cell-key {:attestations attest
                                                             :request-id "a request/id"})]
        (is (= (mapv :rkey records) (mapv :rkey effects)))
        (is (= (mapv :collection records) (mapv :collection effects)))
        (is (every? closed? (map :rkey effects)))))))

;; ------------------------------------------------------- characterization

(deftest caller-supplied-fields-currently-outrank-the-actors-own-identity-fields
  ;; CHARACTERIZATION, not endorsement. `records-for` merges the caller's
  ;; record LAST, so it wins over the boundary's own `:$type`, `:actorDid`,
  ;; `:scaffold` and `:constitutionalStatus`. The existing suite asserts
  ;; `(= actor-did (:actorDid record))` but only feeds inputs that do not set
  ;; it, so that assertion holds for a reason other than the invariant.
  ;;
  ;; Recorded here so the precedence is stated rather than assumed. It should
  ;; be revisited before R1 lets any real record reach the MST: an $type that
  ;; disagrees with its collection is invalid under atproto, and an actorDid
  ;; the actor did not choose is a self-attestation the actor did not make.
  (let [[{:keys [record]}] (m/records-for single-collection-spec
                                          {:record {:$type "com.evil.spoof"
                                                    :actorDid "did:web:attacker"
                                                    :scaffold false
                                                    :constitutionalStatus "production"}})]
    (is (= "com.evil.spoof" (:$type record)))
    (is (= "did:web:attacker" (:actorDid record)))
    (is (false? (:scaffold record)))
    (is (= "production" (:constitutionalStatus record)))))
