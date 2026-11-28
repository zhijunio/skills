# Technique selection

Start from a criterion or material risk and the observation that would distinguish correct behavior from failure. Match the method to the question.

| Risk or behavior | Candidate technique |
| --- | --- |
| Named business outcome | Example-based acceptance test |
| Input classes or limits | Equivalence partitions and boundary cases |
| Interacting business conditions | Decision table or justified combinatorial selection |
| Lifecycle and invalid actions | State-transition or model-based tests |
| Invariants over many inputs | Property-based tests with reproducible counterexamples |
| Independent versions or implementations | Differential or contract tests |
| Parser or broad untrusted input | Bounded fuzzing with explicit invariants |
| Real persistence, protocol or runtime semantics | Focused integration tests with compatible dependencies |
| Critical assembled journey | End-to-end test through public behavior |
| Subjective or poorly understood experience | Structured exploratory or human evaluation |
| Capacity, latency or resource constraint | Controlled measurement with defined workload |
| Timeout, restart, duplication or recovery | Deterministic fault or concurrency scenarios |

Choose only techniques that add evidence. A critical rule can often be checked at a small scope; retain broader checks for distinct composition or environment risks. Use positive and negative cases, including denied operations and unchanged state where applicable. Test observable outcomes rather than incidental private calls.

For generated inputs record domain, seed and counterexample. For performance and recovery record conditions, expected measure and source of threshold. Do not equate absence of crashes with correctness, or a simulated dependency with verified real semantics.
