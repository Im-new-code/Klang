# MilkDrop preset import staging area

The initial build does not bundle .milk presets because none have been curated for redistribution and the projectM engine is not integrated. Do not populate this folder with arbitrary third-party preset packs without checking each pack's copyright/license and attribution.

The planned importer should copy chosen text presets into app-private storage, impose a conservative file-size limit, reject invalid encodings/control sequences, avoid executing external code outside the visualizer parser, and expose reset/delete controls. The eventual twenty included presets must be original or explicitly redistributable and tested on GLES 2 / older Adreno hardware.
