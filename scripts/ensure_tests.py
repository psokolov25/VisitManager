#!/usr/bin/env python3
"""Ensure that every Java class has a corresponding test skeleton.

The script walks through `src/main/java`, mirrors the package
structure inside `src/test/java` and creates missing `*Test` classes.
For each public method a stub test method is added when it is missing.
If a source file looks like a stub (contains `TODO` or throws
`UnsupportedOperationException`) then only a plain JUnit test is
created. Otherwise the test class is annotated with `@MicronautTest`
so it can be used as an integration test.
"""
from __future__ import annotations

import pathlib
import re
import subprocess

SOURCE_ROOT = pathlib.Path('src/main/java')
TEST_ROOT = pathlib.Path('src/test/java')

CLASS_RE = re.compile('public\s+(?:class|interface|enum)\s+(\w+)')
METHOD_RE = re.compile('public\s+[^;{]+?\s+(\w+)\s*\(')
PACKAGE_RE = re.compile('package\s+([\w\.]+);')
STUB_RE = re.compile(r'TODO|UnsupportedOperationException')

def ensure_tests_for(java_file: pathlib.Path) -> None:
    content = java_file.read_text(encoding='utf-8')
    package_match = PACKAGE_RE.search(content)
    package_name = package_match.group(1) if package_match else ''
    package_path = pathlib.Path(package_name.replace('.', '/')) if package_name else pathlib.Path()

    class_match = CLASS_RE.search(content)
    if not class_match:
        return
    class_name = class_match.group(1)
    methods = [m for m in METHOD_RE.findall(content) if m != class_name]

    test_dir = TEST_ROOT / package_path
    test_dir.mkdir(parents=True, exist_ok=True)
    test_path = test_dir / f'{class_name}Test.java'
    is_stub = bool(STUB_RE.search(content))

    if test_path.exists():
        test_content = test_path.read_text(encoding='utf-8')
    else:
        imports = ['import org.junit.jupiter.api.Test;']
        if not is_stub:
            imports.append('import io.micronaut.test.extensions.junit5.annotation.MicronautTest;')
        imports_line = '\n'.join(imports)
        package_line = f'package {package_name};\n\n' if package_name else ''
        annotation_line = '@MicronautTest\n' if not is_stub else ''
        test_content = (
            f"{package_line}{imports_line}\n\n"
            f"{annotation_line}class {class_name}Test {{\n"
            "}\n"
        )

    original = test_content
    for method in methods:
        if re.search(rf'\b{method}\s*\(', test_content):
            continue
        snippet = (
            f"    @Test\n"
            f"    void {method}() {{\n"
            f"        // TODO: implement test\n"
            f"    }}\n\n"
        )
        test_content = re.sub(r'\n}\s*$', f"\n{snippet}}}\n", test_content)

    if test_content != original or not test_path.exists():
        test_path.write_text(test_content, encoding='utf-8')
        try:
            subprocess.run(
                [
                    'mvn',
                    '-s', '.mvn/settings.xml',
                    '-q',
                    f'-Dtest={class_name}Test',
                    'test',
                ],
                check=True,
                cwd=pathlib.Path(__file__).resolve().parents[1],
            )
        except subprocess.CalledProcessError:
            test_path.unlink(missing_ok=True)
            raise


def main() -> None:
    for java in SOURCE_ROOT.rglob('*.java'):
        if java.name == 'package-info.java':
            continue
        ensure_tests_for(java)


if __name__ == '__main__':
    main()
