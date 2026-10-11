# Fowler smell baseline (Standards)

Paste this list into the Standards sub-agent when spawning. From Fowler, *Refactoring* ch.3. Match against the scoped change only. These smells sit under the **Structure** Standards dimension.


**Rules**

- **Repo overrides** — a documented repo standard always wins; suppress smells the repo endorses.
- **Always a judgement call** — label as heuristic (e.g. "possible Feature Envy"), never a hard violation by smell alone.
- Skip anything tooling already enforces.

| Smell | What it is | How to fix |
| --- | --- | --- |
| **Mysterious Name** | Name doesn't reveal what the function, variable, or type does or holds | Rename; if no honest name comes, the design is murky |
| **Duplicated Code** | Same logic shape in more than one hunk or file in the change | Extract the shared shape; call it from both |
| **Feature Envy** | A method reaches into another object's data more than its own | Move the method onto the data it envies |
| **Data Clumps** | The same few fields or params keep travelling together | Bundle into one type; pass that |
| **Primitive Obsession** | A primitive or string stands in for a domain concept | Give the concept its own small type |
| **Repeated Switches** | Same `switch`/`if`-cascade on the same type across the change | Polymorphism, or one map both sites share |
| **Shotgun Surgery** | One logical change forces scattered edits across many files | Gather what changes together into one module |
| **Divergent Change** | One file or module edited for several unrelated reasons | Split so each module changes for one reason |
| **Speculative Generality** | Abstraction, parameters, or hooks for needs the spec doesn't have | Delete; inline until a real need shows |
| **Message Chains** | Long `a.b().c().d()` navigation the caller shouldn't depend on | Hide the walk behind one method on the first object |
| **Middle Man** | Mostly delegates onward | Cut it; call the real target direct |
| **Refused Bequest** | Subclass/implementer ignores or overrides most of what it inherits | Drop inheritance; use composition |
