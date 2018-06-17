(ns com.furkanbayraktar.json-schema.core-test
  (:require [clojure.test :refer :all]
            [com.furkanbayraktar.json-schema.core :as core]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.java.io :as io])
  (:import (java.io File)))

(s/def :test/field-1 string?)
(s/def :test/field-2 number?)
(s/def :test/field-3 boolean?)
(s/def :test/test-spec (s/keys :req-un [:test/field-1 :test/field-2]
                               :opt-un [:test/field-3]))

(defn create-temp-dir! []
  (let [temp-file (File/createTempFile "json-schema-temp" "")
        temp-dir  (io/file (.getParentFile temp-file) "json-schema-temp")
        _         (.mkdirs temp-dir)
        _         (.delete temp-file)]
    temp-dir))

(defn delete-temp-dir! [temp-dir]
  (doseq [file (reverse (file-seq temp-dir))]
    (.delete file)))

(deftest spec->spec-tools--normal-spec--return-spec-tools-spec
  (let [res (core/spec->spec-tools :test/test-spec)]
    (is (= (st/create-spec {:spec :test/test-spec})
           res))))

(deftest spec->spec-tools--spec-tools-spec--return-spec-tools-spec
  (let [test-spec (st/create-spec {:spec :test/test-spec})
        res (core/spec->spec-tools test-spec)]
    (is (= test-spec res))))

(deftest key-fn--case-type-is-pascal--return-pascal-converter
  (let [res (#'com.furkanbayraktar.json-schema.core/key-fn :testing-case-conversion)]
    (is (= "TestingCaseConversion" res))))

(deftest spec->json-schema--enabled-additional-props--return-json-schema-for-given-spec
  (let [json-schema (core/spec->json-schema-map :test/test-spec true)]
    (is (= {:properties {"field-1" {:type "string"}
                         "field-2" {:format "double" :type "number"}
                         "field-3" {:type "boolean"}}
            :required   ["field-1" "field-2"]
            :type       "object"}
           json-schema))))

(deftest spec->json-schema--disabled-additional-props--return-json-schema-for-given-spec-with-additional-props-false
  (let [json-schema (core/spec->json-schema-map :test/test-spec false)]
    (is (= {:properties           {"field-1" {:type "string"}
                                   "field-2" {:format "double" :type "number"}
                                   "field-3" {:type "boolean"}}
            :required             ["field-1" "field-2"]
            :type                 "object"
            :additionalProperties false}
           json-schema))))

(deftest spec->json-schema--using-spec-tools--return-json-schema-for-given-spec
  (let [test-spec   (st/spec (s/keys :req-un [:test/field-1 :test/field-2]
                                     :opt-un [:test/field-3]))
        json-schema (core/spec->json-schema-map test-spec true)]
    (is (= {:properties {"field-1" {:type "string"}
                         "field-2" {:format "double" :type "number"}
                         "field-3" {:type "boolean"}}
            :required   ["field-1" "field-2"]
            :type       "object"}
           json-schema))))

(deftest spec->json-schema--disabled-additional-props-and-using-spec-tools-with-additional-props--return-json-schema-for-given-spec-with-additional-props-true
  (let [test-spec   (st/spec (s/keys :req-un [:test/field-1 :test/field-2]
                                     :opt-un [:test/field-3]))
        test-spec   (assoc test-spec :json-schema/additionalProperties true)
        json-schema (core/spec->json-schema-map test-spec false)]
    (is (= {:properties           {"field-1" {:type "string"}
                                   "field-2" {:format "double" :type "number"}
                                   "field-3" {:type "boolean"}}
            :required             ["field-1" "field-2"]
            :type                 "object"
            :additionalProperties true}
           json-schema))))

(deftest spec->json-schema--disabled-additional-props-and-using-spec-tools--return-json-schema-for-given-spec-with-additional-props-false
  (let [test-spec   (st/spec (s/keys :req-un [:test/field-1 :test/field-2]
                                     :opt-un [:test/field-3]))
        json-schema (core/spec->json-schema-map test-spec false)]
    (is (= {:properties           {"field-1" {:type "string"}
                                   "field-2" {:format "double" :type "number"}
                                   "field-3" {:type "boolean"}}
            :required             ["field-1" "field-2"]
            :type                 "object"
            :additionalProperties false}
           json-schema))))

(deftest spec->name--spec-is-a-keyword--return-spec-name
  (let [res (core/spec->name :test/test-spec)]
    (is (= "TestSpec" res))))

(deftest spec->name--spec-is-a-spec-tools-spec--return-spec-name
  (let [test-spec (st/spec {:spec (s/keys :req-un [:test/field-1 :test/field-2]
                                          :opt-un [:test/field-3])
                            :name :st/test-spec})
        res       (core/spec->name test-spec)]
    (is (= "TestSpec" res))))

(deftest spec->name--spec-is-a-spec-tools-spec-with-normal-spec--return-spec-name
  (let [test-spec (st/spec {:spec :test/test-spec})
        res       (core/spec->name test-spec)]
    (is (= "TestSpec" res))))

(deftest spec->json-schema--test
  (let [res (core/spec->json-schema :test/test-spec true false)]
    (is (= "{\"type\":\"object\",\"properties\":{\"field-1\":{\"type\":\"string\"},\"field-2\":{\"type\":\"number\",\"format\":\"double\"},\"field-3\":{\"type\":\"boolean\"}},\"required\":[\"field-1\",\"field-2\"]}"
           res))))

(deftest spit-schemas--test
  (let [temp-dir  (create-temp-dir!)
        test-spec (st/spec {:spec (s/keys :req-un [:test/field-1 :test/field-2]
                                          :opt-un [:test/field-3])
                            :name :my-spec})
        output    (with-out-str
                    (core/spit-schemas! [:test/test-spec test-spec]
                                        {:output-path            (.getAbsolutePath temp-dir)
                                         :api-gateway-model-opts {}}))
        _         (delete-temp-dir! temp-dir)]
    (is (= (str "JSON Schema: Deleting old schemas.\n"
                "JSON Schema: Creating schema files.\n"
                "JSON Schema: Creating " (.getAbsolutePath temp-dir) "/TestSpec.json\n"
                "JSON Schema: Creating " (.getAbsolutePath temp-dir) "/MySpec.json\n"
                "JSON Schema: Done!\n")
           output))))
