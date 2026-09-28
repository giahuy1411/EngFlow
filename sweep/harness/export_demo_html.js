#!/usr/bin/env node
/**
 * export_demo_html.js — xuất `docs/demo-engflow-4-chuc-nang.md` thành một trang HTML in được.
 *
 * VÌ SAO CẦN
 * ----------
 * Cẩm nang demo (`docs/demo-engflow-4-chuc-nang.md`) là tài liệu markdown dài (~3600 dòng) để
 * hội đồng đối chiếu khi bảo vệ. Khi cần **đọc trên máy hoặc in ra giấy**, markdown thô không
 * thuận tiện. Script này chuyển nó thành **một file HTML tự chứa** (không cần mạng để đọc),
 * có CSS in A4 và render sơ đồ Mermaid.
 *
 * CÁCH DÙNG (từ repo root)
 * ------------------------
 *   node sweep/harness/export_demo_html.js
 *   # hoặc chỉ định đường dẫn
 *   node sweep/harness/export_demo_html.js --in docs/demo-engflow-4-chuc-nang.md --out docs/demo-engflow-4-chuc-nang.html
 *
 * GHI CHÚ
 * -------
 * * Dùng `marked` đã có sẵn trong `frontend/node_modules` (không cài thêm gì).
 * * Sơ đồ Mermaid tải thư viện từ cdn.jsdelivr.net khi MỞ file (cần mạng cho 5 sơ đồ). Nếu
 *   offline, khối sơ đồ vẫn hiện ở dạng văn bản thô — tài liệu vẫn đọc được.
 * * Mỗi Chương / Phụ lục tự ngắt sang trang mới khi in (CSS `break-before: page`).
 * * Sinh lại sau mỗi lần sửa file markdown để bản HTML không lệch.
 */
"use strict";

const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..", "..");

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

const IN = path.resolve(ROOT, arg("in", "docs/demo-engflow-4-chuc-nang.md"));
const OUT = path.resolve(ROOT, arg("out", "docs/demo-engflow-4-chuc-nang.html"));

// `marked` nằm trong frontend/node_modules (đã có sẵn, không cài thêm).
let marked;
try {
  ({ marked } = require(path.join(ROOT, "frontend", "node_modules", "marked")));
} catch (e) {
  console.error("Không tìm thấy `marked` trong frontend/node_modules. Chạy `npm install` trong frontend/ trước.");
  process.exit(1);
}

const md = fs.readFileSync(IN, "utf-8");

// ── Renderer: giữ code block thành <pre><code>, riêng ```mermaid -> <pre class="mermaid"> ──
const renderer = new marked.Renderer();
renderer.code = function (codeOrToken, langMaybe) {
  // marked 14 truyền một token object; bản cũ hơn truyền (code, lang).
  const isTok = codeOrToken && typeof codeOrToken === "object";
  const code = isTok ? codeOrToken.text : codeOrToken;
  const lang = isTok ? codeOrToken.lang : langMaybe;
  if (lang === "mermaid") {
    return '<pre class="mermaid">' + escapeHtml(code) + "</pre>\n";
  }
  const cls = lang ? ' class="language-' + escapeAttr(lang) + '"' : "";
  return "<pre><code" + cls + ">" + escapeHtml(code) + "</code></pre>\n";
};

function escapeHtml(s) {
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function escapeAttr(s) {
  return String(s).replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;");
}

const body = marked.parse(md, { renderer, gfm: true, breaks: false });

// Tiêu đề lấy từ H1 đầu tiên.
const h1 = (md.match(/^#\s+(.+)$/m) || [, "EngFlow — Cẩm nang demo"])[1].trim();

const html = `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>${escapeHtml(h1)}</title>
<!--
  Sinh tự động từ docs/demo-engflow-4-chuc-nang.md
  Sinh lại: node sweep/harness/export_demo_html.js
  ĐỪNG sửa tay file này — sửa file .md rồi chạy lại script.
-->
<style>
  :root {
    --ink: #1e293b;
    --muted: #475569;
    --line: #cbd5e1;
    --accent: #6d28d9;
    --bg: #ffffff;
    --code-bg: #f6f7fb;
  }
  * { box-sizing: border-box; }
  html { -webkit-text-size-adjust: 100%; }
  body {
    margin: 0;
    color: var(--ink);
    background: var(--bg);
    font-family: "Be Vietnam Pro", "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    font-size: 15px;
    line-height: 1.62;
  }
  .wrap { max-width: 960px; margin: 0 auto; padding: 32px 24px 96px; }
  h1 { font-size: 28px; line-height: 1.25; font-weight: 800; margin: 0 0 8px; }
  h2 {
    font-size: 22px; font-weight: 800; margin: 40px 0 12px;
    padding-bottom: 6px; border-bottom: 2px solid var(--ink);
  }
  h3 { font-size: 18px; font-weight: 700; margin: 26px 0 8px; color: var(--accent); }
  h4 { font-size: 16px; font-weight: 700; margin: 20px 0 6px; }
  p, li { color: var(--ink); }
  a { color: var(--accent); }
  code {
    font-family: "JetBrains Mono", "Cascadia Code", Consolas, "Courier New", monospace;
    font-size: 13px;
    background: var(--code-bg);
    padding: 1px 5px;
    border-radius: 4px;
    word-break: break-word;
  }
  pre {
    background: var(--code-bg);
    border: 1px solid var(--line);
    border-left: 4px solid var(--accent);
    border-radius: 6px;
    padding: 12px 14px;
    overflow-x: auto;
    page-break-inside: avoid;
  }
  pre code { background: none; padding: 0; font-size: 12.5px; line-height: 1.5; }
  pre.mermaid { background: #fff; border-left-color: var(--line); text-align: center; }
  table {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0 18px;
    font-size: 13.5px;
    page-break-inside: avoid;
  }
  th, td { border: 1px solid var(--line); padding: 6px 9px; text-align: left; vertical-align: top; }
  th { background: #eef2ff; font-weight: 700; }
  tr:nth-child(even) td { background: #fafbff; }
  blockquote {
    margin: 12px 0;
    padding: 10px 14px;
    border-left: 4px solid var(--accent);
    background: #faf8ff;
    color: var(--muted);
    border-radius: 0 6px 6px 0;
  }
  blockquote p { margin: 4px 0; }
  hr { border: none; border-top: 1px solid var(--line); margin: 28px 0; }
  .toolbar {
    position: sticky; top: 0; z-index: 5;
    background: rgba(255,255,255,.94);
    backdrop-filter: blur(6px);
    border-bottom: 1px solid var(--line);
    padding: 8px 24px; margin: 0 -24px 20px;
    font-size: 13px; color: var(--muted);
    display: flex; gap: 12px; align-items: center; flex-wrap: wrap;
  }
  .toolbar button {
    font: inherit; font-weight: 700; cursor: pointer;
    border: 2px solid var(--ink); background: #fff; color: var(--ink);
    padding: 4px 12px; border-radius: 999px;
  }
  .toolbar button:hover { background: var(--ink); color: #fff; }

  /* ── In ấn: A4, mỗi Chương/Phụ lục sang trang mới ── */
  @page { size: A4; margin: 14mm 12mm; }
  @media print {
    body { font-size: 11.5px; line-height: 1.5; }
    .wrap { max-width: none; padding: 0; }
    .toolbar { display: none; }
    h1 { font-size: 22px; }
    h2 { font-size: 17px; margin-top: 0; break-before: page; }
    h2:first-of-type { break-before: avoid; }
    h3 { font-size: 14px; break-after: avoid; }
    h4 { font-size: 12.5px; break-after: avoid; }
    a { color: inherit; text-decoration: none; }
    pre, table, blockquote { break-inside: avoid; }
    pre code { font-size: 10px; }
    table { font-size: 10.5px; }
    thead { display: table-header-group; }
    tr { break-inside: avoid; }
  }
</style>
</head>
<body>
<div class="wrap">
  <div class="toolbar">
    <span>Bản HTML in được — sinh từ <code>docs/demo-engflow-4-chuc-nang.md</code></span>
    <button type="button" onclick="window.print()">🖨️ In / Lưu PDF</button>
  </div>
${body}
</div>

<!-- Mermaid: render 5 sơ đồ. Offline thì khối văn bản thô vẫn đọc được. -->
<script type="module">
  try {
    const mermaid = (await import("https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs")).default;
    mermaid.initialize({ startOnLoad: false, theme: "neutral", securityLevel: "loose" });
    await mermaid.run({ querySelector: "pre.mermaid" });
  } catch (e) {
    // Không có mạng hoặc CDN chặn: giữ nguyên khối <pre> văn bản.
    console.warn("Mermaid không tải được (offline?) — sơ đồ hiển thị dạng văn bản.", e);
  }
</script>
</body>
</html>
`;

fs.writeFileSync(OUT, html, "utf-8");
console.log("Đã xuất: " + path.relative(ROOT, OUT).replace(/\\/g, "/"));
console.log("  nguồn : " + path.relative(ROOT, IN).replace(/\\/g, "/"));
console.log("  kích thước: " + (Buffer.byteLength(html, "utf-8") / 1024).toFixed(1) + " KB");
