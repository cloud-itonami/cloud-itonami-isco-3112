(ns civeng.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [civeng.store :as store]
            [civeng.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "proj-1" :name "Downtown Bridge Inspection" :location "5th & Main"})
    st))

(deftest ok-on-clean-survey-record
  (let [st (fresh-store)
        proposal {:op :draft-survey-record :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-inspection-logging
  (let [st (fresh-store)
        proposal {:op :log-inspection-data :effect :propose :confidence 0.85 :stake :medium}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-site-visit-scheduling
  (let [st (fresh-store)
        proposal {:op :schedule-site-visit :effect :propose :confidence 0.8 :stake :low}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-project
  (let [st (fresh-store)
        proposal {:op :draft-survey-record :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "no-such-project"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-project (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :draft-survey-record :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-structural-concern
  (let [st (fresh-store)
        proposal {:op :flag-structural-concern :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :draft-survey-record :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:project-id "proj-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:project-id "proj-1" :op :draft-survey-record})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "proj-1"))))
    (is (= 1 (count (store/ledger st))))))
