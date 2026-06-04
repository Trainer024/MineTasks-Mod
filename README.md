# NoteTask Mod
**Minecraft 1.21.11 · Fabric · Java 21**

In-game notes and task tracker with HUD progress, storage scanning, and optional [Reliable Recipe Viewer (RRV)](https://modrinth.com/mod/rrv) integration.

## Features

### Tasks
- **Item tasks** — auto-track inventory (+ optional nearby chests / ender chest)
- **Manual tasks** — simple checkboxes
- **HUD overlay** — pinned tasks first, progress bars, optional item icons
- **Search, sort** — filter by name/ID; sort by order, name, or progress
- **Keyboard shortcuts** in the task menu: Delete, Enter (edit), Ctrl+D (duplicate)
- **Quick-add** keybind — create a task from the item in your main hand
- **Progress milestones** — chat hints at 25%, 50%, 75%
- **Presets** in the task editor for common materials

### Notes
- Title + multiline body
- **Pin** important notes to the top
- **Search** in the shared search bar on the Notes tab

### Reliable Recipe Viewer (optional)
Install [RRV](https://modrinth.com/mod/rrv) alongside NoteTask to:
- **Craft** — open recipes that produce the selected item’s stack
- **Uses** — open recipes that consume the item as an ingredient
- Same actions in the task editor when RRV is present

### Settings (⚙ on main screen or Mod Menu)
- HUD position, size, collapse keybind
- Storage scan radius, completion sound, milestones
- **Export backup** to `config/notetask/backups/`

## Controls
| Action | Default |
|---|---|
| Open NoteTask | **N** |
| Quick-add held item task | *(unbound — set in Controls)* |
| Toggle HUD | *(unbound — set in Controls)* |

## Building
Requires JDK 21. On first build, Gradle downloads the RRV API jar into `libs/` for compile-time integration.

```bash
gradlew.bat build   # Windows
./gradlew build     # Linux / macOS
```

Output: `build/libs/notetask-1.0.0.jar`

## Installing
1. [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11
2. [Fabric API](https://modrinth.com/mod/fabric-api)
3. Optional: [Mod Menu](https://modrinth.com/mod/modmenu), [RRV](https://modrinth.com/mod/rrv)
4. Place `notetask-1.0.0.jar` in `.minecraft/mods/`

Data: `config/notetask/data.json` · Client options: `config/notetask/client.json`
