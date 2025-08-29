(ns clockwork.workshop.reader
  (:refer-clojure :exclude [read])
  (:require [clockwork.gears :as gears :refer [->clockwork mesh simple]]))

;; We will design the logic first: no objects, no records, not even any protocols. Pure logic

;; Basic op set for our reader

;; current: retrieves the current character
;; mark: puts the "mark" at the current character
;; emit: get the string between mark and current
;; seek(chars, bool): seeks the next occurence (if true?) or past (if false?) all occurences of the character set specified

;; If you are not in demo mode, like I am here, feel free to declare a protocol up front:
;;  what I'm doing isn't necessarily optimal for real use - this is instructional.

;; We will introduce a real object and protocols later. For this instructional (I hope) example
;; let's do things in reverse order, to demonstrate how, and that we can be as flexible as we need
;; - no protocols required: any object can work

;; in this case, our "object" is just a loose set of vectors and keyword tags

;; First, some primitives that will generate our op-"object"s
(def emit
  "Get text between mark and current"
  [:emit])

;; these primitives are not fns, so we can't just invoke them if we enqueue them as-is. Which I want to do.
(def mark
  "Put the mark at the :current index"
  [:mark])

(defn seek
  "Generate a seek instruction. Supported are a string (at most one), or a category specified by keyword.
  Let's support: :whitespace and :endings, and a string of arbitrary characters - must be specified as a coll
  The boolean flag dictates whether we stop on finding a match, or whether we would seek until no-match - aka chew"
  [targets stop?]
  [:seek targets stop?])

(def chew-whitespace
  "Finds the next non-whitespace character in the string, also ignoring commas as whitespace"
  (seek [:whitespace ","] false)) ; I think false should mean we assume we are on a whitespace, seeking the end

(def end-of-word
  "Find the end of the current word"
  (seek [:whitespace :endings] true))

(declare read-table)

(def read-next-form
  "Read the next form that our reader can find"
  (mesh chew-whitespace
        ;; current ; you know what? I changed my mind, let's remove this - and just have chew/seek send its dest
        read-table))

(def comment-line
  "After reading a \\; character, we will want to find the end of line; then we might as well grab the next form"
  (mesh (seek ["\n\r"] true)
        read-next-form))

(def end-of-string
  "Find the end of the string: look for the next double-quote (that's not backslash-escaped)"
  (mesh (seek ["\"\\"] true)
        #(simple (if 3 4)))) ; Clockwork is flexible. Let's just mesh a lambda that accepts that char as input

;; this one needs input from the previous step, so it'll have to be enqueued as a fn. We will check for this when we decomplect
(defn read-table
  "Decide what to do based on the last-read character: dispatch our next command based on the current char"
  [ch]
  (case \; comment-line
        \" end-of-string
        \' end-of-word))

;; we can drive a Clockwork by simply destructuring it - but first we will need an actual
;; state object, as the above is pure declarative logic. Think of it as "reader forms" or commands

;; we will create a protocol now, after-the-fact :)
;; you might instead have a protocol designed up front, and might not use vectors with keywords
;; what if... you just used the protocol methods themselves instead of keywords, and just - you know what?
;; I don't want to be confusing. Let's keep it gentle and just go on

(defprotocol State ;; "Inspired by" the tags in our declarative vectors above
  (current* [_])
  (emit* [_])
  (mark* [_])
  (seek* [_ targets stop-or-chew?]))

;; The last thing we will do is make our state. Why last? Keeps things safe! State is messy and gets everywhere
(defrecord StringState [backing mark index]
  ;; This is simply an immutable/persistent "state" on a string
  State
  (current* [_]
    (when (< index (count backing)) (nth backing index)))
  (emit* [_]
    (subs backing mark index))
  (mark* [this]
    (assoc this :mark index))
  (seek* [this targets invert?]
    (let [chars (first (filter string? targets))
          whitespace (when (some #{:whitespace} targets) "\\s")
          endings (when (some #{:endings} targets) ";\"{}()^@`,\\[\\]") ;; TODO: move down to `decomplect` - why not? Decide as late as possible if you like
          patter-str (str "[" (when (not invert?) "^")
                          whitespace chars endings "]")
          pattern (re-pattern patter-str)
          match (re-matcher pattern backing)]
      (.region match index (count backing))
      (when (.find match)
        (assoc this :index (.start match))))))
      
;; let's try running it through its paces - fun. I have a feeling this will be nice to have:
(defmacro ^:private resubmit [decomplected queue-expr]
  `(if-let [[->next# & queue#] (seq ~queue-expr)] ;; todo: really need the seq?
     (->clockwork (if (fn? ->next#)
                    (->next# ~decomplected)
                    ->next#)
                  queue#)
     (simple ~decomplected)))

;; this function encapsulates the *control* aspect. We have logic in the functions above, pure declarative.
;; we have the stateful object - pure side effects. Side effects start feeling more simple when you keep
;; logic and control clean and out of the picture.
(defn decomplect
  "This drives our string reader, keeps our state and uses it via its protocol.
  Brings the reader to life, powers it. We can safely deconstruct Clockworks as they are a fixed
  structure that never changes:
  {:keys [flow payload]}, where the flow is
  {:keys [simple complected]} and complected is
  {:keys [gear chain]}
  Later on, they will be invokable as a clojure.lang.IFn, follow a protocol you can use, and so on. For now, we destructure :)"
  [{:keys [flow payload]} state]
  ;; In the future, we will be able to run a clockwork with fancy Mainspring functions and an invoke implementation via IFn. For now, I have to put that on hold:
  ;; it's taking too much attention from more important (but less fun) things :( - For performance reasons(!), we go with explicit destructure + a hot loop :)
  (case flow
    :simple payload
    :complected
    ;; here we destructure our custom reader method, since clockwork doesn't know how
    (let [{[op seek-targets find-or-chew?] :gear
           ;; destructuring, we are in "low-level" mode so we must be sure not to lose our enqueued steps. If you invoke the Clockwork, it handles this for you
           ->next :chain} payload]
      ;; Above, we wrote ops that aren't a fn, and we want to just mesh those as-is. It will be nice, just creates
      ;; a tiny bit of extra work for us here. It'll be worth it, though - and less work than invoking, which wraps the queue management in a small closure
      (case op
        ;; simple mapping from opcode to our value+effects
        :seek (let [seeking (seek* state seek-targets find-or-chew?)]
                (recur (resubmit (current* seeking) ->next) seeking))
        :emit (recur (resubmit (emit* state) ->next) state)
        :mark (recur (resubmit nil ->next) (mark* state))
        :current (recur (resubmit (current* state) ->next) state)
        {:unknown-op-code payload}))
    {:unknown-control {:flow flow :payload payload :state state}}))

(defn read
  "Run reader commands over a string. Commands can be step functions that take input from
  the previous step, or simple operations"
  [input reader & commands]
  (decomplect (->clockwork reader commands) (->StringState input 0 0)))
