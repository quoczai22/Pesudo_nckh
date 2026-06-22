# FILE_DEPENDENCY

## High-level dependency graph

```text
MainCUP
  -> SequenceDatabase
  -> FileLogger
  -> AlgoCUP

AlgoCUP
  -> SequenceDatabase
  -> FileLogger
  -> IDListProjection
  -> IDListReduction
  -> IDList_ArrayProjSimple
  -> LocalIdBitmap

IDList_ArrayProjSimple
  -> IntArrayList
  -> IPattern

LocalIdBitmap
  -> IDList_ArrayProjSimple

SequenceDatabase
  -> FileLogger

IDListReduction
  -> IPattern

IPattern
  -> ItemAbstractionPair

ItemAbstractionPair
  -> Item
  -> Abstraction_Generic
  -> Abstraction_Qualitative

Abstraction_Qualitative
  -> Abstraction_Generic

CUPCorrectnessTest
  -> AlgoCUP
  -> SequenceDatabase
  -> IDList_ArrayProjSimple
  -> LocalIdBitmap
  -> FileLogger
```

## Runtime dependency chain

For the actual CUP execution path, the minimum dependency chain is:

```text
MainCUP
  -> SequenceDatabase
  -> AlgoCUP
     -> IDList_ArrayProjSimple
     -> LocalIdBitmap
     -> FileLogger
```

The following packages are present but not central in the current CUP runtime path:

- `abstractions/`
- `datastructures/`
- `patterns/`

They appear to be retained from SPMF-style abstractions and interfaces.

## Per-file dependencies

## `main/MainCUP.java`

- Depends on:
  - `algorithms.AlgoCUP`
  - `database.SequenceDatabase`
  - `utils.FileLogger`
- Used by:
  - JVM launcher only

## `algorithms/AlgoCUP.java`

- Depends on:
  - `database.SequenceDatabase`
  - `idLists.IDListProjection`
  - `idLists.IDListReduction`
  - `idLists.IDList_ArrayProjSimple`
  - `idLists.LocalIdBitmap`
  - `utils.FileLogger`
- Used by:
  - `main.MainCUP`
  - `tests.CUPCorrectnessTest`

## `database/SequenceDatabase.java`

- Depends on:
  - `utils.FileLogger`
- Used by:
  - `main.MainCUP`
  - `algorithms.AlgoCUP`
  - `tests.CUPCorrectnessTest`

## `idLists/IDList_ArrayProjSimple.java`

- Depends on:
  - `idLists.IntArrayList`
  - `idLists.IDListProjection`
  - `patterns.IPattern`
- Used by:
  - `algorithms.AlgoCUP`
  - `idLists.LocalIdBitmap`
  - `tests.CUPCorrectnessTest`

## `idLists/LocalIdBitmap.java`

- Depends on:
  - `idLists.IDList_ArrayProjSimple`
- Used by:
  - `algorithms.AlgoCUP`
  - `tests.CUPCorrectnessTest`

## `idLists/IntArrayList.java`

- Depends on:
  - standard library only
- Used by:
  - `idLists.IDList_ArrayProjSimple`

## `idLists/IDListProjection.java`

- Depends on:
  - `idLists.IDListReduction`
- Used by:
  - `algorithms.AlgoCUP`
  - `idLists.IDList_ArrayProjSimple`

## `idLists/IDListReduction.java`

- Depends on:
  - `patterns.IPattern`
- Used by:
  - `idLists.IDListProjection`
  - `algorithms.AlgoCUP`
  - `idLists.IDList_ArrayProjSimple`

## `utils/FileLogger.java`

- Depends on:
  - standard I/O classes
- Used by:
  - `main.MainCUP`
  - `database.SequenceDatabase`
  - `algorithms.AlgoCUP`
  - `tests.CUPCorrectnessTest`

## `tests/CUPCorrectnessTest.java`

- Depends on:
  - `algorithms.AlgoCUP`
  - `database.SequenceDatabase`
  - `idLists.IDList_ArrayProjSimple`
  - `idLists.LocalIdBitmap`
  - `utils.FileLogger`
- Used by:
  - manual test execution

## `patterns/IPattern.java`

- Depends on:
  - `abstractions.ItemAbstractionPair`
- Used by:
  - `idLists.IDListReduction`
  - `idLists.IDList_ArrayProjSimple`

## `abstractions/ItemAbstractionPair.java`

- Depends on:
  - `datastructures.Item`
  - `abstractions.Abstraction_Generic`
  - `abstractions.Abstraction_Qualitative`
- Used by:
  - `patterns.IPattern`

## `abstractions/Abstraction_Qualitative.java`

- Depends on:
  - `abstractions.Abstraction_Generic`
- Used by:
  - `abstractions.ItemAbstractionPair`

## `abstractions/Abstraction_Generic.java`

- Depends on:
  - standard library only
- Used by:
  - `abstractions.Abstraction_Qualitative`
  - `abstractions.ItemAbstractionPair`

## `datastructures/Item.java`

- Depends on:
  - standard library only
- Used by:
  - `abstractions.ItemAbstractionPair`

## Dependency observations relevant to research

### 1. Algorithm/data structure coupling is tight

`AlgoCUP` is tightly coupled to `IDList_ArrayProjSimple` even though it types fields as `IDListProjection`. Evidence:

- [createLocalBitmap](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:281) explicitly checks and casts to `IDList_ArrayProjSimple`

### 2. pseudo-IDList is not isolated as its own module

The paper-level pseudo-IDList concept is spread across:

- `AlgoCUP.PatternClass.pattern`
- `IDList_ArrayProjSimple.backboneIDList`
- `IDList_ArrayProjSimple.sequence_ItemsetEntries`
- `LocalIdBitmap`

### 3. SPMF legacy interfaces remain but are peripheral

`IPattern`, `Item`, `ItemAbstractionPair`, and the abstraction classes are largely outside the hot path of the current CUP implementation.
