(ns cljs-wdc.core
  (:refer-clojure :exclude [enable-console-print!])
  (:require-macros [cljs.core.async.macros :as async])
  (:require [cljs.core.async :as async]
            [cljs.spec :as s]
            cljsjs.tableauwdc))

; Don't forget to set :elide-asserts to true in your production compiler settings!
(set! *assert* true)
(s/check-asserts true)

(defn enable-console-print!
  "Set *print-fn* to console.log and tableau.log"
  []
  (set! *print-newline* false)
  (set! *print-fn*
    (fn [& args]
      (.log js/tableau (apply str args))))
  (set! *print-err-fn* *print-fn*)
  nil)

(enable-console-print!)

;; Functions and objects of the WDC API v2 (http://tableau.github.io/webdataconnector/docs/api_ref)
(s/def ::id (s/and string? #(re-matches #"\w+" %)))
(s/def ::alias string?)
(s/def ::description string?)
(s/def ::incrementColumnId (s/or :nil nil? :empty #{""} :some ::id))
(s/def ::joinOnly boolean?)
(s/def ::filterable boolean?)
(s/def ::dataType #{"bool" "date" "datetime" "float" "int" "string"})
(s/def ::aggType #{"avg" "count" "count_dist" "median" "sum"})
(s/def ::columnRole #{"dimension" "measure"})
(s/def ::columnType #{"continuous" "discrete"})
(s/def ::geoRole #{"area_code" "cbsa_msa" "city" "congressional_district" "country_region" "county" "state_province" "zip_code_postcode"})
(s/def ::numberFormat #{"currency" "number" "percentage" "scientific"})
(s/def ::unitsFormat #{"billions_english" "billions_standard" "millions" "thousands"})
(s/def ::foreignKey (s/keys :req-un [::tableId ::columnId]))
(s/def ::columnInfo (s/keys :req-un [::dataType
                                     ::id]
                            :opt-un [::aggType
                                     ::alias
                                     ::columnRole
                                     ::columnType
                                     ::description
                                     ::geoRole
                                     ::numberFormat
                                     ::unitsFormat
                                     ::filterable
                                     ::foreignKey]))
(s/def ::columns (s/coll-of ::columnInfo))
(s/def ::tableInfo (s/keys :req-un [::columns
                                    ::id]
                           :opt-un [::alias
                                    ::description
                                    ::incrementColumnId
                                    ::joinOnly]))
(s/def ::tableInfos (s/coll-of ::tableInfo))
(s/def ::standardConnection (s/keys :req-un [::alias
                                             ::tables
                                             ::joins]))
(s/def ::standardConnections (s/coll-of ::standardConnection))
(s/def ::phase (s/nilable #{"auth" "interactive" "gatherData"}))
(s/def ::authType #{"none" "basic" "custom"})

(s/def ::state (s/keys :opt-un [::connection-data ::username ::password]))

(defprotocol IWebDataConnector
  "Web Data Connector protocol"
  (get-auth-type [this] "AuthType")
  (get-name [this] "Connection name")
  (get-table-infos [this] "Return a value conforming to `::tableInfos`")
  (get-standard-connections [this] "Return a value conforming to `::standardConnections`")
  (<get-rows [this table-info increment-value filter-values] "Return a `chan` holding arrays of rows")
  (shutdown [this] "Called when the current WDC phase ends. Return state which needs to be persisted (must conform to `::state`).")
  (init [this phase state] "Called when a new WDC phase is entered with `::state` saved at the end of previous phase.")
  (check-auth [this state done] "Called to check whether auth credentials are valid. Call `done` with: no args if OK, error str if not"))

;; Set up spec/assert to check return values of the IWebDataConnector implementation
;;
;; Note that we can't put spec/assert into the usual :post condition since its return value might be nil on a valid nilable value.
;; Putting s/valid? into the :post condition gives worse error messages when the spec fails.
;; Another option is spec/fdef with spec.test/check but that won't enforce post conditions.

(defn- -get-auth-type [w]
  (s/assert ::authType (get-auth-type w)))

(defn- -get-table-infos [w]
  (s/assert ::tableInfos (get-table-infos w)))

(defn- -get-standard-connections [w]
  (s/assert ::standardConnections (get-standard-connections w)))

(defn- -shutdown [w]
  (s/assert ::state (shutdown w)))

;; Stubs for methods that don't have checks
(def ^{:private true} -get-name get-name)
(def ^{:private true} -<get-rows <get-rows)
(def ^{:private true} -init init)
(def ^{:private true} -check-auth check-auth)

(defn get-phase
  "Returns the current WDC phase"
  []
  (s/assert ::phase (.-phase js/tableau)))

(def api-version "2.2")

(defn- tab-init [w callback]
  (let [phase (get-phase)
        state {:username (.-username js/tableau)
               :password (.-password js/tableau)
               :connection-data (some-> (.-connectionData js/tableau)
                                        (not-empty)
                                        (#(.parse js/JSON %))
                                        (js->clj :keywordize-keys true))}
        check-auth-cb (fn
                        ([] (callback))
                        ([error-str]
                         (do (println (str "aborting for auth: " error-str))
                             (.abortForAuth js/tableau error-str))))]
    (println (str "tab-init: entering phase: " phase))
    (set! (.-authType js/tableau) (-get-auth-type w))
    (set! (.-version js/tableau) api-version)
    (-init w phase state)
    (if (= phase "gatherData")
      (-check-auth w state check-auth-cb)
      (callback))))

(defn- tab-shutdown [w callback]
  (println (str "tab-shutdown: exiting phase: " (get-phase)))
  (let [{:keys [username password connection-data]} (-shutdown w)
        connection-data-str (->> connection-data
                                 clj->js
                                 (.stringify js/JSON))]
    (set! (.-connectionData js/tableau) connection-data-str)
    (set! (.-username js/tableau) username)
    (set! (.-password js/tableau) password))
  (callback))

(defn- tab-get-schema [w callback]
  (println "tab-get-schema")
  (callback (clj->js (-get-table-infos w))
            (clj->js (-get-standard-connections w))))

(defn- tab-get-data [w js-table callback]
  (println "tab-get-data")
  (let [js-table-info (aget js-table "tableInfo")
        increment-value (aget js-table "incrementValue")
        filter-values (js->clj (aget js-table "filterValues"))
        append-rows (aget js-table "appendRows")
        table-info (s/assert ::tableInfo (js->clj js-table-info :keywordize-keys true))
        <rows-chan (-<get-rows w table-info increment-value filter-values)]
    (async/go-loop [total 0]
      (if-let [rows (async/<! <rows-chan)]
        (do
          (append-rows (clj->js rows))
          (let [num (+ total (count rows))
                msg (str (:alias table-info) ": " num " rows fetched")]
            (println msg)
            (.reportProgress js/tableau msg)
            (recur num)))
        (do
          (println "tab-get-data DONE")
          (callback))))))

(defn register!
  "Registers the WDC"
  [w]
  {:pre [(satisfies? IWebDataConnector w)]}
  (let [connector
        (doto (.makeConnector js/tableau)
          (aset "init" (partial tab-init w))
          (aset "shutdown" (partial tab-shutdown w))
          (aset "getSchema" (partial tab-get-schema w))
          (aset "getData" (partial tab-get-data w)))]
    (.registerConnector js/tableau connector)))

(defn go!
  "Transitions from WDC 'interactive' phase to 'gather data' phase"
  [w]
  {:pre [(satisfies? IWebDataConnector w)]}
  (tab-shutdown w #(println "shutdown callback"))
  (set! (.-connectionName js/tableau) (-get-name w))
  (.submit js/tableau))
