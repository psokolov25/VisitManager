import re, pathlib

root = pathlib.Path('src/main/java')
output = ["# Документация классов\n"]
comment_pattern = re.compile(
    r"/\*\*(?P<doc>.*?)\*/\s*(?P<code>(?:@\w+(?:\([^)]*\))?\s*)*(?:public|protected|private)[^{;]*[;{])",
    re.S,
)

def clean(doc: str) -> str:
    lines = [line.strip().lstrip('*').strip() for line in doc.splitlines()]
    return ' '.join(line for line in lines if line)

def squeeze(s: str) -> str:
    return ' '.join(part.strip() for part in s.split())

for path in sorted(root.rglob('*.java')):
    if path.name == 'package-info.java':
        continue
    text = path.read_text(encoding='utf-8')
    package = '.'.join(path.relative_to(root).with_suffix('').parts[:-1])
    current = None
    for match in comment_pattern.finditer(text):
        doc = clean(match.group('doc'))
        code = squeeze(match.group('code'))
        if any(kw in code for kw in [' class ', ' interface ', ' enum ']):
            m = re.search(r'(class|interface|enum)\s+(\w+)', code)
            if not m:
                continue
            class_name = m.group(2)
            current = {'name': class_name, 'doc': doc, 'ann': code.split(' ')[0], 'fields': [], 'methods': []}
            output.append(f"### {package}.{class_name}\n\n{doc}\n")
        elif code.endswith(';') and current:
            m = re.search(r'(?:public|protected|private)\s+(?:static\s+|final\s+|transient\s+|volatile\s+)*([^\s]+(?:\s*<[^>]+>)?)\s+(\w+)\s*;', code)
            if m:
                typ, name = squeeze(m.group(1)), m.group(2)
                note = ""
                if any(k in (current['ann'] + code) for k in ['@Getter', '@Data']):
                    note = " (Lombok: getter)"
                current['fields'].append(f"- `{typ} {name}` — {doc}{note}")
        elif code.endswith('{') and current:
            m = re.search(r'(?:public|protected|private)\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+)*([^\s]+(?:\s*<[^>]+>)?)\s+(\w+)\s*\(([^)]*)\)\s*{', code)
            if m:
                params = squeeze(m.group(3))
                current['methods'].append(f"- `{m.group(2)}({params})` — {doc}")
    # add fields and methods for last class
    if current:
        if current['fields']:
            output.append('#### Поля')
            output.extend(current['fields'])
            output.append('')
        if current['methods']:
            output.append('#### Методы')
            output.extend(current['methods'])
            output.append('')

pathlib.Path('docs/class-docs.md').write_text('\n'.join(output), encoding='utf-8')
