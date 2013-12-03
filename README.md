Caribou Plugin
==============

This clojure project defines a protocol `caribou.plugin.protocol.CaribouPlugin`
that allows you to define a configuration accessor, a configuration processor, a required caribou migration, any number of helpers and request handlers, and ensures that they are initialized in the proper order and integrated into a caribou project.
