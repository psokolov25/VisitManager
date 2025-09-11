#!/usr/bin/env bash
set -euo pipefail

out_dir="docs/site"
out_file="$out_dir/index.html"

mkdir -p "$out_dir"

cat > "$out_file" <<'HTML'
<!doctype html>
<html lang="ru">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>VisitManager — Шпаргалка по curl</title>
  <style>
    :root { --bg:#0f1218; --fg:#e6edf3; --sec:#1f6feb; --mut:#9aa6b2; }
    body { margin:0; font:14px/1.5 system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; background:var(--bg); color:var(--fg); }
    header { padding:20px 24px; background:#0b0f14; position:sticky; top:0; border-bottom:1px solid #222; }
    header h1 { margin:0; font-size:18px; }
    main { padding: 20px 24px 60px; max-width: 1200px; }
    section { margin: 28px 0; }
    h2 { margin: 16px 0; border-left:3px solid var(--sec); padding-left:10px; }
    h3 { font-size: 15px; color: var(--sec); margin: 18px 0 8px; }
    article { background:#0b0f14; border:1px solid #1e232b; border-radius:6px; padding:12px 12px; margin:12px 0; }
    pre { background:#0c1117; border:1px solid #1f242c; padding:10px; border-radius:6px; overflow:auto; }
    code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace; font-size: 13px; }
    .file { color: var(--mut); font-size: 12px; margin: 4px 0 12px; }
    .legend { color: var(--mut); font-size: 13px; margin-top: 6px; }
    a { color: var(--sec); text-decoration: none; }
  </style>
  <meta name="robots" content="noindex" />
</head>
<body>
  <header>
    <h1>VisitManager — Шпаргалка по curl (из JavaDoc)</h1>
    <div class="legend">Генерация на основе комментариев «Пример curl» в контроллерах ru.aritmos.api</div>
  </header>
  <main>
HTML

# Iterate controllers
for f in src/main/java/ru/aritmos/api/*.java; do
  [ -f "$f" ] || continue
  base=$(basename "$f")
  # Skip package-info
  if [[ "$base" == "package-info.java" ]]; then continue; fi
  echo "  <section>" >> "$out_file"
  echo "    <h2>${base}</h2>" >> "$out_file"
  echo "    <div class=\"file\">${f}</div>" >> "$out_file"

  # Use awk to extract blocks
  awk -v file="$base" '
    BEGIN{ n=0 }
    { n++; l[n]=$0 }
    END{
      for (i=1; i<=n; i++) {
        if (index(l[i], "Пример curl")>0) {
          # find code block start/end
          start=-1; fin=-1
          for (j=i; j<=n; j++) { if (index(l[j], "<pre><code>")>0) { start=j+1; break } }
          for (k=start; k<=n; k++) { if (index(l[k], "</code></pre>")>0) { fin=k-1; break } }

          # find annotation (@Get/@Post/@Put/@Delete) and URI
          http=""; uri="";
          ann=-1
          for (a=fin+1; a<=n && a<=fin+40; a++) {
            if (l[a] ~ /@(Get|Post|Put|Delete)/) { ann=a; break }
          }
          if (ann>0) {
            if (l[ann] ~ /@Get/) http="GET"; else if (l[ann] ~ /@Post/) http="POST"; else if (l[ann] ~ /@Put/) http="PUT"; else if (l[ann] ~ /@Delete/) http="DELETE";
            # try inline string
            if (match(l[ann], /"([^"]+)"/, m)) { uri=m[1] }
            if (uri == "") {
              for (b=ann; b<=n && b<=ann+10; b++) {
                if (match(l[b], /uri[[:space:]]*=[[:space:]]*"([^"]+)"/, m2)) { uri=m2[1]; break }
              }
            }
          }

          # print article
          printf("    <article>\n")
          title = (http != "" || uri != "") ? http " " uri : "Пример curl"
          gsub(/&/, "&amp;", title); gsub(/</, "&lt;", title); gsub(/>/, "&gt;", title)
          printf("      <h3>%s</h3>\n", title)
          printf("      <pre><code>\n")
          for (c=start; c<=fin; c++) {
            line=l[c]; gsub(/&/, "&amp;", line); gsub(/</, "&lt;", line); gsub(/>/, "&gt;", line); printf("%s\n", line)
          }
          printf("      </code></pre>\n")
          printf("    </article>\n")

          i=fin
        }
      }
    }
  ' "$f" >> "$out_file"
  echo "  </section>" >> "$out_file"
done

cat >> "$out_file" <<'HTML'
    <section>
      <h2>Сводные материалы</h2>
      <article>
        <div class="legend">См. также:</div>
        <ul>
          <li><a href="../REST-Examples.md">REST-Examples.md</a> — примеры с переменными окружения</li>
          <li><a href="../postman/VisitManager.postman_collection.json">Postman collection</a> и <a href="../postman/VisitManager.env.postman_environment.json">environment</a></li>
        </ul>
      </article>
    </section>
  </main>
</body>
</html>
HTML

echo "Cheatsheet generated at $out_file"

