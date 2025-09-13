import re
import pathlib

root = pathlib.Path('src/main/java')
output = ['# Документация классов\n']
pattern = re.compile(r"/\*\*(.*?)\*/\s*(?:@[^\n]*\n\s*)*(?:public\s+)?(?:class|interface|enum)\s+(\w+)", re.S)
for path in sorted(root.rglob('*.java')):
    if path.name == 'package-info.java':
        continue
    text = path.read_text(encoding='utf-8')
    m = pattern.search(text)
    if not m:
        continue
    doc, class_name = m.group(1), m.group(2)
    doc = re.split(r"\n\s*@", doc.strip())[0]
    lines = [line.strip().lstrip('*').strip() for line in doc.splitlines()]
    doc_clean = ' '.join(line for line in lines if line)
    package = '.'.join(path.relative_to(root).with_suffix('').parts[:-1])
    output.append(f"### {package}.{class_name}\n\n{doc_clean}\n")
pathlib.Path('docs/class-docs.md').write_text('\n'.join(output), encoding='utf-8')
