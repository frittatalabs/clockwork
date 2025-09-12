# Clockwork
**Composable control flow for Clojure — precision‑engineered, endlessly hackable.**

Clockwork works out of the box — a lightweight engine for building fluent, composable control flows in Clojure. Start with drop‑in forms that feel like let or for, then mix and match gears to orchestrate stateful, branching, or asynchronous workflows.
```clojure
(require '[clockwork.gears.guard :as guard])

(guard/let?
  [x nil
   y (inc x)]
  (+ x y))
;; => nil
```
Pipelines, interpreters, reactive systems — if it has moving parts, Clockwork lets you model it as a set of **gears** that mesh cleanly and predictably.

It grew out of an obsession:
> *What if logic, state, and control flow could be separated so cleanly that you could swap one without touching the others?*

The answer worked so well, it became its own machine.

---

## About Clockwork
- **Practical today** — drop it into real projects without ceremony
- **Nail your Logic** — develop behavior and flow iteratively, with super easy mocking
- **Elegant & fun** — APIs that feel like Clojure, with extra polish
- **Extreme composition** — swap gears to change behavior
- **Dry‑run mode** — trace and inspect your flow without running side effects
- **Protocol‑free** — “gear” is just a metaphor; you can plug in anything

---

## The Big Idea

At its heart, Clockwork is powered by a desire to be immediately useful. The goals are:
* Provide a "polished, curated" API (public functions and macros) in `clockwork.core` and aim for familiar-feeling forms that work like `let`, `for`, or `fn` but with extra abilities. Usable out-of-the-box
* Intermediate users can customize behavior by parameterizing the underlying forms, and building bespoke functions and macros on top of the core layers. Built to suit your own needs
* Advanced users can **compose gears** to make combinations that blend their ablilites. Think of it like LEGO meets Voltron.
* Maestro level: **Write your own driver** for fully custom behavior
At its heart, Clockwork is powered by a small set of reduce‑like drivers: a "gear" is just a unit of logic with clear inputs and outputs.

---

## Quick Tour

### 1. Guarded Bindings — `let?`
Stop nils before they spread. Drop it into real code today — no special protocols, no boilerplate

```clojure
(guard/let?
  [x 6
   y (inc x)]
  (+ x y))
;; => 13

(guard/let?
  [x nil
   y (inc x)]
  (+ x y))
;; => nil
```
Works like `let`, but with built‑in nil‑guarding. You can start using it instantly

---

### 2. Zipping Streams — `let-zip`
Like `for`, but walks columns in lockstep.

```clojure
(take 5
 (seqs/let-zip
   [n     (iterate inc 1)
    badge ["🔥" "✨" "🎯" "🚀" "💎"]
    label    [:fire :sparkle :bullseye :rocket :diamond]]
  (str "Column " n ": " badge " " (->> label name clojure.string/capitalize))))
;; => ("Column 1: 🔥 Fire"
;;     "Column 2: ✨ Sparkle"
;;     "Column 3: 🎯 Bullseye"
;;     "Column 4: 🚀 Rocket"
;;     "Column 5: 💎 Diamond")
```

---

### 3. **Built‑in Introspection & Debugging**  
Just a workshop toy, but you can trace execution by attaching notes to values as they flow. 

```clojure
(log/let-notes
  [x (log/with-note 3 "Assigning 3 to x.")
   y (- 99 x)
   z (let [xy (* x y)]
       (log/with-note xy (str "And x*y = " xy)))]
  (+ x y z)) ;; Prints "Notes collected: [Assigning 3 to x. And x*y = 288]"
;; => 387
```

Your flow becomes self‑documenting — perfect for debugging complex pipelines.

---

### 4. Stateful Cells — `let-cell`
Also a workshop toy, but demonstrates inline mutable state without losing functional clarity. For fun.

```clojure
(mem/let-cell
 "Initial State"                                ;; pass in anything
 [initial (mem/put {:x {:y {:z "deep value"}}}) ;; put returns previous state
  z       (mem/get :x :y :z)
  desc    (str "z is a " z)
  _       (mem/put (str initial " ==> Done")
 desc) ;; prints "Final state:  Initial State ==> Done"
;; => "z is a deep value"
```

---

### 5. Ridiculously composable expressive power
For example, in the workshop, you can build your own parsing pipeline

```clojure
(reader/read " ,  Word! "
             reader/chew-whitespace
             reader/mark
             reader/end-of-word
             reader/emit)
;; => "Word!"
```
### **Developer Delight**
APIs designed to feel *fun* — fluent, readable, and metaphor‑rich.

### **Future‑Proof Extensibility**
Minimal, orthogonal primitives make it easy to add new gears without breaking old ones.

---

## Coming Soon
- `tap>` gear  
- `let-default` gear  
- Namespace analyzer gear  
- Macro trace gear  
- Abstract VM gear  

---

Clockwork isn’t just a library — it’s a **workshop**: start with the polished tools, then open the hood and build your own.  
It’s for people who care about **clarity**, **composability**, and **delight** in their control flow.

---

**MIT License**  
Crafted in Seattle & Milano 🇺🇸🇮🇹
