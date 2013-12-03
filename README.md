Caribou Plugin
==============

A simple lightweight coordinator for self contained caribou features.

This clojure project defines a protocol `caribou.plugin.protocol.CaribouPlugin`
that allows you to define a configuration accessor, a configuration processor, a required caribou migration, any number of helpers, request handlers, and pages; and ensures that they are initialized in the proper order and integrated into a caribou project.
