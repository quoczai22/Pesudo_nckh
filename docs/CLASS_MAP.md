# CLASS_MAP

This map covers every Java file currently present under `src/`.

## `src/main/MainCUP.java`

- Path: [MainCUP.java](D:/PESUDO_KQ/P1/src/main/MainCUP.java:1)
- Responsibility:
  - CLI entry point and top-level pipeline bootstrap.
- Important classes:
  - `MainCUP`
- Important methods:
  - [main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [resolveDefaultDataFile](D:/PESUDO_KQ/P1/src/main/MainCUP.java:57)
- Called by:
  - JVM entry point
- Calls to:
  - `new FileLogger(...)`
  - `new SequenceDatabase()`
  - `db.loadFile(...)`
  - `new AlgoCUP(...)`
  - `algo.run(...)`
- Related paper section:
  - operational wrapper, not explicitly described in the paper

## `src/algorithms/AlgoCUP.java`

- Path: [AlgoCUP.java](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:1)
- Responsibility:
  - core CUP mining pipeline
  - threshold normalization
  - frequent 1-pattern scan
  - IDList construction
  - candidate generation
  - DUB pruning
  - DFS traversal
  - output export
- Important classes:
  - `AlgoCUP`
  - internal `PatternClass`
  - internal `IntSupportAccumulator`
- Important methods:
  - [run](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
  - [step1ScanDB](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:79)
  - [step2BuildIDLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
  - [step3DFSMining](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:135)
  - [dfsExpand](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:179)
  - [dubCheckFast](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:230)
  - [dubCheckLocal](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:248)
  - [joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
  - [createLocalBitmap](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:281)
  - [exportExtractedPatterns](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:297)
  - [exportPatternLengthProfile](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:315)
  - [exportResearchMetrics](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:339)
  - [logStatisticsSummary](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:356)
  - [printFinalResults](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:375)
- Called by:
  - [MainCUP.main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [CUPCorrectnessTest.testPaperExample](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:50)
- Calls to:
  - `SequenceDatabase.getSequences()`
  - `IDList_ArrayProjSimple.registerBit(...)`
  - `IDListProjection.join(...)`
  - `LocalIdBitmap.fromParentAndChild(...)`
  - `FileLogger.trace(...)`
  - `FileLogger.log(...)`
- Related paper section:
  - Section 4.3, 4.4, 4.5

## `src/database/SequenceDatabase.java`

- Path: [SequenceDatabase.java](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:1)
- Responsibility:
  - input loading
  - parsing
  - in-memory storage of sequences
- Important classes:
  - `SequenceDatabase`
  - internal `ParsedSequence`
  - internal `ParseSource`
- Important methods:
  - [loadFile](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:33)
  - [parseSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:92)
  - [parseTupleSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:103)
  - [parseTokenStreamSequence](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:118)
  - [containsDigit](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:155)
  - [getSequences](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:192)
- Called by:
  - [MainCUP.main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [CUPCorrectnessTest.testPaperExample](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:50)
- Calls to:
  - `FileLogger.traceHeader(...)`
  - `FileLogger.trace(...)`
- Related paper section:
  - precondition / input preparation, not a direct paper section

## `src/idLists/IDList_ArrayProjSimple.java`

- Path: [IDList_ArrayProjSimple.java](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:1)
- Responsibility:
  - concrete vertical structure used for 1-patterns and joined projections
  - support access
  - temporal join implementation
  - row-level access used by local DUB
- Important classes:
  - `IDList_ArrayProjSimple`
- Important methods:
  - [registerBit](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:51)
  - [getSupport](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:97)
  - [join](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:128)
  - [equalLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:195)
  - [laterLoop](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:264)
  - [getSequenceItemsetEntries](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:333)
  - [getBackbone_idlist](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:346)
  - [getRowCount](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:355)
  - [getSequenceIdAtRow](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:365)
- Called by:
  - [AlgoCUP.step2BuildIDLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:108)
  - [AlgoCUP.joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
  - [LocalIdBitmap.fromParentAndChild](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:27)
  - [CUPCorrectnessTest](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:26)
- Calls to:
  - `IntArrayList` operations
  - `IPattern` in compatibility method `setAppearingSequences`
- Related paper section:
  - Section 4.1 and 4.2 at the representation level
  - Section 4.4 and 4.5 at the join/exploration level

## `src/idLists/LocalIdBitmap.java`

- Path: [LocalIdBitmap.java](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:1)
- Responsibility:
  - local-id-space DUB bitmap construction and sibling intersection
- Important classes:
  - `LocalIdBitmap`
- Important methods:
  - [fromParentAndChild](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:27)
  - [intersectionSupport](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:53)
  - [cardinality](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:60)
  - [getUniverseSize](D:/PESUDO_KQ/P1/src/idLists/LocalIdBitmap.java:64)
- Called by:
  - [AlgoCUP.createLocalBitmap](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:281)
  - [CUPCorrectnessTest.testLocalBitmapShrinks](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:26)
- Calls to:
  - `IDList_ArrayProjSimple.getRowCount()`
  - `IDList_ArrayProjSimple.getSequenceIdAtRow(...)`
- Related paper section:
  - Section 4.3 DUB implementation technique

## `src/idLists/IntArrayList.java`

- Path: [IntArrayList.java](D:/PESUDO_KQ/P1/src/idLists/IntArrayList.java:1)
- Responsibility:
  - primitive integer dynamic array used by `IDList_ArrayProjSimple`
- Important classes:
  - `IntArrayList`
- Important methods:
  - constructor overloads
  - `trim`
  - `ensureCapacity`
  - `size`
  - `get`
  - `add`
  - `set`
  - `clear`
- Called by:
  - [IDList_ArrayProjSimple](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14)
- Calls to:
  - `java.util.Arrays.copyOf`
- Related paper section:
  - no direct paper section; implementation utility only

## `src/idLists/IDListProjection.java`

- Path: [IDListProjection.java](D:/PESUDO_KQ/P1/src/idLists/IDListProjection.java:1)
- Responsibility:
  - marker interface extending `IDListReduction`
- Important classes:
  - `IDListProjection`
- Important methods:
  - none beyond inherited contract
- Called by:
  - [AlgoCUP](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:29)
  - [IDList_ArrayProjSimple](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:14)
- Calls to:
  - none
- Related paper section:
  - none directly

## `src/idLists/IDListReduction.java`

- Path: [IDListReduction.java](D:/PESUDO_KQ/P1/src/idLists/IDListReduction.java:1)
- Responsibility:
  - interface contract for joinable vertical structures
- Important classes:
  - `IDListReduction`
- Important methods:
  - `join`
  - `getSupport`
  - `setAppearingSequences`
  - `clear`
- Called by:
  - [IDListProjection](D:/PESUDO_KQ/P1/src/idLists/IDListProjection.java:5)
  - [AlgoCUP.joinIdLists](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:267)
- Calls to:
  - refers to `IPattern`
- Related paper section:
  - none directly

## `src/utils/FileLogger.java`

- Path: [FileLogger.java](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:1)
- Responsibility:
  - create output directory
  - manage lazy writers
  - write traces and result artifacts
- Important classes:
  - `FileLogger`
- Important methods:
  - [log](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:34)
  - [trace](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:57)
  - [traceHeader](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:64)
  - [close](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:74)
- Called by:
  - [MainCUP.main](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
  - [SequenceDatabase.loadFile](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:33)
  - [AlgoCUP.run](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
  - [CUPCorrectnessTest.testPaperExample](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:50)
- Calls to:
  - `java.io.BufferedWriter`
  - `java.io.FileWriter`
- Related paper section:
  - output infrastructure, not a paper algorithm section

## `src/tests/CUPCorrectnessTest.java`

- Path: [CUPCorrectnessTest.java](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:1)
- Responsibility:
  - local correctness checks for:
    - local-id bitmap shrink behavior
    - exact pattern/support results on `paper_example`
- Important classes:
  - `CUPCorrectnessTest`
- Important methods:
  - [main](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:17)
  - [testLocalBitmapShrinks](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:26)
  - [testPaperExample](D:/PESUDO_KQ/P1/src/tests/CUPCorrectnessTest.java:50)
- Called by:
  - manual execution only
- Calls to:
  - `SequenceDatabase.loadFile(...)`
  - `AlgoCUP.run(...)`
  - `LocalIdBitmap.fromParentAndChild(...)`
- Related paper section:
  - not part of the paper; reproduction support only

## `src/patterns/IPattern.java`

- Path: [IPattern.java](D:/PESUDO_KQ/P1/src/patterns/IPattern.java:1)
- Responsibility:
  - generic pattern interface from SPMF-style lineage
- Important classes:
  - `IPattern`
- Important methods:
  - pattern element accessors
  - `compareTo`
  - `isPrefix`
  - `getAppearingIn`
  - `setAppearingIn`
- Called by:
  - [IDListReduction](D:/PESUDO_KQ/P1/src/idLists/IDListReduction.java:5)
  - [IDList_ArrayProjSimple.setAppearingSequences](D:/PESUDO_KQ/P1/src/idLists/IDList_ArrayProjSimple.java:312)
- Calls to:
  - `ItemAbstractionPair`
- Related paper section:
  - none directly in the current CUP path

## `src/datastructures/Item.java`

- Path: [Item.java](D:/PESUDO_KQ/P1/src/datastructures/Item.java:1)
- Responsibility:
  - generic comparable item wrapper
- Important classes:
  - `Item`
  - package-private `itemComparator`
- Important methods:
  - constructor
  - `getId`
  - `equals`
  - `hashCode`
  - `compareTo`
- Called by:
  - [ItemAbstractionPair](D:/PESUDO_KQ/P1/src/abstractions/ItemAbstractionPair.java:28)
- Calls to:
  - `java.util.Comparator`
- Related paper section:
  - none directly in the current CUP path

## `src/abstractions/Abstraction_Generic.java`

- Path: [Abstraction_Generic.java](D:/PESUDO_KQ/P1/src/abstractions/Abstraction_Generic.java:1)
- Responsibility:
  - base abstraction contract
- Important classes:
  - `Abstraction_Generic`
- Important methods:
  - abstract `equals`, `hashCode`, `toString`, `toStringToFile`
- Called by:
  - [Abstraction_Qualitative](D:/PESUDO_KQ/P1/src/abstractions/Abstraction_Qualitative.java:32)
  - [ItemAbstractionPair](D:/PESUDO_KQ/P1/src/abstractions/ItemAbstractionPair.java:28)
- Calls to:
  - none
- Related paper section:
  - none directly in the current CUP path

## `src/abstractions/Abstraction_Qualitative.java`

- Path: [Abstraction_Qualitative.java](D:/PESUDO_KQ/P1/src/abstractions/Abstraction_Qualitative.java:1)
- Responsibility:
  - represent equal vs after qualitative relation
- Important classes:
  - `Abstraction_Qualitative`
- Important methods:
  - `create`
  - `hasEqualRelation`
  - `compareTo`
  - `toStringToFile`
- Called by:
  - [ItemAbstractionPair](D:/PESUDO_KQ/P1/src/abstractions/ItemAbstractionPair.java:28)
- Calls to:
  - `Abstraction_Generic`
- Related paper section:
  - conceptually related to vertical sequential relations, but not active in current `AlgoCUP` path

## `src/abstractions/ItemAbstractionPair.java`

- Path: [ItemAbstractionPair.java](D:/PESUDO_KQ/P1/src/abstractions/ItemAbstractionPair.java:1)
- Responsibility:
  - represent `<item, abstraction>` pairs
- Important classes:
  - `ItemAbstractionPair`
- Important methods:
  - constructor
  - `getItem`
  - `getAbstraction`
  - `toStringToFile`
  - `compareTo`
- Called by:
  - [IPattern](D:/PESUDO_KQ/P1/src/patterns/IPattern.java:6)
- Calls to:
  - `Item`
  - `Abstraction_Generic`
  - `Abstraction_Qualitative`
- Related paper section:
  - no direct role in the current clickstream-only CUP pipeline
