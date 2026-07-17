# Recipe Editor Design

## Scope

The first feature adds a `recipe_editor` block and a dedicated creative tab. Right-clicking the block opens an LDLib2 UI styled after the vanilla crafting table. Vanilla crafting tables remain unchanged.

The editor uses nine phantom input slots and one phantom output slot. Phantom slots copy the carried stack or accept JEI ghost ingredients without consuming inventory. The player inventory remains visible. The recipe book is omitted.

## Recipe Model

Each input slot supports three representations:

- Exact item, optionally preserving its data component patch.
- Item tag such as `#minecraft:planks`.
- Alternative ingredients represented as a KubeJS array.

Left-clicking a slot while carrying an inventory item copies that item into the phantom slot without consuming it. Left-clicking with an empty cursor clears an input or output slot. Right-clicking a non-empty input slot with an empty cursor opens its ingredient settings dialog; right-clicking a non-empty output opens its count editor. Empty slots do not open dialogs and remain available for carried stacks or JEI ghost ingredients. In item-tag mode, the editor reads Item Tags through the same `ItemStack.typeHolder().tags()` source used by `/kubejs hand` and presents them in a sorted dropdown instead of accepting a manually typed tag. Block Tags shown separately by that command are excluded because they are not valid item recipe ingredients. Saving is rejected when that item has no tags. Component data is captured from the slot stack by default and can be overridden with a manually edited KubeJS component patch. The output also preserves component data.

## Actions

The main UI provides three actions:

- Toggle shaped/shapeless.
- Preview and copy generated KubeJS code.
- Write generated code to `kubejs/server_scripts/generated_recipes.js`. The first recipe creates the complete `ServerEvents.recipes` wrapper; later recipes insert only their `event.shaped` or `event.shapeless` call before the existing wrapper closes.

Copy is client-side. File writes execute on the server through LDLib2 UI RPC. Integrated single-player servers allow writes; multiplayer servers require an operator. The server validates the text size and wrapper format and creates the script directory. The first write creates one complete `ServerEvents.recipes` block; later writes insert only their event call before that block closes.

## Validation

Generation requires at least one input and a non-empty output. Invalid tag identifiers, empty alternative lists, invalid component patch delimiters, oversized payloads, missing operator permission, and file IO failures return a visible error without closing the editor.

Verification is performed through compilation, a full build, and direct client runtime checks covering registration, creative-tab visibility, block interaction, LDLib2 rendering, JEI ghost input, code preview, and server write feedback.
