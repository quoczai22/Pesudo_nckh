# PROJECT_STRUCTURE

## Scope analyzed

Repository analyzed from:

- `D:\PESUDO_KQ\P1`

No source files were modified during this analysis. This document only describes the current state of the repository.

## Top-level structure

```text
P1/
|- .vscode/
|- bin/
|- dataset/
|  |- clickstream/
|  |  `- FIFA.txt
|  `- sequence/
|- Output/
|- src/
|  |- abstractions/
|  |- algorithms/
|  |- database/
|  |- datastructures/
|  |- idLists/
|  |- main/
|  |- Output/
|  |- patterns/
|  |- tests/
|  `- utils/
|- 1. Efficient algorithms for mining clickstream patterns using 2020.pdf
|- P1.rar
`- Paper1_PseudoIDList_CUP.pptx
```

## Runtime-critical path

The executable path for CUP is:

1. Entry point: [MainCUP.java](D:/PESUDO_KQ/P1/src/main/MainCUP.java:17)
2. Dataset loading: [SequenceDatabase.loadFile](D:/PESUDO_KQ/P1/src/database/SequenceDatabase.java:33)
3. Mining pipeline: [AlgoCUP.run](D:/PESUDO_KQ/P1/src/algorithms/AlgoCUP.java:57)
4. Output writing: [FileLogger](D:/PESUDO_KQ/P1/src/utils/FileLogger.java:15)

## Package overview

### `src/main`

- Contains the CLI entry point.
- `MainCUP` parses `--data`, `--sup`, `--out`, creates the logger and launches the pipeline.

### `src/algorithms`

- Contains the core CUP implementation.
- `AlgoCUP` is the main orchestration class for threshold conversion, frequent 1-pattern discovery, IDList construction, candidate generation, DUB pruning, DFS traversal, and result export.

### `src/database`

- Contains input parsing and in-memory sequence storage.
- `SequenceDatabase` loads either tuple-like lines or token-stream / SPMF-like lines.

### `src/idLists`

- Contains the key vertical data structures used by the implementation.
- `IDList_ArrayProjSimple` is the actual core representation used during joins.
- `LocalIdBitmap` stores local-id-space DUB bitmaps.
- `IntArrayList` is a low-level integer container used by `IDList_ArrayProjSimple`.
- `IDListProjection` and `IDListReduction` are interfaces.

### `src/utils`

- Contains output/logging support.
- `FileLogger` creates the output directory and writes all text artifacts.

### `src/tests`

- Contains a plain-Java correctness harness.
- `CUPCorrectnessTest` is not part of the main pipeline.

### `src/abstractions`, `src/datastructures`, `src/patterns`

- These are legacy SPMF-style support abstractions.
- In the current CUP runtime path, they are mostly compatibility scaffolding rather than active mining logic.

### `src/Output`

- Contains `Detailed_Execution_Trace.txt`.
- This is not source code. It is a generated trace file currently stored inside `src`, which is structurally inconsistent with the rest of the repository.

## Important non-source artifacts

### `dataset/`

- Input data directory.
- Currently observed:
  - `dataset/clickstream/FIFA.txt`
  - empty `dataset/sequence/`

### `Output/`

- Generated run artifacts:
  - `Detailed_Execution_Trace.txt`
  - `Extracted_Patterns.txt`
  - `Metrics_Research.tsv`
  - `Pattern_Length_Profile.tsv`
  - `Stats_Summary.txt`

### `bin/`

- Compiled `.class` artifacts mirroring `src`.

### Top-level PDF / PPTX / RAR

- PDF: paper source used for code-to-paper mapping.
- PPTX: presentation artifact.
- RAR: archive artifact, not part of the Java runtime.

## Current reproduction risk spotted immediately

The default dataset resolution in [MainCUP.java](D:/PESUDO_KQ/P1/src/main/MainCUP.java:57) checks:

1. `dataset/SIGN.txt`
2. fallback `dataset/paper_example.txt`

But neither of these files was present in the analyzed repository snapshot. That means the default execution path is not reproducible from the current repository state without passing `--data` explicitly or restoring the missing files.
