# json-schema

A library to generate JSON schemas from Clojure specs.
It provides utility functions to convert both Clojure specs
and [spec-tools](https://github.com/metosin/spec-tools) specs
into [JSON schema](http://json-schema.org).

[![Clojars Project](https://img.shields.io/clojars/v/com.furkanbayraktar/json-schema.svg)](https://clojars.org/com.furkanbayraktar/json-schema)
[![GitHub license](https://img.shields.io/github/license/furkan3ayraktar/json-schema.svg)](LICENSE)
[![Circle CI](https://circleci.com/gh/furkan3ayraktar/json-schema/tree/master.svg?style=shield)](https://circleci.com/gh/furkan3ayraktar/json-schema/tree/master)

## Installation

To include `json-schema`, add the following to your
`:dependencies`:

    [com.furkanbayraktar/json-schema "0.1.0"]

## Usage

json-schema provides a function called `spit-schemas!` to write
JSON schemas to individual files. By default, it uses `schemas`
folder in your project root. Here is a sample use case:

```clojure
(ns json-schema-test.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.furkanbayraktar.json-schema.core :as js]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]))

;; Clojure specs

(s/def :test/field-1 (s/and string? #(not (str/blank? %))))
(s/def :test/field-2 pos-int?)
(s/def :test/field-3 boolean?)

(s/def :test/test-spec (s/keys :req-un [:test/field-1 :test/field-2]
                               :opt-un [:test/field-3]))

;; spec-tools specs

(def my-integer? (assoc (st/spec integer?)
                   :description "It's an int"
                   :name :test/my-integer))

(def not-blank-string-regex #"^.*\S.*$")

(def not-blank-string?
  (st/spec {:spec :test/field-1
            :type :string
            :description "A non blank string checked by clojure.string/blank?"
            :json-schema/pattern (str not-blank-string-regex)}))

;; spec-tools data spec

;; spec-tools data spec does not add :name to Spec record, so it needs to
;; be added manually.
(def test-data-spec
  (assoc
    (ds/spec {:name :test/data-spec
              :spec {:id my-integer?
                     :name :test/field-1
                     :description not-blank-string?
                     :birthday (s/int-in 1900 2018)}})
    :name :test/data-spec))

;; Function to be called through leiningen (or anywhere else) when needed
(defn generate-schemas! []
  (js/spit-schemas! [:test/test-spec my-integer? test-data-spec]))
```

Once you have a function like `generate-schemas!` that calls spit-schemas!,
you can define an alias in your `project.clj` file in order to generate
schema files on your continuous integration pipeline or any time you need
outside of the project. You can learn more about creating aliases on
[leiningen documentation](https://github.com/technomancy/leiningen/blob/master/doc/PLUGINS.md#not-writing-a-plugin-无为).

```clojure
(defproject json-schema-test "0.1.0-SNAPSHOT"
  :description "Test project for json-schema"
  :dependencies [[com.furkanbayraktar/json-schema "0.1.0"]
                 [metosin/spec-tools "0.7.0"]
                 [org.clojure/clojure "1.9.0"]]

  ;; Alias that calls generate-schemas! function to generate
  ;; JSON schemas from your specs!

  :aliases {"generate-schemas" ["run" "-m" "json-schema-test.core/generate-schemas!"]})
```

The alias named `generate-schemas` will let you run:

    $ lein generate-schemas

on your command line. It will generate an output like this:

```
$ lein generate-schemas

JSON Schema: Deleting old schemas.
JSON Schema: Deleting /Users/furkan/test-json-schema/schemas/TestSpec.json
JSON Schema: Deleting /Users/furkan/test-json-schema/schemas/MyInteger.json
JSON Schema: Creating /Users/furkan/test-json-schema/schemas/DataSpec.json
JSON Schema: Creating schema files.
JSON Schema: Creating /Users/furkan/test-json-schema/schemas/TestSpec.json
JSON Schema: Creating /Users/furkan/test-json-schema/schemas/MyInteger.json
JSON Schema: Creating /Users/furkan/test-json-schema/schemas/DataSpec.json
JSON Schema: Done!
```

If you open `DataSpec.json` file under `schemas` folder, you will see:

```json
{
  "type" : "object",
  "properties" : {
    "id" : {
      "type" : "integer",
      "description" : "It's an int",
      "title" : "test/my-integer"
    },
    "name" : {
      "type" : "string"
    },
    "description" : {
      "type" : "string",
      "description" : "A non blank string checked by clojure.string/blank?",
      "pattern" : "^.*\\S.*$"
    },
    "birthday" : {
      "allOf" : [ {
        "type" : "integer",
        "format" : "int64"
      }, {
        "minimum" : 1900,
        "maximum" : 2018
      } ]
    }
  },
  "required" : [ "id", "name", "description", "birthday" ],
  "title" : "test/data-spec"
}
```

You can find different options you can pass to `spit-schemas!` function
in the next section or in the documentation of function.

json-schema contains other utility functions like `spec->json-schema` and
`spec->json-schema-map`. `spec->json-schema` returns a JSON schema string
for given spec. Similarly, `spec->json-schema-map` returns a Clojure map
of the JSON Schema, if you prefer to convert JSON string later on.

```clojure
(spec->json-schema :test/test-spec)
```

evaluates to:

```clojure
=>
"{
   \"type\" : \"object\",
   \"properties\" : {
     \"field-1\" : {
       \"type\" : \"string\"
     },
     \"field-2\" : {
       \"type\" : \"integer\",
       \"format\" : \"int64\",
       \"minimum\" : 1
     },
     \"field-3\" : {
       \"type\" : \"boolean\"
     }
   },
   \"required\" : [ \"field-1\", \"field-2\" ]
 }"
```

Similarly,

```clojure
(spec->json-schema-map :test/test-spec)
```

evaluates to:

```clojure
=>
{:type "object",
 :properties {"field-1" {:type "string"},
              "field-2" {:type "integer", :format "int64", :minimum 1},
              "field-3" {:type "boolean"}},
 :required ["field-1" "field-2"]}
```

## Options

```clojure
(defn spit-schemas!
  ([specs {:keys [delete-old-schemas?             ; If false, does not delete old schema files before creating new.
                  output-path                     ; Defines the output folder for generated schema files.
                  additional-props?               ; If false, adds adds additionalProperties field with false value to schema.
                  convert-name?                   ; If false, keeps spec name as file name, otherwise converts it
                                                  ; to [Pascal Case](https://en.wikipedia.org/wiki/Camel_case)
                  pretty?]                        ; If false, skips prettifying JSON string.
           :or   {delete-old-schemas? true
                  output-path         "schemas"
                  additional-props?   true
                  convert-name?       true
                  pretty?             true}}]

  ; ...
```

```clojure
(defn spec->json-schema
  ([spec
    additional-props? ; If false, adds additionalProperties field with false value to schema.
    pretty?]          ; If false, skips prettifying JSON string.

  ; ...

  ([spec]
   (spec->json-schema spec true true)))
```

```clojure
(defn spec->json-schema-map
  ([spec
    additional-props?] ; If false, adds additionalProperties field with false value to schema.

  ; ...

  ([spec]
   (spec->json-schema-map spec true)))
```

Reference to [additionalProperties](http://json-schema.org/latest/json-schema-validation.html#rfc.section.6.5.6)
can be found in the link.

## License

Copyright © 2018 Furkan Bayraktar

Distributed under the Eclipse Public License, the same as Clojure.
