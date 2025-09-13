#!/usr/bin/env python3
"""Utility to ensure each Java class has a corresponding test skeleton.

The script walks through all Java files in ``src/main/java`` and checks whether
an appropriate test exists under ``src/test/java``. For missing tests it
creates a JUnit 5 skeleton with one failing test method per public method. If
an implementation looks like a stub, only plain unit tests are generated;
otherwise a Micronaut integration test is created.

Requirements: ``pip install javalang``.
"""
from __future__ import annotations

import argparse
from pathlib import Path
from typing import Iterable

import javalang

SRC_MAIN = Path("src/main/java")
SRC_TEST = Path("src/test/java")


def is_stub(content: str, class_name: str) -> bool:
    """Heuristically detect whether a class is a stub implementation."""
    markers = [
        "TODO", "FIXME", "throw new UnsupportedOperationException", "return null",
    ]
    if any(m in content for m in markers):
        return True
    if class_name.endswith("Stub"):
        return True
    return False


def ensure_test(java_file: Path) -> None:
    """Create or update tests for the given Java source file."""
    source = java_file.read_text(encoding="utf-8")
    tree = javalang.parse.parse(source)
    package = tree.package.name if tree.package else ""
    package_path = Path(package.replace('.', '/'))

    for type_decl in tree.types:
        if not isinstance(type_decl, javalang.tree.ClassDeclaration):
            continue
        class_name = type_decl.name
        test_file = SRC_TEST / package_path / f"{class_name}Test.java"
        stub = is_stub(source, class_name)
        if test_file.exists():
            update_existing_test(test_file, type_decl)
        else:
            create_new_test(test_file, package, type_decl, stub)


def create_new_test(test_file: Path, package: str, class_decl, stub: bool) -> None:
    test_file.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        f"package {package};",
        "",
        "import org.junit.jupiter.api.Test;",
        "import static org.junit.jupiter.api.Assertions.*;",
    ]
    if not stub:
        lines.insert(2, "import io.micronaut.test.extensions.junit5.annotation.MicronautTest;")
        lines.append("")
        lines.append("@MicronautTest")
    lines.append(f"class {class_decl.name}Test {{")
    for method in class_decl.methods:
        if 'public' not in method.modifiers:
            continue
        lines.append("    @Test")
        lines.append(f"    void {method.name}() {{")
        lines.append("        fail(\"Not yet implemented\");")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    test_file.write_text("\n".join(lines) + "\n", encoding="utf-8")


def update_existing_test(test_file: Path, class_decl) -> None:
    content = test_file.read_text(encoding="utf-8")
    try:
        tree = javalang.parse.parse(content)
        existing_methods = {m.name for m in tree.types[0].methods}
    except Exception:
        existing_methods = set()

    lines = content.rstrip().splitlines()
    for method in class_decl.methods:
        if 'public' not in method.modifiers or method.name in existing_methods:
            continue
        insert_point = len(lines) - 1
        lines.insert(insert_point, "    @Test")
        lines.insert(insert_point + 1, f"    void {method.name}() {{")
        lines.insert(insert_point + 2, "        fail(\"Not yet implemented\");")
        lines.insert(insert_point + 3, "    }")
        lines.insert(insert_point + 4, "")
    test_file.write_text("\n".join(lines) + "\n", encoding="utf-8")


def iter_java_files(base: Path) -> Iterable[Path]:
    for path in base.rglob("*.java"):
        if path.name.endswith("package-info.java"):
            continue
        yield path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default=str(SRC_MAIN), help="Base directory for source files")
    args = parser.parse_args()
    for java_file in iter_java_files(Path(args.base)):
        ensure_test(java_file)


if __name__ == "__main__":
    main()
