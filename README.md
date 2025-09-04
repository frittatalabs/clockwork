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

## Why Clockwork?
- **Practical today** — drop it into real projects without ceremony
- **Nail your Logic** — Trace behavior and flow in a dry run to verify correctness
- **Elegant & fun** — APIs that feel like Clojure, but with extra polish
- **Extreme composition** — swap any gear for another to change behavior
- **Dry‑run mode** — trace and inspect your flow without running side effects
- **Protocol‑free** — “gear” is just a metaphor; you can plug in anything

---

## The Big Idea

At its heart, Clockwork is powered by a small set of **reduce‑like drivers**.  
A *gear* is just a unit of logic with clear inputs and outputs. You can:

1. **Use curated gears** from the public API — drop‑in forms and macros that feel like `let`, `for`, or `fn` but with extra abilities
2. **Compose gears** to make your own flavors
3. **Write your own driver** for fully custom behavior

Think of it like LEGO® for control flow: start with ready‑made bricks, then start carving your own

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
Works like `let`, but with built‑in nil‑guarding. You can start using it in minutes.

---

### 2. Zipping Streams — `let-zip`
Like `for`, but walks columns in lockstep.

```clojure
(take 5
 (seqs/let-zip
   [n     (iterate inc 1)
    badge ["🔥" "✨" "🎯" "🚀" "💎"]
    label    [:fire :sparkle :bullseye :rocket :diamond]]
   (->> label name clojure.string/capitalize (str "Column " n ": " badge " "))))
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
  [x (log/with-note 3 "Assigning 3 to x")
   y (- 99 x)
   z (let [xy (* x y)]
       (log/with-note xy (str "x*y = " xy)))]
  (+ x y z))
;; Prints notes, returns 387
```

Your flow becomes self‑documenting — perfect for debugging complex pipelines.

---

### 4. Stateful Cells — `let-cell`
Also a workshop toy, but demonstrates inline mutable state without losing functional clarity. For fun.

```clojure
(mem/let-cell
 "Initial State"
 [initial (mem/put {:x {:y {:z "deep value"}}})
  z       (mem/get :x :y :z)
  _       (mem/put (str initial " ==> Done")
 z) ;; prints "Final state:  Initial State ==> Done"
;; => "deep value"
```

---

### 5. Ridiculously composable expressive power
For example, in the workshop, you can build your own parsing pipeline.

```clojure
(reader/read " ,  Word! "
             reader/chew-whitespace
             reader/mark
             reader/end-of-word
             reader/emit)
;; => "Word!"
```
## 6. **Developer Delight**  
APIs designed to feel *fun* — fluent, readable, and metaphor‑rich.

---

## 7. **Future‑Proof Extensibility**  
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
Crafted in Seattle & Milano 🇺🇸🇮🇹 (well, Bonney Lake & Cremona - close enough)
