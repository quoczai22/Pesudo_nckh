# QUESTIONS

This file lists unresolved points after reading the current repository snapshot. These are not guesses. They are explicit open issues or evidence gaps.

## 1. Where are the default datasets referenced by `MainCUP`?

Evidence:

- [resolveDefaultDataFile](D:/PESUDO_KQ/P1/src/main/MainCUP.java:57) checks:
  - `dataset/SIGN.txt`
  - fallback `dataset/paper_example.txt`
- In the analyzed snapshot, I observed:
  - `dataset/clickstream/FIFA.txt`
  - empty `dataset/sequence/`
- I did not observe `dataset/SIGN.txt` or `dataset/paper_example.txt`.

Why this matters:

- The repository is not directly reproducible through the default entry point as-is.

## 2. Is `src/Output/Detailed_Execution_Trace.txt` intentionally versioned?

Evidence:

- There is a trace file inside `src/Output/`.
- This is structurally different from the main generated output directory `Output/`.

Why this matters:

- It looks like a generated artifact mixed into source.
- If intentional, we need to know why.

## 3. Is the current loader intentionally clickstream-only even when reading SPMF-like input?

Evidence:

- [parseTokenStreamSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:118) ignores `-1` separators and flattens the whole sequence into `List<Integer>`.

Why this matters:

- For clickstream data, this is acceptable because each event contains one item.
- For general SPADE-style sequential databases with multi-item itemsets, this is not behaviorally equivalent.

## 4. Is the equal-relation branch intentionally unused in CUP?

Evidence:

- `IDList_ArrayProjSimple` implements [equalLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:195) and [laterLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:264).
- But [AlgoCUP.joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267) always calls `join(..., false, ...)`.

Why this matters:

- This makes the current mining path strictly after-relation only.
- That fits clickstream assumptions, but we should confirm whether this was a deliberate narrowing from a more general ancestor implementation.

## 5. Is the current code meant to represent `data IDList` and `pseudo-IDList` using the same Java class?

Evidence:

- There is no explicit `DataIDList` or `PseudoIDList` class.
- The same [IDList_ArrayProjSimple](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14) is used for:
  - frequent 1-pattern storage
  - joined projected structures

Why this matters:

- The paper distinguishes the concepts explicitly.
- The code collapses them into one implicit representation.

## 6. Is the DUB implementation in this repository intended to be exactly the paper version?

Evidence:

- Length-2 pruning uses global CID bitsets in [dubCheckFast](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230).
- Deeper levels use local-id bitmaps in [dubCheckLocal](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:248).

Why this matters:

- The paper emphasizes local-id bitmap shrinking.
- The code uses a mixed strategy.
- This may be a pragmatic implementation detail, but should be confirmed if strict reproduction is the goal.

## 7. Is the repository supposed to include more benchmark datasets than `FIFA.txt`?

Evidence:

- The paper reports experiments on `FIFA`, `Kosarak`, `MSNBC`, and `BMS2`.
- In the analyzed snapshot I only observed `dataset/clickstream/FIFA.txt`.

Why this matters:

- Benchmark reproduction is incomplete without the other datasets or documented download instructions.

## 8. Is the comment on default support in `MainCUP` correct?

Evidence:

- [MainCUP.main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:20) sets `double minSupport = 0.00001;`
- The inline comment says `// 60% default`

Why this matters:

- `0.00001` and `60%` are not consistent.
- This is a documentation inconsistency even if runtime behavior is unchanged.

## 9. Is the paper's running example perfectly aligned with this repository's expected `paper_example`?

Evidence:

- The repository test harness expects an exact pattern/support set in [CUPCorrectnessTest.testPaperExample](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:50).
- The actual `paper_example.txt` file was not present in the analyzed snapshot.

Why this matters:

- We cannot verify whether the repository's intended example file matches the exact running example in the paper without the missing file.

## 10. Are `abstractions/`, `datastructures/`, and `patterns/` intentionally retained for future SPADE / CM-SPADE work?

Evidence:

- These packages are lightly connected to the current CUP runtime path.
- They look inherited from a more general SPMF lineage.

Why this matters:

- If they are intentional scaffolding, we should preserve them.
- If they are dead compatibility leftovers, that is still important to document before any future cleanup.
