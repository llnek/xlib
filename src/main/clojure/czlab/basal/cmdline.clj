;; Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Console interactions."
      :author "Kenneth Leung" }

  czlab.basal.cmdline

  (:require [czlab.basal.logging :as log]
            [clojure.string :as cs])

  (:use [czlab.basal.core]
        [czlab.basal.str])

  (:import [java.io
            InputStreamReader
            OutputStreamWriter
            BufferedOutputStream]
           [java.io Reader Writer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- isOption?
  ""
  [^String option]
  (and (some? option)
       (not= "--" option)
       (or (.startsWith option "--")
           (.startsWith option "-")
           (.startsWith option "/"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeOption
  ""
  [option key?]
  (when (isOption? option)
    (let [s (-> (cs/replace option #"^(-|/)+" "")
                (cs/trim))]
      (if (> (.length s) 0)
        (if key? (keyword s) s)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn parseOptions
  ""
  ([cmdline] (parseOptions cmdline true))
  ([cmdline key?]
   (loop [options (transient {})
          [p1 p2 & more
           :as args] cmdline]
     (if-some [o1 (maybeOption p1 key?)]
       (if (or (nil? p2)
               (isOption? p2))
         (recur (assoc! options o1 true)
                (if (nil? p2)
                  more
                  (cons p2 more)))
         (recur (assoc! options o1 p2) more))
       [(persistent! options)
        (if (= "--" p1)
          (if (some? p2)
            (cons p2 more) [])
          (or args []))]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- readData
  "Read user input"
  ^String
  [^Writer cout ^Reader cin]
  ;; windows has '\r\n' linux has '\n'
  (let
    [bf (strbf<>)
     ms (loop
          [c (.read cin)]
          (let
            [m (cond
                 (or (== c -1) (== c 4))
                 #{:quit :break}
                 (== c (int \newline))
                 #{:break}
                 (or (== c (int \backspace))
                     (== c (int \return))
                     (== c 27))
                 nil
                 :else
                 (do->nil
                   (.append bf (char c))))]
            (if (in? m :break)
              m
              (recur (.read cin)))))]
    (if-not (in? ms :quit) (strim bf))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- onAnswer
  "Process the answer, returning the next question"
  [^Writer cout
   cmdQ
   props
   answer]
  (let [{:keys [default
                result
                id
                must
                next]}
        cmdQ]
    (if (nil? answer)
      (do->nil (.write cout "\n"))
      (let [rc (stror answer default)]
        (cond
          ;;if required to answer, repeat the question
          (and (nichts? rc) must)
          id

          (keyword? result)
          (do (swap! props
                     assoc result rc)
              next)

          (fn? result)
          (let [[n p]
                (result rc @props)]
            (reset! props p)
            n)

          :else :end)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- popQQ
  "Pop the question"
  [^Writer cout
   ^Reader cin
   cmdQ
   props]
  (let [{:keys [^String question
                ^String choices
                ^String default]}
        cmdQ]
    (.write cout
            (str question
                 (if (:must cmdQ) "*" "") "? "))
    ;; choices ?
    (when-not (nichts? choices)
      (if (has? choices \n)
        (.write cout
                (str (if (.startsWith choices "\n") "[" "[\n")
                     choices
                     (if (.endsWith choices "\n") "]" "\n]" )))
        (.write cout (str "[" choices "]"))))
    ;; defaults ?
    (when-not (nichts? default)
      (.write cout (str "(" default ")")))
    (doto cout (.write " ")(.flush))
    ;; get the input from user
    ;; return the next question, :end ends it
    (->> (readData cout cin)
         (onAnswer cout cmdQ props))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- popQ
  "Pop the question"
  [cout cin cmdQ props]
  (if (some? cmdQ) (popQQ cout cin cmdQ props) :end))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cycleQ
  "Cycle through the questions"
  [cout cin cmdQNs start props]
  (loop [rc (popQ cout
                  cin
                  (cmdQNs start) props)]
    (cond
      (= :end rc) @props
      (nil? rc) {}
      :else (recur (popQ cout
                         cin
                         (cmdQNs rc) props)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn consoleIO
  "Prompt a sequence of questions via console"
  [cmdQs question1]
  {:pre [(map? cmdQs)]}
  (let [cout (->> (BufferedOutputStream. (System/out))
                  (OutputStreamWriter.))
        kp (if (isWindows?) "<ctrl-c>" "<ctrl-d>")
        cin (InputStreamReader. (System/in))
        func (partial cycleQ cout cin)]
    (.write cout (str ">>> Press "
                      kp
                      "<Enter> to cancel...\n"))
    (->
      (reduce
        #(assoc %1 %2 (assoc (get cmdQs %2) :id %2))
        {}
        (keys cmdQs))
      (func question1 (atom {})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(comment
(def QM
  {:q1 {:question "hello ken"
        :choices "q|b|c"
        :default "c"
        :required true
        :result :a1
        :next :q2}

   :q2 {:question "hello paul"
        :result :a2
        :next :q3}

   :q3 {:question "hello joe"
        :choices "2"
        :result (fn [answer result]
                  [:end (assoc result :zzz answer)])}})
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

