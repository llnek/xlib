;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; Copyright (c) 2013-2016, Kenneth Leung. All rights reserved.

(ns ^{:doc "Helper functions for String related operations"
      :author "Kenneth Leung" }

  czlab.xlib.str

  (:require
    [czlab.xlib.logging :as log]
    [clojure.string :as cs])

  (:import
    [java.util Arrays Collection Iterator StringTokenizer]
    [java.net URLEncoder URLDecoder]
    [clojure.lang
     Keyword
     APersistentVector]
    [java.io
     File
     CharArrayWriter
     OutputStream
     OutputStreamWriter
     Reader
     Writer]
    [java.lang StringBuilder]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)
(defonce ^:private HEXCHS (.toCharArray "0123456789abcdef"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro hgl?

  "true if this string *has good length* - not empty or nil"

  [s]

  `(not (empty? ~s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro stror

  "If not s then s2"
  [s s2]

  `(let [s# ~s] (if (empty? s#) ~s2 s#)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro lcase

  "Lowercase string, handling nil"
  [s]
  `(if-some [s# ~s] (cs/lower-case s#) ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro ucase

  "Uppercase string, handling nil"
  [s]

  `(if-some [s# ~s] (cs/upper-case s#) ""))

;;#^"[Ljava.lang.Class;"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn triml

  "Get rid of unwanted chars from left"

  ^String
  [^String src ^String unwantedChars]

  (if (and (hgl? unwantedChars)
           (hgl? src))
    (loop [len (.length src)
           pos 0]
      (if (and (< pos len)
               (>= (.indexOf unwantedChars
                             (int (.charAt src pos))) 0))
        (recur len (inc pos))
        (.substring src pos)))
    src))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn trimr

  "Get rid of unwanted chars from right"

  ^String
  [^String src ^String unwantedChars]

  (if (and (hgl? unwantedChars)
           (hgl? src))
    (loop [pos (.length src)]
      (if (and (> pos 0)
               (>= (.indexOf unwantedChars
                             (int (.charAt src (dec pos)))) 0))
        (recur (dec pos))
        (.substring src 0 pos)))
    src))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn splitTokens

  "String tokenizer"

  ^APersistentVector
  [^String s ^String sep & [incSep?]]

  (let [t (StringTokenizer. s sep (boolean incSep?))]
    (loop [rc (transient [])]
      (if-not (.hasMoreTokens t)
        (persistent! rc)
        (recur (conj! rc (.nextToken t)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn embeds?

  "true if sub-str is inside the bigger str"

  [^String bigs ^String s]

  (>= (.indexOf bigs s) 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro hasNoCase?

  "true if sub-str is inside the bigger str - ignore case"

  [bigs s]

  `(embeds? (lcase ~bigs) (lcase ~s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn has?

  "true if the char is inside the str"

  [^String bigs ^Character ch]

  (>= (.indexOf bigs (int (.charValue ch))) 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn indexAny

  "if any one char is inside the str, return the position"

  [^String bigs ^String chStr]

  (if (and (hgl? chStr)
           (hgl? bigs))
    (let [rc (some #(let [x (.indexOf bigs (int %))]
                      (if (< x 0) nil x))
                   (.toCharArray chStr))]
      (if (nil? rc) -1 (int rc)))
    -1))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn countStr

  "Count the times this sub-str appears in the source str"

  [^String src ^String sub]

  (if (and (hgl? src)
           (hgl? sub))
    (loop [len (.length sub)
           total 0
           start 0]
      (let [pos (.indexOf src sub start)]
        (if (< pos 0)
          total
          (recur len (inc total) (+ pos len)))))
    0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn countChar

  "Count the times this char appears in the source str"

  [^String src ^Character ch]

  (reduce
    #(if (= ch %2) (inc %1) %1)
    0
    (.toCharArray src)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro sname

  "Safely get the name of this object"

  ^String
  [n]

  `(when-some [n# ~n] (name n#)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn nsb

  "Empty string if obj is null, or obj.toString"

  ^String
  [obj]

  (if (keyword? obj) (name obj) (str obj)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn toKW

  "Concatenate all args and return it as a keyword"

  ^Keyword
  [& args]

  (if-not (empty? args)
    (keyword (cs/join "/" args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn nsn

  "(null) if obj is null, or obj.toString"

  ^String
  [obj]

  (if (nil? obj) "(null)" (str obj)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn same?

  "true if these 2 strings are the same"

  [^String a ^String b]

  (cond
    (and (nil? a) (nil? b))
    true

    (or (nil? a) (nil? b))
    false

    (not= (.length a) (.length b))
    false

    :else
    (Arrays/equals (.toCharArray a) (.toCharArray b)) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro nichts?

  "true if this string is empty"

  [s]

  `(empty? ~s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn strim

  "Safely trim this string - handles null"

  ^String
  [^String s]

  (if (nil? s) "" (cs/trim s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn strimAny

  "Strip source string of these unwanted chars"

  ^String
  [^String src ^String unwantedChars & [whitespace?]]

  (let [s (-> (if whitespace? (strim src) src)
              (trimr unwantedChars)
              (triml unwantedChars))]
    (if whitespace? (strim s) s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn addDelim!

  "Append to a string-builder, optionally
   inserting a delimiter if the buffer is not empty"

  ^StringBuilder
  [^StringBuilder buf ^String delim ^String item]

  (when (some? item)
    (when (and (> (.length buf) 0)
               (some? delim))
      (.append buf delim))
    (.append buf item))
  buf)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn splunk

  "Split a large string into chunks, each chunk having a specific length"

  ^APersistentVector
  [^String largeString chunkLength]

  (if-not (hgl? largeString)
    []
    (loop [ret (transient [])
           src largeString ]
      (if (<= (.length src) chunkLength)
        (persistent! (if (> (.length src) 0)
                       (conj! ret src)
                       ret))
        (recur (conj! ret (.substring src 0 chunkLength))
               (.substring src chunkLength))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn hasicAny?

  "Match against a list of possible args. (ignoring case)"

  [^String src substrs]

  {:pre [(coll? substrs)]}

  (if (or (empty? substrs)
          (nil? src))
    false
    (let [lc (lcase src)]
      (true? (some #(>= (.indexOf lc (lcase %)) 0) substrs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn hasAny?

  "Returns true if src contains one of these substrings"

  [^String src substrs]

  {:pre [(coll? substrs)]}

  (if (or (empty? substrs)
          (nil? src))
    false
    (true? (some #(>= (.indexOf src ^String %) 0) substrs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ewicAny?

  "Tests endsWith() no-case, looping through
   the list of possible suffixes"

  [^String src suxs]

  {:pre [(coll? suxs)]}

  (if (or (empty? suxs)
          (nil? src))
    false
    (let [lc (lcase src)]
      (true? (some #(.endsWith lc (lcase %)) suxs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ewAny?

  "Tests endsWith(), looping through
   the list of possible suffixes"

  [^String src suxs]

  {:pre [(coll? suxs)]}

  (if (or (empty? suxs)
          (nil? src))
    false
    (true? (some #(.endsWith src ^String %) suxs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn swicAny?

  "Tests startWith() no-case, looping through
   the list of possible prefixes"

  [^String src pfxs]

  {:pre [(coll? pfxs)]}

  (if (or (empty? pfxs)
          (nil? src))
    false
    (let [lc (lcase src)]
      (true? (some #(.startsWith lc (lcase %)) pfxs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn swAny?

  "Tests startWith(), looping through
   the list of possible prefixes"

  [^String src pfxs]

  {:pre [(coll? pfxs)]}

  (if (or (empty? pfxs)
          (nil? src))
    false
    (true? (some #(.startsWith src ^String %) pfxs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro eqic?

  ""
  [^String src ^String other]

  `(.equalsIgnoreCase ~src ~other))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn eqicAny?

  "Tests String.equals() against a
   list of possible args. (ignore-case)"

  [^String src strs]

  {:pre [(coll? strs)]}

  (if (or (empty? strs)
          (nil? src))
    false
    (let [lc (lcase src)]
      (true? (some #(.equals lc (lcase %)) strs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn eqAny?

  "Tests String.equals() against a list of possible args"

  [^String src strs]

  {:pre [(coll? strs)]}

  (if (or (empty? strs)
          (nil? src))
    false
    (true? (some #(.equals src %) strs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn makeString

  "Make a string of certain length"

  ^String
  [^Character ch cnt]

  (let [buf (StringBuilder.) ]
    (dotimes [n cnt]
      (.append buf ch))
    (.toString buf)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn rights

  "Gets the rightmost len characters of a String"

  ^String
  [^String src len]

  (if (or (<= len 0)
          (nil? src))
    ""
    (if (< (.length src) len)
      src
      (.substring src (- (.length src) len)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn lefts

  "Gets the leftmost len characters of a String"

  ^String
  [^String src len]

  (if (or (<= len 0)
          (nil? src))
    ""
    (if (< (.length src) len)
      src
      (.substring src 0 len))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- sformat

  ""

  [^String fmt & args]

  (String/format fmt (into-array Object args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn urldecode

  ""

  ^String
  [^String s & [enc]]

  (URLDecoder/decode s ^String (or enc "utf-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn urlencode

  ""

  ^String
  [^String s & [enc]]

  (URLEncoder/encode s ^String (or enc "utf-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


