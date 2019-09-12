;; Copyright © 2013-2019, Kenneth Leung. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Useful os process & runtime functions."
      :author "Kenneth Leung"}

  czlab.basal.proc

  (:require [czlab.basal.util :as u]
            [czlab.basal.log :as l]
            [czlab.basal.core :as c]
            [czlab.basal.xpis :as po])

  (:import [java.util
            Map
            Timer
            Timer
            TimerTask
            Properties]
           [java.lang
            Thread
            Runnable]
           [java.util.concurrent
            ThreadFactory
            LinkedBlockingQueue
            ThreadPoolExecutor
            TimeUnit
            ConcurrentHashMap
            RejectedExecutionHandler]
           [java.lang.management
            RuntimeMXBean
            ManagementFactory
            OperatingSystemMXBean]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn tcore<>
  ""
  ([id] (tcore<> id (* 2 (.availableProcessors (Runtime/getRuntime)))))
  ([id tds] (tcore<> id tds true))
  ([id tds trace?] (tcore<> id tds 60000 trace?))
  ([id tds keepAliveMillis trace?]
   (let [^ThreadPoolExecutor
         core (proxy [ThreadPoolExecutor]
                [(int (max 1 tds))
                 (int (max 1 tds))
                 ^long keepAliveMillis
                 TimeUnit/MILLISECONDS
                 (LinkedBlockingQueue.)])
         paused? (c/int-var)
         rex (reify
               RejectedExecutionHandler
               (rejectedExecution [_ r x]
                 (if trace? (l/error "TCore rejecting work!"))))]
     (.setRejectedExecutionHandler core rex)
     (.setThreadFactory core
                        (reify ThreadFactory
                          (newThread [_ r]
                            (c/do-with [t (Thread. r)]
                              (.setName t (str id "#" (u/seqint2)))
                              (.setContextClassLoader t (u/get-cldr))))))
     (if trace?
       (l/debug
         "TCore#%s - ctor: threads = %s" id (.getCorePoolSize core)))
     (reify
       Object
       (toString [_]
         (c/fmt "TCore#%s - threads = %s." id (.getCorePoolSize core)))
       po/Startable
       (start [me _] (locking me (c/int-var paused? 0) me))
       (start [me] (.start me nil))
       (stop [me] (locking me (c/int-var paused? 1) me))
       po/Enqueable
       (put [me r]
         (if (and (c/is? Runnable r)
                  (zero? (c/int-var paused?)))
           (.execute core ^Runnable r)
           (l/warn "TCore is not running!")) me)
       po/Finzable
       (finz [me]
         (.stop me)
         (.shutdown core)
         (if trace?
           (l/debug
             "TCore#%s - disposed and shut down." id)) me)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn thread<>
  "Run code in a separate thread."
  {:tag Thread}

  ([func start?]
   (thread<> func start? nil))

  ([func start? {:keys [cldr daemon] :as options}]
   {:pre [(fn? func)]}
   (c/do-with [t (Thread. (u/run<> (func)))]
     (let [c (or cldr (u/get-cldr))]
       (some->> (c/cast? ClassLoader c)
                (.setContextClassLoader t))
       (.setDaemon t (true? daemon))
       (if start? (.start t))
       (l/debug "thread#%s%s%s"
                (.getName t)
                ", daemon = " (.isDaemon t))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn async!
  "Run function async."
  ([func]
   (async! func nil))
  ([func options]
   (thread<> func true options)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord JvmInfo [])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn jvm-info
  "Get info on the jvm." []
  (let [os (ManagementFactory/getOperatingSystemMXBean)
        rt (ManagementFactory/getRuntimeMXBean)]
    (c/object<> JvmInfo
                :spec-version (.getSpecVersion rt)
                :vm-version (.getVmVersion rt)
                :spec-vendor (.getSpecVendor rt)
                :vm-vendor (.getVmVendor rt)
                :spec-name (.getSpecName rt)
                :vm-name (.getVmName rt)
                :name (.getName rt)
                :arch (.getArch os)
                :os-name (.getName os)
                :os-version (.getVersion os)
                :processors (.getAvailableProcessors os))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn process-pid
  "Get process pid." ^String []
  (let [b (ManagementFactory/getRuntimeMXBean)]
   (u/try!! "" (c/_1 (-> b .getName str (c/split "@"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delay-exec
  "Run function after some delay."
  [func delayMillis]
  {:pre [(fn? func)
         (c/spos? delayMillis)]}
  (.schedule (Timer. true) (u/tmtask<> func) ^long delayMillis))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn exit-hook
  "Add a shutdown hook."
  [func]
  {:pre [(fn? func)]}
  (->> (thread<> func false {:daemon true})
       (.addShutdownHook (Runtime/getRuntime))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;scheduler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- prerun
  [hQ w] (some->> w (.remove ^Map hQ)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-timer
  [timer task delayMillis]
  (.schedule ^Timer
             timer ^TimerTask task ^long delayMillis))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Scheduler
  "Schedules tasks."
  (alarm [_ delayMillis f args] "")
  (run [_ w] "Run this task.")
  (postpone [_ w delayMillis] ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn scheduler<>
  "Make a Scheduler."
  ([]
   (scheduler<> (u/jid<>) nil))
  ([named options]
   (let [{:keys [threads trace?]} options
         id (c/sname named)
         timer (Timer. id true)
         cpu (tcore<> id
                      ^long
                      (c/num?? threads 0)
                      (c/!false? trace?))]
     (reify Scheduler
       (alarm [_ delayMillis f args]
         {:pre [(fn? f)
                (sequential? args)
                (c/spos? delayMillis)]}
         (c/do-with
           [tt (u/tmtask<> #(apply f args))]
           (add-timer timer tt delayMillis)))
       (run [me w]
         (po/put cpu w) me)
       (postpone [me w delayMillis]
         {:pre [(number? delayMillis)]}
         (cond (zero? delayMillis)
               (.run me w)
               (pos? delayMillis)
               (.alarm me delayMillis #(.run me w) [])) me)
       po/Activable
       (activate [me]
         (po/start cpu) me)
       (deactivate [me]
         (po/stop cpu)
         (doto timer .cancel .purge) me)
       po/Finzable
       (finz [me]
         (.deactivate me)
         (po/finz cpu) me)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


