package ru.aritmos.docs;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Генератор HTML-шпаргалки по curl-примерам, извлечённым из JavaDoc контроллеров ru.aritmos.api.
 * <p>
 * Использует маркеры "Пример curl", <pre><code> и </code></pre> в комментариях.
 */
public final class CurlCheatsheetGenerator {

  /**
   * Точка входа генератора HTML‑шпаргалки по curl.
   *
   * @param args не используются
   * @throws IOException при ошибке чтения/записи файлов
   */
  @SuppressWarnings("all")
  public static void main(String[] args) throws IOException {
    Path projectDir = Paths.get("");
    Path controllersDir = projectDir.resolve("src/main/java/ru/aritmos/api");
    Path outDir = projectDir.resolve("docs/site");
    Path outFile = outDir.resolve("index.html");

    Files.createDirectories(outDir);
    StringBuilder html = new StringBuilder();
    html.append("<!doctype html>\n")
        .append("<html lang=\"ru\">\n<head>\n<meta charset=\"UTF-8\"/>\n")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n")
        .append("<title>VisitManager — Шпаргалка по curl</title>\n")
        .append("<style>:root{--bg:#0f1218;--fg:#e6edf3;--sec:#1f6feb;--mut:#9aa6b2}body{margin:0;font:14px/1.5 system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;background:var(--bg);color:var(--fg)}header{padding:20px 24px;background:#0b0f14;position:sticky;top:0;border-bottom:1px solid #222}header h1{margin:0;font-size:18px}main{padding:20px 24px 60px;max-width:1200px}section{margin:28px 0}h2{margin:16px 0;border-left:3px solid var(--sec);padding-left:10px}h3{font-size:15px;color:var(--sec);margin:18px 0 8px}article{background:#0b0f14;border:1px solid #1e232b;border-radius:6px;padding:12px 12px;margin:12px 0}pre{background:#0c1117;border:1px solid #1f242c;padding:10px;border-radius:6px;overflow:auto}code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,\"Liberation Mono\",monospace;font-size:13px}.file{color:var(--mut);font-size:12px;margin:4px 0 12px}.legend{color:var(--mut);font-size:13px;margin-top:6px}a{color:var(--sec);text-decoration:none}</style>\n")
        .append("<meta name=\"robots\" content=\"noindex\"/>\n</head>\n<body>\n<header>\n")
        .append("<h1>VisitManager — Шпаргалка по curl (из JavaDoc)</h1>\n")
        .append("<div class=\"legend\">Генерация на основе комментариев «Пример curl» в контроллерах ru.aritmos.api</div>\n")
        .append("</header>\n<main>\n");

    if (Files.exists(controllersDir)) {
      Files.list(controllersDir)
          .filter(p -> p.toString().endsWith(".java"))
          .filter(p -> !p.getFileName().toString().equals("package-info.java"))
          .sorted()
          .forEach(
              p -> {
                try {
                  appendControllerSection(html, p);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    html.append("<section><h2>Сводные материалы</h2><article><div class=\"legend\">См. также:</div><ul>")
        .append("<li><a href=\"../REST-Examples.md\">REST-Examples.md</a></li>")
        .append(
            "<li><a href=\"../postman/VisitManager.postman_collection.json\">Postman collection</a> и <a href=\"../postman/VisitManager.env.postman_environment.json\">environment</a></li>")
        .append("</ul></article></section>");

    html.append("</main>\n</body>\n</html>\n");

    try (BufferedWriter bw =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFile.toFile()), StandardCharsets.UTF_8))) {
      bw.write(html.toString());
    }
    System.out.println("Cheatsheet generated at " + outFile.toAbsolutePath());
  }

  /**
   * Добавить секцию контроллера с извлечёнными примерами curl.
   * Источник — комментарии JavaDoc в файле контроллера.
   *
   * @param html буфер HTML
   * @param file путь к файлу контроллера
   * @throws IOException ошибка чтения файла
   */
  private static void appendControllerSection(StringBuilder html, Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    String base = file.getFileName().toString();
    html.append("  <section>\n");
    html.append("    <h2>").append(escape(base)).append("</h2>\n");
    html.append("    <div class=\"file\">").append(escape(file.toString())).append("</div>\n");

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (!line.contains("Пример curl")) continue;

      // find <pre><code> start and </code></pre> end
      int start = -1, fin = -1;
      for (int j = i; j < Math.min(lines.size(), i + 100); j++) {
        if (lines.get(j).contains("<pre><code>")) {
          start = j + 1;
          break;
        }
      }
      for (int j = start; j < Math.min(lines.size(), start + 200); j++) {
        if (lines.get(j).contains("</code></pre>")) {
          fin = j - 1;
          break;
        }
      }
      if (start == -1 || fin == -1) continue;

      // find annotation and uri nearby
      String http = "";
      String uri = "";
      for (int a = fin + 1; a < Math.min(lines.size(), fin + 40); a++) {
        String la = lines.get(a);
        if (la.contains("@Get(") || la.contains("@Post(") || la.contains("@Put(") || la.contains("@Delete(")) {
          if (la.contains("@Get(")) http = "GET";
          else if (la.contains("@Post(")) http = "POST";
          else if (la.contains("@Put(")) http = "PUT";
          else if (la.contains("@Delete(")) http = "DELETE";
          // try to extract inline string
          int q1 = la.indexOf('"');
          int q2 = la.indexOf('"', q1 + 1);
          if (q1 >= 0 && q2 > q1) {
            uri = la.substring(q1 + 1, q2);
          } else {
            // look ahead for uri = "..."
            for (int b = a; b < Math.min(lines.size(), a + 10); b++) {
              String lb = lines.get(b);
              int idx = lb.indexOf("uri");
              if (idx >= 0) {
                int s = lb.indexOf('"');
                int e = lb.indexOf('"', s + 1);
                if (s >= 0 && e > s) {
                  uri = lb.substring(s + 1, e);
                  break;
                }
              }
            }
          }
          break;
        }
      }

      html.append("    <article>\n");
      String title = (http.isEmpty() && uri.isEmpty()) ? "Пример curl" : (http + " " + uri);
      html.append("      <h3>").append(escape(title)).append("</h3>\n");
      html.append("      <pre><code>\n");
      for (int c = start; c <= fin; c++) {
        html.append(escape(lines.get(c))).append("\n");
      }
      html.append("      </code></pre>\n");
      html.append("    </article>\n");

      i = fin;
    }
    html.append("  </section>\n");
  }

  /**
   * Экранировать спецсимволы HTML.
   *
   * @param s исходная строка
   * @return экранированная строка
   */
  private static String escape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
