;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defproject io.czlab/basal "1.1.0"

  :license {:url "http://www.eclipse.org/legal/epl-v10.html"
            :name "Eclipse Public License"}

  :description "General clojure helper functions"
  :url "https://github.com/llnek/basal"

  :dependencies [[org.apache.logging.log4j/log4j-slf4j-impl "2.12.0"]
                 [org.apache.logging.log4j/log4j-core "2.12.0"]
                 [org.slf4j/slf4j-api "1.7.26" ]
                 [org.clojure/core.async "0.4.500"]
                 [org.flatland/ordered "1.5.7"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.5.0"]]

  :exclusions [org.clojure/clojure]

  :plugins [[cider/cider-nrepl "0.21.1"]
            [lein-cprint "1.3.1"]
            [lein-codox "0.10.7"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure
                                        "1.10.1" :scope "provided"]]}
             :uberjar {:aot :all}}

  :global-vars {*warn-on-reflection* true}
  :target-path "out/%s"
  :aot :all

  :coordinate! "czlab"
  :omit-source true

  :java-source-paths ["src/main/java" "src/test/java"]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :resource-paths ["src/main/resources"]

  :test-selectors {:core :test-core
                   :str :test-str
                   :misc :test-misc
                   :psub :test-psub
                   :proc :test-proc
                   :ini :test-ini
                   :io :test-io
                   :meta :test-meta
                   :dates :test-dates
                   :util :test-util}

  :jvm-opts ["-Dlog4j.configurationFile=file:attic/log4j2.xml"
             "-Dczlabloggerflag=true"]

  :javac-options ["-source" "8"
                  "-Xlint:unchecked" "-Xlint:-options" "-Xlint:deprecation"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

