# ALGORITHM_MAPPING

This mapping uses the section headings extracted from the paper:

- Section 4.1. `(Semi-vertical) data IDList`
- Section 4.2. `Pseudo-IDList`
- Section 4.3. `Dynamic Intersection Upper Bound Constraint Pruning Heuristic (DUB)`
- Section 4.4. `Candidate generation`
- Section 4.5. `The CUP algorithm`

## Section 4.1 `(Semi-vertical) data IDList`

Paper concept:

- 1-pattern vertical data structure
- rows conceptually contain `{Data id, UCID, Position list}`
- support is row count

Implementation:

- File:
  - [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
  - [IDList_ArrayProjSimple.java](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14)
- Class:
  - `AlgoCUP`
  - `IDList_ArrayProjSimple`
- Methods:
  - [step2BuildIDLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
  - [registerBit](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:51)
  - [getSupport](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:97)

Evidence-based interpretation:

- Frequent 1-pattern vertical structures are built only once in `step2BuildIDLists`.
- `registerBit` stores sequence header and position information into two primitive arrays:
  - `backboneIDList`
  - `sequence_ItemsetEntries`

Difference from paper wording:

- The code does not expose a named `Data IDList` class.
- The paper's `Data IDList` is represented implicitly by `IDList_ArrayProjSimple` when used for frequent 1-patterns.

## Section 4.2 `Pseudo-IDList`

Paper concept:

- pseudo-IDList stores:
  - `P`
  - `DIP`
  - `M = {Local id, Data id, Start index}`
- support is number of rows in `M`

Implementation:

- File:
  - [IDList_ArrayProjSimple.java](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14)
  - [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
- Class:
  - `IDList_ArrayProjSimple`
- Methods:
  - [join](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:128)
  - [laterLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:264)
  - [getRowCount](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:355)
  - [getSequenceIdAtRow](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:365)

Evidence-based interpretation:

- Joined results do not materialize fresh copied position lists.
- The returned structure reuses another structure's `backboneIDList` as its data source and stores row pointers in `sequence_ItemsetEntries`.
- In practice this behaves like a pointer-based pseudo representation.

Difference from paper wording:

- The code does not explicitly store a Java object with fields named `P`, `DIP`, and `M`.
- Instead:
  - `P` is stored outside the structure as `List<Integer>` in `AlgoCUP.PatternClass`
  - `DIP` is approximated by reuse of `backboneIDList`
  - `M` is encoded as alternating integers in `sequence_ItemsetEntries`
- This is semantically close, but structurally more implicit than the paper description.

## Section 4.3 `Dynamic Intersection Upper Bound Constraint Pruning Heuristic (DUB)`

Paper concept:

- use set intersection of sequences containing sibling patterns as a tighter upper bound
- implement efficiently with bitmaps
- paper emphasizes dynamic shrinking by local ids

Implementation:

- File:
  - [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230)
  - [LocalIdBitmap.java](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:12)
- Class:
  - `AlgoCUP`
  - `LocalIdBitmap`
- Methods:
  - [dubCheckFast](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230)
  - [dubCheckLocal](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:248)
  - [createLocalBitmap](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:281)
  - [fromParentAndChild](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:27)
  - [intersectionSupport](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:53)

Evidence-based interpretation:

- Length-2 generation uses global sequence bitsets `onePatternCidBitsets`.
- Deeper DFS uses local-id bitmaps attached to `PatternClass`.
- `LocalIdBitmap` enforces that sibling bitmaps must share the same parent local-id universe.

Difference from paper wording:

- The paper presents local-id shrinking as the main bitmap implementation technique.
- The code uses a mixed strategy:
  - global CID bitsets for the first expansion
  - local-id bitmaps for deeper levels
- This is not necessarily wrong, but it should be documented as an implementation choice.

## Section 4.4 `Candidate generation`

Paper concept:

- two frequent `k`-patterns with shared `(k-1)`-prefix generate:
  - `{(X, lastY), (Y, lastX)}` if last items differ
  - one candidate if they are equal
- DUB is applied before expensive construction

Implementation:

- File:
  - [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)
- Class:
  - `AlgoCUP`
- Methods:
  - [step3DFSMining](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)
  - [dfsExpand](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:179)

Evidence-based interpretation:

- Level 2:
  - `itemA` is paired with every `itemB`
  - DUB is checked before join
- Deeper levels:
  - `pcA` is combined with each sibling `pcB`
  - `lastB` is appended to `pcA.pattern`
  - DUB is checked before join

Difference from paper wording:

- The code avoids duplicates by checking `frequentPatterns.containsKey(cand)`.
- The candidate-generation logic is expressed directly in loops rather than as a separate explicit candidate-generation module.

## Section 4.5 `The CUP algorithm`

Paper concept:

- scan database
- build data IDLists and pseudo-IDLists for frequent 1-patterns
- generate candidates
- apply DUB
- create pseudo-IDLists
- DFS traverse deeper branches

Implementation:

- File:
  - [MainCUP.java](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
- Class:
  - `MainCUP`
  - `AlgoCUP`
- Methods:
  - [main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [run](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
  - [step1ScanDB](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:79)
  - [step2BuildIDLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
  - [step3DFSMining](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)
  - [dfsExpand](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:179)

Observed mapping:

1. Step 0 in code:
   - dataset loading in `SequenceDatabase.loadFile`
2. Step 1 in code:
   - frequent 1-pattern scan in `step1ScanDB`
3. Step 2 in code:
   - 1-pattern IDList construction in `step2BuildIDLists`
4. Step 3 in code:
   - DFS, candidate generation, DUB, join, support test, recursion

Difference from paper wording:

- The code splits CUP into three explicit steps after loading, not exactly the same textual step numbering as the paper.
- The code does not create an explicit named pseudo-IDList object for each pattern.

## Related paper concepts not cleanly mirrored as explicit classes

### `P`

- Stored as `List<Integer>` in `AlgoCUP.PatternClass`

### `DIP`

- Not named as such in code
- represented indirectly by which `backboneIDList` the joined projection points to

### `M`

- Not stored as a visible 3-column matrix object
- encoded in `sequence_ItemsetEntries`

## Code-paper mismatches to keep visible

### Loader generality vs paper domain

- The paper is about clickstream sequences with one event per timestamp.
- The loader supports tuple and SPMF-like token input, then flattens itemsets into a plain item sequence.
- This is broader in accepted input format, but narrower in preserved structure.

### Equal relation implementation exists but is unused in CUP path

- `IDList_ArrayProjSimple.equalLoop(...)` exists.
- `AlgoCUP.joinIdLists(...)` always calls `join(..., false, ...)`.
- So runtime behavior is strictly the after-relation branch.

### Repository snapshot and reproducibility

- The default input paths in `MainCUP` do not match the observed current dataset files.
- That is a repository-state issue, not necessarily a paper mismatch, but it affects faithful reproduction.
