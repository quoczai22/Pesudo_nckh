# EXECUTION_FLOW

## End-to-end flow

### 1. Program startup

- Entry point: [MainCUP.main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
- Responsibilities:
  - pick a default dataset path via [resolveDefaultDataFile](D:/PESUDO_KQ/P1/src/main/MainCUP.java:57)
  - parse `--data`, `--sup`, `--out`
  - print run header
  - construct [FileLogger](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:15)
  - construct [SequenceDatabase](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:16)
  - call `db.loadFile(...)`
  - construct [AlgoCUP](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:45)
  - call `algo.run(db)`

## 2. Dataset loading

- Loader: [SequenceDatabase.loadFile](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:33)
- Input modes:
  - tuple mode via [parseTupleSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:103)
  - token stream / SPMF-like mode via [parseTokenStreamSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:118)
- Data stored as:
  - `List<List<Integer>> sequences`

### Observed preprocessing behavior

- Empty lines and comment lines starting with `#`, `%`, `@` are skipped.
- In token-stream mode:
  - `-1` is ignored
  - `-2` ends the sequence
  - tokens with `:` are truncated before `:`
- Result:
  - the loader flattens input into a single integer list per sequence
  - it does not preserve multi-item itemsets as nested itemsets

This is consistent with clickstream assumptions, but it is not a general sequential-database loader in the SPADE sense.

## 3. Threshold normalization

- Method: [AlgoCUP.run](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
- Conversion:
  - `absoluteMinSupport = ceil(relativeMinSupport * number_of_sequences)`

## 4. Frequent 1-pattern scan

- Method: [AlgoCUP.step1ScanDB](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:79)
- Behavior:
  - scans every sequence
  - builds `itemCids: Map<Integer, BitSet>`
  - each bitset records which sequence ids contain the item
  - frequent items are stored in `frequentItems`
  - frequent 1-pattern CID bitsets are stored in `onePatternCidBitsets`

This is the first support-counting stage.

## 5. Data IDList / projected structure construction

- Method: [AlgoCUP.step2BuildIDLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
- Builder class: [IDList_ArrayProjSimple](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14)
- Row insertion method: [registerBit](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:51)

### What is built

- One structure per frequent 1-pattern.
- These are stored in `onePatternIdLists`.
- For each sequence position `pos` where item `x` appears, the builder stores:
  - sequence header information in `backboneIDList`
  - projected pointers in `sequence_ItemsetEntries`

### Important interpretation

The paper names the 1-pattern structure a `data IDList`.

The implementation does not expose an explicit Java class named `DataIDList` or `PseudoIDList`.

Instead, the same concrete class `IDList_ArrayProjSimple` is reused to represent:

- frequent 1-pattern vertical data
- projected results after joins

So the paper-level distinction is conceptual, not class-level, in this repository.

## 6. Candidate generation for length 2

- Method: [AlgoCUP.step3DFSMining](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)

### Flow

1. Store all frequent 1-patterns into `frequentPatterns`.
2. For each `itemA`:
   - pair it with every `itemB`
   - build candidate `cand = [itemA, itemB]`
3. Apply first-stage DUB by intersecting global CID bitsets via [dubCheckFast](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230)
4. If not pruned, join the two IDLists via [joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
5. If support is frequent:
   - store candidate support
   - create a local-id DUB bitmap via [createLocalBitmap](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:281)
   - push the candidate into the current equivalence-class-like list `classMembers`

## 7. Support counting

Support is counted in two places:

### Upper-bound support for pruning

- Global sequence intersection for 2-candidates:
  - [dubCheckFast](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230)
- Local-id bitmap intersection for deeper DFS:
  - [dubCheckLocal](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:248)

### True pattern support after temporal join

- Join wrapper:
  - [joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
- Actual join implementation:
  - [IDList_ArrayProjSimple.join](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:128)
- Support accessor:
  - [IDList_ArrayProjSimple.getSupport](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:97)

## 8. Temporal join

- Main join call:
  - `left.join(right, false, absoluteMinSupport)`
  - from [AlgoCUP.joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:269)

### Important consequence

The CUP mining path currently always calls join with `equals = false`.

That means the active path uses only the `after` relation implemented by [laterLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:264).

The equal-relation branch exists in code via [equalLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:195), but it is not used by `AlgoCUP`.

This matches clickstream sequences with one item per event, but it is narrower than general SPADE.

## 9. DFS traversal

- Entry: [AlgoCUP.step3DFSMining](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)
- Recursive expansion: [dfsExpand](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:179)

### Observed traversal pattern

For each class member `pcA`:

1. combine `pcA` with every sibling `pcB`
2. determine `lastB`
3. append `lastB` to `pcA.pattern`
4. skip if already present in `frequentPatterns`
5. apply local DUB
6. join pseudo/data structure projections
7. if frequent, add to `newClass`
8. recurse on `newClass`

This is the code-level realization of candidate generation plus DFS traversal.

## 10. DUB implementation

### First layer

- Global sequence bitsets for frequent 1-patterns:
  - `onePatternCidBitsets`
  - [AlgoCUP.step1ScanDB](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:79)

### Deeper layers

- Local-id bitmap class:
  - [LocalIdBitmap](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:12)
- Constructed from parent and child projected structures:
  - [fromParentAndChild](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:27)
- Intersected in:
  - [dubCheckLocal](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:248)

## 11. Output generation

All output is written by [FileLogger.log](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:34).

Files generated by `AlgoCUP.run`:

- extracted patterns:
  - [exportExtractedPatterns](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:297)
- pattern-length profile:
  - [exportPatternLengthProfile](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:315)
- run metrics:
  - [exportResearchMetrics](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:339)
- summary text:
  - [logStatisticsSummary](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:356)
- console summary:
  - [printFinalResults](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:375)

## 12. Failure points relevant to reproduction

### Missing default input files

- [resolveDefaultDataFile](D:/PESUDO_KQ/P1/src/main/MainCUP.java:57) points to files not observed in the repository snapshot.

### Output path mismatch in console text

- Console summary prints `Output/Extracted_Patterns.txt` literally in [printFinalResults](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:386), even if a different `--out` directory was supplied.

### Trace artifact inside `src`

- `src/Output/Detailed_Execution_Trace.txt` is a generated file placed inside the source tree.
