#!/usr/bin/env python3
"""Create missing test skeletons for all Java classes and methods.

The script scans `src/main/java` recursively, mirrors the package
structure inside `src/test/java` and for every public class and method
ensures a corresponding test file exists. Missing test methods are
populated with a failing JUnit stub so developers can fill them in
later.

Usage: `python scripts/ensure_all_tests.py`
Requires: `pip install javalang`
"""
from __future__ import annotations

from pathlib import Path
import javalang

SRC_MAIN = Path('src/main/java')
SRC_TEST = Path('src/test/java')

def parse_source(java_file: Path):
    """Yield (package, package_path, class_decl) for each class in file."""
    tree = javalang.parse.parse(java_file.read_text(encoding='utf-8'))
    package = tree.package.name if tree.package else ''
    package_path = Path(package.replace('.', '/')) if package else Path()
    for type_decl in tree.types:
        if isinstance(type_decl, javalang.tree.ClassDeclaration):
            yield package, package_path, type_decl

def existing_test_methods(test_file: Path) -> set[str]:
    if not test_file.exists():
        return set()
    try:
        tree = javalang.parse.parse(test_file.read_text(encoding='utf-8'))
        return {m.name for m in tree.types[0].methods}
    except Exception:
        return set()

def create_or_update_test(package: str, package_path: Path, class_decl):
    test_dir = SRC_TEST / package_path
    test_dir.mkdir(parents=True, exist_ok=True)
    test_file = test_dir / f'{class_decl.name}Test.java'

    lines = []
    if test_file.exists():
        lines = test_file.read_text(encoding='utf-8').splitlines()
    else:
        if package:
            lines.append(f'package {package};')
            lines.append('')
        lines.extend([
            'import org.junit.jupiter.api.Test;',
            'import static org.junit.jupiter.api.Assertions.fail;',
            '',
            f'class {class_decl.name}Test {{',
            '}'
        ])

    methods_in_test = existing_test_methods(test_file)

    for method in class_decl.methods:
        if 'public' not in method.modifiers or method.name in methods_in_test:
            continue
        insert_at = len(lines) - 1
        snippet = [
            '    @Test',
            f'    void {method.name}() {',
            '        fail("Not yet implemented");',
            '    }',
            ''
        ]
        lines[insert_at:insert_at] = snippet
        methods_in_test.add(method.name)

    test_file.write_text('\n'.join(lines) + '\n', encoding='utf-8')

def ensure_tests(java_file: Path) -> None:
    for package, package_path, class_decl in parse_source(java_file):
        create_or_update_test(package, package_path, class_decl)

def main() -> None:
    for java_file in SRC_MAIN.rglob('*.java'):
        if java_file.name == 'package-info.java':
            continue
        ensure_tests(java_file)

if __name__ == '__main__':
    main()
