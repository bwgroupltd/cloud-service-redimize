(defproject audiogum/redimize "0.1.10"
  :description "two level memoize redis caching in clojure"
  :url "https://github.com/bwgroupltd/cloud-service-redimize"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.taoensso/carmine "3.3.2"]
                 [org.clojure/core.memoize "1.0.257"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "releases"]
                  ["change" "version" "leiningen.release/bump-version" "patch"]
                  ["vcs" "commit"]
                  ["vcs" "push" "origin" "main"]]

  :plugins [[s3-wagon-private "1.3.4"]]

  :repositories {"releases" {:url           "s3p://repo.bowerswilkins.net/releases/"
                             :no-auth       true
                             :sign-releases false}}

  :profiles {:dev {:dependencies [[audiogum/redis-embedded-clj "0.0.4"]]}}

  :repl-options {:init-ns redimize.core})
