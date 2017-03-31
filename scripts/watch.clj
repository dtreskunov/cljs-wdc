(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'cljs-wdc.core
   :output-to "out/cljs_wdc.js"
   :output-dir "out"})
