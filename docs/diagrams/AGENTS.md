# Diagrams Guidelines

- Each PlantUML file in this directory **must** include `!include style.puml` right after `@startuml` to enforce a non-transparent white background.
- Regenerate SVG diagrams with `plantuml -tsvg *.puml` after changes.
 - After generation run `../../scripts/add-white-background.sh` to ensure exported SVGs have an opaque white background.
