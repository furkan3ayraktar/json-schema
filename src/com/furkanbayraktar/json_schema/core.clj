(ns com.furkanbayraktar.json-schema.core
  (:require [camel-snake-kebab.core :as c]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [spec-tools.core :as st]
            [spec-tools.json-schema :as js])
  (:import (org.apache.commons.io FilenameUtils)
           (java.io File)))

(defmulti spec->spec-tools (fn [spec] (class spec)))

(defmethod spec->spec-tools spec_tools.core.Spec [spec]
  spec)

(defmethod spec->spec-tools clojure.lang.Keyword [spec]
  (st/create-spec {:spec spec}))

(defn spec->json-schema-map
  "Converts given spec into a Clojure map representing a JSON schema.

   additional-props? -> If false and spec does not define :json-schema/additionalProperties
                        field, adds additionalProperties field as false to schema. By default,
                        it is set to true, which means does not add anything to schema."
  ([spec additional-props?]
   (let [spec (spec->spec-tools spec)
         spec (if (and (not additional-props?)
                       (nil? (:json-schema/additionalProperties spec)))
                (assoc spec :json-schema/additionalProperties false)
                spec)]
     (js/transform spec)))
  ([spec]
   (spec->json-schema-map spec true)))

(defn ^:private file? [^File file]
  (.isFile file))

(defn ^:private file-ext [^File file]
  (FilenameUtils/getExtension (.getName file)))

(defn ^:private delete-old-schemas! [output-folder]
  (when (.exists output-folder)
    (println "JSON Schema: Deleting old schemas.")
    (let [files (filter #(and (= "json" (file-ext %))
                              (file? %))
                        (file-seq output-folder))]
      (doseq [file files]
        (println "JSON Schema: Deleting" (.getAbsolutePath file))
        (io/delete-file file)))))

(defn ^:private key-fn [k]
  (c/->PascalCaseString k :separator \-))

(defmulti spec->name (fn [spec] (class spec)))

(defmethod spec->name spec_tools.core.Spec [spec]
  (if-let [spec-name (st/spec-name spec)]
    (key-fn spec-name)
    (throw (ex-info "Given spec-tools Spec does not have a name associated with it."
                    {:causes #{:spec-tools-spec-name :name-is-nil}}))))

(defmethod spec->name clojure.lang.Keyword [spec]
  (key-fn spec))

(defn spec->json-schema
  "Converts given spec into a String representing a JSON schema.

   additional-props? -> If false and spec does not define :json-schema/additionalProperties
                        field, adds additionalProperties field as false to schema. By default,
                        it is set to true, which means does not add anything to schema.

   pretty?           -> If false, skips prettifying JSON string. By default, it is set to
                        true."
  ([spec additional-props? pretty?]
   (let [json-schema-map (spec->json-schema-map spec additional-props?)]
     (cheshire/generate-string json-schema-map {:pretty pretty?})))
  ([spec]
   (spec->json-schema spec true true)))

(defn spit-schemas!
  "Converts given specs into JSON schemas and writes them into separate JSON files.

   delete-old-schemas? -> If false, does not delete old schema files before creating new.
                          Default value is true.

   output-path         -> Defines the output folder for generated schema files. Default value
                          is 'schemas'.

   additional-props?   -> If false and spec does not define :json-schema/additionalProperties
                          field, adds additionalProperties field as false to schema. By default,
                          it is set to true, which means does not add anything to schema.

   convert-name?       -> If false, keeps spec name as file name, otherwise converts it to
                          PascalCase. Example :test/test-spec becomes TestSpec.json.

   pretty?             -> If false, skips prettifying JSON string. By default, it is set to
                          true."
  ([specs {:keys [delete-old-schemas?
                  output-path
                  additional-props?
                  convert-name?
                  pretty?]
           :or   {delete-old-schemas? true
                  output-path         "schemas"
                  additional-props?   true
                  convert-name?       true
                  pretty?             true}}]
   (let [output-folder (io/file output-path)]
     (when delete-old-schemas? (delete-old-schemas! output-folder))
     (.mkdirs output-folder)
     (println "JSON Schema: Creating schema files.")
     (doseq [spec specs]
       (let [name            (if convert-name? (spec->name spec) (name name))
             output-file     (io/file output-folder (str name ".json"))
             json-schema-map (spec->json-schema-map spec additional-props?)
             output-json     (cheshire/generate-string json-schema-map {:pretty pretty?})]
         (println "JSON Schema: Creating" (.getAbsolutePath output-file))
         (spit output-file output-json)))
     (println "JSON Schema: Done!")))
  ([specs]
   (spit-schemas! specs nil)))
