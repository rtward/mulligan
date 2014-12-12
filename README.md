# mulligan

A small clojure library to retry a bit of code if it fails.

Inspired heavily by the perl [Retry](http://search.cpan.org/~tjc/Retry-1.01/lib/Retry.pm) module

## Usage

Basic usage

    (retry (my-function-call))

This will retry your code with the default options.  Three retries, only retrying when an exception is thrown.


Advanced usage

    (retry :tries   5
           :retry-on-nil     true
           :retry-on-false   true
           :on-success       (fn [result] (println “Success))
           :on-failure       (fn [result] (println “Non-final failure))
           :on-final-failure (fn [result] (println “Final failure”))
           :wait             100
           :backoff          :linear
      (println “Running your code here”)
      (my-function-call)
      (println “Code is done))

You can see the different options supported by the library above.

The success callback will be given the return value of your code.
The failure callbacks will be given the exceptions thrown by your code, or NilResult or FalseResult if you’ve selected those options.

## License

Copyright © 2014 Robert Ward

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
