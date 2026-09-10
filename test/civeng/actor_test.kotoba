(ns civeng.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [civeng.store :as store]
            [civeng.advisor :as advisor]
            [civeng.actor :as actor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "proj-1" :name "Downtown Bridge Inspection" :location "5th & Main"})
    st))

(deftest run-request-accepts-survey-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:project-id "proj-1" :op :draft-survey-record :stake :low}
                                  {}
                                  "thread-1")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "proj-1"))))))

(deftest run-request-escalates-structural-concern
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:project-id "proj-1" :op :flag-structural-concern :stake :high}
                                  {}
                                  "thread-2")]
    (is (= :interrupted (:status result)))
    (is (empty? (store/records-of st "proj-1")))))

(deftest run-request-rejects-unregistered-project
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph
                                  {:project-id "no-such-project" :op :draft-survey-record :stake :low}
                                  {}
                                  "thread-3")]
    (is (= :done (:status result)))
    (is (empty? (store/records-of st "no-such-project")))))

(deftest approve-resumes-and-commits
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result1 (actor/run-request! graph
                                   {:project-id "proj-1" :op :flag-structural-concern :stake :high}
                                   {}
                                   "thread-4")]
    (is (= :interrupted (:status result1)))
    (is (empty? (store/records-of st "proj-1")))

    ;; approve and resume
    (let [result2 (actor/approve! graph "thread-4")]
      (is (= :done (:status result2)))
      (is (= 1 (count (store/records-of st "proj-1")))))))

(deftest audit-ledger-records-all-events
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        _ (actor/run-request! graph
                             {:project-id "proj-1" :op :draft-survey-record :stake :low}
                             {}
                             "thread-5")]
    (is (>= (count (store/ledger st)) 1))))
