# cljs-wdc

[![Clojars Project](https://img.shields.io/clojars/v/cljs-wdc.svg)](https://clojars.org/cljs-wdc)
[![Build Status](https://travis-ci.org/dtreskunov/cljs-wdc.svg?branch=master)](https://travis-ci.org/dtreskunov/cljs-wdc)

ClojureScript wrapper for the Tableau Web Data Connector library.

## Overview

This library attempts to simplify development of Tableau Web Data Connectors using ClojureScript.

* [Tableau WDC Overview](https://tableau.github.io/webdataconnector/docs/)
* [Tableau WDC API](https://tableau.github.io/webdataconnector/docs/api_ref.html)

## Usage

1. It is recommended to start by using Leiningen to create a project from the `figwheel` template:

    lein new figwheel my-new-wdc
    
2. Add this library to `project.clj`
3. Require it from `core.cljs`
4. Create an object that implements the `tableau.cljs-wdc.core/IWebDataConnector` protocol
5. Register the connector by calling `tableau.cljs-wdc.core/register!`
6. *IMPORTANT*: `index.html` should not include a link to `tableauwdc.js` since it is provided by
[cljsjs/tableauwdc](https://github.com/cljsjs/packages/tree/master/tableauwdc)!

Look at the [dhis2-wdc](https://github.com/dtreskunov/dhis2-wdc) project for an example of a working
WDC written using this library.

## License

Copyright © 2017 Tableau

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
