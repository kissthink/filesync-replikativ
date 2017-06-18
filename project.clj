(defproject filesync-replikativ "0.1.0-SNAPSHOT"
  :description "Filesyncing over replikativ."
  :url "https://github.com/replikativ/filesync-replikativ"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [io.replikativ/replikativ "0.2.2"]
                 [com.fzakaria/slf4j-timbre "0.3.1"]
                 [es.topiq/clj-ipfs-api "1.2.4-SNAPSHOT"]]

  :main filesync-replikativ.core)
