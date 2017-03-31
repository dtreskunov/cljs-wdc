(defproject tableau/cljs-wdc "0.1.0"
  :description "ClojureScript wrapper for the Tableau Web Data Connector library"
  :url "https://github.com/dtreskunov/cljs-wdc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/clojurescript "1.9.473" :scope "provided"]
                 [org.clojure/core.async "0.3.442"]
                 [cljsjs/tableauwdc "2.2.0-1"]]
  :plugins [[lein-cljsbuild "1.1.5"]]
  :cljsbuild {:builds
              [{:id "none"
                :source-paths ["src"]
                :compiler
                {:asset-path "target/none"
                 :optimizations :none
                 :output-dir "target/none"
                 :output-to "target/none.js"}}
               {:id "advanced"
                :source-paths ["src"]
                :compiler
                {:asset-path "target/advanced"
                 :optimizations :advanced
                 :elide-asserts true
                 :output-dir "target/advanced"
                 :output-to "target/advanced.js"
                 :pretty-print false}}]}

  ;; credentials will be looked up in environment variables: LEIN_USERNAME and LEIN_PASSWORD
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :username :env
                              :password :env}]
                 ["snapshots" {:url "https://clojars.org/repo"
                               :username :env
                               :password :env}]]
  :min-lein-version "2.7.1")
