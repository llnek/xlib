;; Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Console interactions."
      :author "Kenneth Leung"}

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
(defn- isOption? "" [option]
  (and (string? option)
       (not= "--" option)
       (or (cs/starts-with? option "--")
           (cs/starts-with? option "-")
           (cs/starts-with? option "/"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeOption "" [option key?]
  (if (isOption? option)
    (if-some+
      [s (cs/replace option
                     #"^(-|/)+" "")]
      (if key? (keyword s) s))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn parseOptions
  "Parse command line, returning options and args"
  ([cmdline-args] (parseOptions cmdline-args true))
  ([cmdline-args key?]
   (loop [options (transient {})
          [p1 p2 & more
           :as args] cmdline-args]
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
   {:keys [default result id
           must? next] :as cmdQ}
   props answer]

  (if (nil? answer)
    (do->nil (. cout write "\n"))
    (let [rc (stror answer default)]
      (cond
        ;;if required to answer, repeat the question
        (and (nichts? rc) must?)
        id

        (keyword? result)
        (do (swap! props
                   assoc result rc) next)

        (fn? result)
        (let [[n p]
              (result rc @props)]
          (reset! props p)
          (if (nil? n) ::caio!! n))

        :else :caio!!))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- popQQ
  "Pop the question"
  [^Writer cout
   ^Reader cin
   {:keys [question
           choices
           default] :as cmdQ} props]

  (.write cout
          (str question
               (if (:must? cmdQ) "*") "? "))
  ;; choices ?
  (if (hgl? choices)
    (if (has? choices \n)
      (.write cout
              (str (if (cs/starts-with? choices "\n") "[" "[\n")
                   choices
                   (if (cs/ends-with? choices "\n") "]" "\n]" )))
      (.write cout (str "[" choices "]"))))
  ;; defaults ?
  (if (hgl? default)
    (.write cout (str "(" default ")")))
  (doto cout (.write " ")(.flush))
  ;; get the input from user
  ;; return the next question, :end ends it
  (->> (readData cout cin)
       (onAnswer cout cmdQ props)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- popQ
  "Pop the question"
  [cout cin cmdQ props]
  (if (some? cmdQ) (popQQ cout cin cmdQ props) :caio!!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cycleQ
  "Cycle through the questions"
  [cout cin cmdQNs start props]

  (loop [rc (popQ cout
                  cin
                  (cmdQNs start) props)]
    (cond
      (= :caio!! rc) @props
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
        cin (InputStreamReader. (System/in))
        func (partial cycleQ cout cin)]
    (.write cout (str ">>> Press "
                      "<ctrl-c> or <ctrl-d>"
                      "<Enter> to cancel...\n"))
    (-> (reduce
          #(update-in %1 [%2] assoc :id %2)
          cmdQs
          (keys cmdQs))
        (func question1 (atom {})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
#_
(def ExampleQuestions
  {:q1 {:question "hello ken"
        :choices "q|b|c"
        :default "c"
        :must? true
        :result :a1
        :next :q2}
   :q2 {:question "hello paul"
        :result :a2
        :next :q3}
   :q3 {:question "hello joe"
        :choices "2"
        :result (fn [answer result]
                  [nil (assoc result :zzz answer)])}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

