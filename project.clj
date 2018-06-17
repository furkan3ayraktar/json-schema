(defproject com.furkanbayraktar/json-schema "0.1.0"
  :description "A library to generate JSON schemas from Clojure specs"
  :url "https://github.com/furkan3ayraktar/json-schema"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url  "https://github.com/furkan3ayraktar/json-schema"}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo/",
                                    :username      :env/clojars_username,
                                    :password      :env/clojars_password,
                                    :sign-releases false}]]
  :dependencies [[camel-snake-kebab "0.4.0"]
                 [cheshire "5.8.0"]
                 [commons-io/commons-io "2.6"]
                 [metosin/spec-tools "0.7.0"]
                 [org.clojure/clojure "1.9.0"]])
