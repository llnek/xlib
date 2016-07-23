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

(ns czlabtest.xlib.strutils

  (:require [czlab.xlib.str :as SU])
  (:use [clojure.test]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftest czlabtestxlib-strutils

  (is (not (SU/matchChar? \space #{ \a \b \x })))
  (is (SU/matchChar? \x #{ \a \b \x }))

  (is (= "ABC" (SU/ucase "abc")))
  (is (= "abc" (SU/lcase "ABC")))

  (is (= "" (SU/ucase nil)))
  (is (= "" (SU/lcase nil)))

  (is (not (SU/hgl? nil)))
  (is (not (SU/hgl? "")))
  (is (SU/hgl? "aaa"))

  (is (= "b" (SU/stror nil "b")))
  (is (= "b" (SU/stror "" "b")))
  (is (= "a" (SU/stror "a" "b")))

  (is (= "abcdefghijk" (SU/triml "abcdefghijk" "xyz")))
  (is (= "defghijk" (SU/triml "abcdefghijk" "abc")))
  (is (= "" (SU/triml "abcdefghijk" "abcdefghijk")))

  (is (= "abcdefghijk" (SU/trimr "abcdefghijk" "xyz")))
  (is (= "abcdefgh" (SU/trimr "abcdefghijk" "ijk")))
  (is (= "" (SU/trimr "abcdefghijk" "abcdefghijk")))

  (is (== 4 (SU/countChar "abcaaazzz" \a)))
  (is (== 0 (SU/countChar "abcaaazzz" \A)))

  (is (== 4 (SU/countStr "abcaaazzz" "a")))
  (is (== 0 (SU/countStr "abcaaazzz" "A")))

  (is (= ["abc" "def" "ghijk"] (SU/splitTokens "abc,def,ghijk,,,," ",")))
  (is (= ["abc" ":" "def"] (SU/splitTokens "abc:def" ":" true)))
  (is (= ["abc:def" ] (SU/splitTokens "abc:def" "?")))

  (is (not (SU/embeds? "hello joe" "john")))
  (is (SU/embeds? "hello joe" "joe"))
  (is (not (SU/embeds? "hello joe" "JOE")))
  (is (SU/hasNoCase? "hello joe" "JOE"))

  (is (== -1 (SU/indexAny "hallowed are the ori" "zku")))
  (is (== -1 (SU/indexAny "hallowed are the ori" "")))
  (is (== 5 (SU/indexAny "hallowed are the ori" "zkw")))
  (is (== 1 (SU/indexAny "hallowed are the ori" "akz")))

  (is (not (SU/has? "hallowed are the ori" \z)))
  (is (SU/has? "hallowed are the ori" \w))

  (is (= "abc" (SU/sname "abc")))
  (is (= "abc" (SU/sname :abc)))
  (is (nil? (SU/sname nil)))

  (is (= :a/b/c (SU/toKW "a" "b" "c")))
  (is (nil? (SU/toKW )))

  (is (= "heeloo" (SU/nsb "heeloo")))
  (is (= "" (SU/nsb nil)))

  (is (= "heeloo" (SU/nsn "heeloo")))
  (is (= "(null)" (SU/nsn nil)))

  (is (not (SU/same? "aaa" "axa")))
  (is (SU/same? "aaa" "aaa"))

  (is (not (SU/nichts? "aaa")))
  (is (SU/nichts? nil))
  (is (SU/nichts? ""))

  (is (= "aaa" (SU/strim "   aaa   ")))
  (is (= "" (SU/strim nil)))
  (is (= "aaabbbccc" (SU/strimAny "aaabbbccc" "xyz")))
  (is (= "bbb" (SU/strimAny "aaabbbccc" "ac")))
  (is (= "bbb" (SU/strimAny "   aaa  bbb  ccc  " "ac" true)))

  (is (not (SU/hgl? "")))
  (is (SU/hgl? "haha"))

  (is (= "haha" (SU/strim "            haha                          ")))
  (is (= "" (SU/strim nil)))

  (is (= "joe;blogg" (str (doto (StringBuilder.)
                            (SU/addDelim! ";" "joe")
                            (SU/addDelim! ";" "blogg")))))

  (is (= 4 (count (SU/splunk "hello, how are you" 5))))


  (is (SU/hasicAny? "hello, how are you?" ["HELLO" ]))
  (is (SU/hasicAny? "hello, how are you?" ["you" ]))
  (is (not (SU/hasicAny? "hello, how are you?" [])))

  (is (not (SU/hasAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "Are" ])))
  (is (SU/hasAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "ori" ]))

  (is (SU/swicAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "Hall" ]))
  (is (SU/swAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "ha" ]))
  (is (not (SU/swAny? "hallowed are the ori" [ "sdfsdg" "jffflf" ])))

  (is (SU/ewicAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "Ori" ]))
  (is (SU/ewAny? "hallowed are the ori" [ "sdfsdg" "jffflf" "ori" ]))
  (is (not (SU/ewAny? "hallowed are the ori" [ "sdfsdg" "jffflf" ])))

  (is (SU/eqicAny? "heeloo" [ "sdfsdg" "jffflf" "HeeLoo" ]))
  (is (SU/eqAny? "heeloo" [ "sdfsdg" "jffflf" "heeloo" ]))
  (is (not (SU/eqAny? "heeloo" [ "sdfsdg" "jffflf" ])))

  (is (not (SU/eqic? "abc" "AbCd")))
  (is (SU/eqic? "abc" "AbC"))

  (is (= 10 (.length (SU/str<> \x 10))))
  (is (= "xxx" (SU/str<> \x 3)))
  (is (= "ori" (SU/rights "Hallowed are the ori" 3)))
  (is (= "Hal" (SU/lefts "Hallowed are the ori" 3)))

  (is (= "abc def" (SU/urlDecode (SU/urlEncode "abc def"))))
  (is (not= "abc def" (SU/urlEncode "abc def")))

;;
)


;;(clojure.test/run-tests 'czlabtest.xlib.strutils)

