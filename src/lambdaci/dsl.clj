(ns lambdaci.dsl)

(def initial-pipeline-state {:running [] :finished []})
(def pipeline-state (atom initial-pipeline-state))

(defn get-pipeline-state []
  @pipeline-state)

(defn reset-pipeline-state []
  (reset! pipeline-state initial-pipeline-state))

(defn set-running! [step-id]
  (swap! pipeline-state #(assoc %1 :running (cons step-id (:running %1)))))

(defn set-finished! [step-id] ;; TODO: this should also remove it from the running-list. at the moment, css magic makes appear ok
  (swap! pipeline-state #(assoc %1 :finished (cons step-id (:finished %1)))))

(defn- range-from [from len] (range (inc from) (+ (inc from) len)))

(defn execute-step [step args step-id]
  (set-running! step-id)
  (let [step-result (step args step-id)]
    (println (str "executing step " step-id step-result))
    (set-finished! step-id)
    {:outputs { step-id step-result}} ))


(defn execute-step-foo [args [step-id step]]
  (execute-step step args step-id))


(defn merge-step-results [r1 r2]
  (merge-with merge r1 r2))

(defn steps-with-ids [steps prev-id]
  (let [significant-part (first prev-id)
        rest-part (rest prev-id)
        significant-ids (range-from significant-part (count steps))
        ids (map #(cons %1 rest-part) significant-ids)]
    (map vector ids steps)))

(defn execute-steps-internal [step-result-producer steps args step-id]
  (let [step-results (step-result-producer args (steps-with-ids steps step-id))]
    (reduce merge-step-results args step-results)))


(defn serial-step-result-producer [args s-with-id]
  (map (partial execute-step-foo args) s-with-id))

(defn execute-steps [steps args step-id]
  (execute-steps-internal serial-step-result-producer steps args step-id))

(defn parallel-step-result-producer [args s-with-id]
  (pmap (partial execute-step-foo args) s-with-id))

(defn execute-steps-in-parallel [steps args step-id]
  (execute-steps-internal parallel-step-result-producer steps args step-id))

(defn in-parallel [& steps]
  (fn [args step-id]
    (execute-steps-in-parallel steps args (cons 0 step-id))))

(defn in-cwd [cwd & steps]
  (fn [args step-id]
    (execute-steps steps (assoc args :cwd cwd) (cons 0 step-id))))


(defn run [pipeline]
  (reset-pipeline-state)
  (let [runnable-pipeline (map eval pipeline)]
    (execute-steps runnable-pipeline {} [0])))
