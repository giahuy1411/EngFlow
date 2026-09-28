#!/usr/bin/env node
/**
 * export_demo_mobile.js — xuất `docs/demo-engflow-4-chuc-nang.md` thành bản HTML **tối ưu cho điện thoại**.
 *
 * KHÁC GÌ BẢN HTML IN (`export_demo_html.js`)
 * -------------------------------------------
 * * Bản in  → khổ A4, mỗi chương sang trang, để **in ra giấy**.
 * * Bản này → bố cục **một cột hẹp**, chữ to, khoảng thở rộng, **menu mục lục trượt**, khối code
 *   **gấp/mở được**, bảng **vuốt ngang**, nút **lên đầu trang**, **lưu vị trí đọc** — để **đọc/học
 *   trên điện thoại**.
 *
 * CẢ HAI đều đọc từ **cùng một file nguồn** `docs/demo-engflow-4-chuc-nang.md`, nên nội dung
 * luôn đầy đủ và khớp nhau. Sửa .md rồi chạy lại script.
 *
 * CÁCH DÙNG (từ repo root)
 * ------------------------
 *   node sweep/harness/export_demo_mobile.js
 *   # hoặc chỉ định đường dẫn
 *   node sweep/harness/export_demo_mobile.js --in docs/demo-engflow-4-chuc-nang.md --out docs/demo-engflow-4-chuc-nang-mobile.html
 *
 * GHI CHÚ
 * -------
 * * Dùng `marked` có sẵn trong `frontend/node_modules` (không cài thêm gì).
 * * Sơ đồ Mermaid tải từ cdn.jsdelivr.net khi MỞ file (cần mạng cho 5 sơ đồ). Offline thì khối
 *   sơ đồ hiện dạng văn bản thô — tài liệu vẫn đọc được.
 * * `localStorage` chỉ lưu **vị trí đọc gần nhất** (tiện lợi riêng của từng máy); bọc try/catch,
 *   trình duyệt chặn thì bỏ qua, trang vẫn chạy bình thường.
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
const OUT = path.resolve(ROOT, arg("out", "docs/demo-engflow-4-chuc-nang-mobile.html"));

let marked;
try {
  ({ marked } = require(path.join(ROOT, "frontend", "node_modules", "marked")));
} catch (e) {
  console.error("Không tìm thấy `marked` trong frontend/node_modules. Chạy `npm install` trong frontend/ trước.");
  process.exit(1);
}

const md = fs.readFileSync(IN, "utf-8");

// ── Tiện ích ────────────────────────────────────────────────────────────────
function escapeHtml(s) {
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function escapeAttr(s) {
  return String(s).replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;");
}
/** Bỏ cú pháp markdown để lấy chữ thuần cho mục lục. */
function plain(s) {
  return String(s)
    .replace(/`([^`]*)`/g, "$1")
    .replace(/\*\*([^*]*)\*\*/g, "$1")
    .replace(/\*([^*]*)\*/g, "$1")
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .trim();
}
/** Sinh id ASCII ổn định từ tiêu đề (bỏ dấu tiếng Việt). */
function slug(s) {
  return plain(s)
    .normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d").replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60) || "muc";
}

// ── 1) Quét tiêu đề để dựng mục lục (H2 + H3) ───────────────────────────────
const tokens = marked.lexer(md);
const toc = [];
const usedIds = new Set();
function uniq(id) {
  let x = id, n = 2;
  while (usedIds.has(x)) x = id + "-" + n++;
  usedIds.add(x);
  return x;
}
for (const t of tokens) {
  if (t.type === "heading" && (t.depth === 2 || t.depth === 3)) {
    toc.push({ depth: t.depth, text: plain(t.text), id: uniq(slug(t.text)) });
  }
}

// ── 2) Renderer ─────────────────────────────────────────────────────────────
// Gán id cho heading khớp với mục lục, theo thứ tự xuất hiện.
let headingCursor = 0;
const renderer = new marked.Renderer();

const defHeading = renderer.heading.bind(renderer);
renderer.heading = function (tok) {
  const html = defHeading(tok);
  if (tok.depth === 2 || tok.depth === 3) {
    const entry = toc[headingCursor++];
    if (entry) {
      const lvl = tok.depth === 2 ? "2" : "3";
      return html.replace(/^<h([23])/, `<h$1 id="${entry.id}" data-lvl="${lvl}"`);
    }
  }
  return html;
};

// Code: ```mermaid -> khối sơ đồ; còn lại -> <pre><code>, gấp lại nếu dài.
renderer.code = function (codeOrToken, langMaybe) {
  const isTok = codeOrToken && typeof codeOrToken === "object";
  const code = isTok ? codeOrToken.text : codeOrToken;
  const lang = isTok ? codeOrToken.lang : langMaybe;

  if (lang === "mermaid") {
    return '<pre class="mermaid">' + escapeHtml(code) + "</pre>\n";
  }
  const cls = lang ? ' class="language-' + escapeAttr(lang) + '"' : "";
  const inner = "<pre><code" + cls + ">" + escapeHtml(code) + "</code></pre>";
  const lines = code.split("\n").length;
  // Khối dài (>18 dòng) gấp lại mặc định cho gọn màn hình điện thoại.
  if (lines > 18) {
    const label = (lang ? lang + " · " : "") + lines + " dòng · bấm để mở";
    return '<details class="code-block"><summary>' + escapeHtml(label) + "</summary>" + inner + "</details>\n";
  }
  return inner + "\n";
};

const body = marked.parse(md, { renderer, gfm: true, breaks: false });

// ── 3) Bọc mỗi bảng vào khung vuốt ngang (bảng rộng không phá layout) ───────
const bodyWrapped = body.replace(/<table>/g, '<div class="table-scroll"><table>').replace(/<\/table>/g, "</table></div>");

// ── 4) Mục lục HTML ─────────────────────────────────────────────────────────
const tocHtml = toc
  .map((h) => {
    const cls = h.depth === 2 ? "toc-h2" : "toc-h3";
    return `<a class="${cls}" href="#${h.id}">${escapeHtml(h.text)}</a>`;
  })
  .join("\n      ");

const h1 = (md.match(/^#\s+(.+)$/m) || [, "EngFlow — Cẩm nang demo"])[1].trim();

const html = `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
<meta name="theme-color" content="#6d28d9" />
<meta name="color-scheme" content="light dark" />
<meta name="description" content="Cẩm nang demo 4 chức năng EngFlow — bản đọc trên điện thoại." />
<title>${escapeHtml(h1)}</title>
<!--
  Bản MOBILE — sinh tự động từ docs/demo-engflow-4-chuc-nang.md
  Sinh lại: node sweep/harness/export_demo_mobile.js
  ĐỪNG sửa tay file này — sửa file .md rồi chạy lại script.
-->
<style>
  :root {
    --ink: #1e293b;
    --muted: #5b6b82;
    --line: #d8dfea;
    --accent: #6d28d9;
    --accent-soft: #f3efff;
    --bg: #ffffff;
    --card: #f8fafc;
    --code-bg: #f5f6fb;
    --ok: #047857;
    --warn: #b45309;
    --radius: 14px;
    --pad: 16px;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --ink: #e7ecf5;
      --muted: #9fb0c7;
      --line: #2b3648;
      --accent: #b79cff;
      --accent-soft: #211a33;
      --bg: #0f1420;
      --card: #171e2c;
      --code-bg: #131a27;
      --ok: #34d399;
      --warn: #fbbf24;
    }
  }
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  html { -webkit-text-size-adjust: 100%; scroll-behavior: smooth; }
  body {
    margin: 0;
    background: var(--bg);
    color: var(--ink);
    font-family: "Be Vietnam Pro", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    font-size: 17px;
    line-height: 1.72;
    overflow-wrap: break-word;
  }
  .wrap { max-width: 720px; margin: 0 auto; padding: 0 var(--pad) 120px; }

  /* ── Thanh trên: tiêu đề + nút menu + tiến độ đọc ── */
  .topbar {
    position: sticky; top: 0; z-index: 40;
    display: flex; align-items: center; gap: 10px;
    padding: 10px var(--pad);
    padding-top: max(10px, env(safe-area-inset-top));
    background: color-mix(in srgb, var(--bg) 92%, transparent);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid var(--line);
  }
  .topbar .ttl {
    flex: 1; min-width: 0;
    font-size: 14px; font-weight: 700; color: var(--muted);
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }
  .iconbtn {
    flex: 0 0 auto;
    display: inline-flex; align-items: center; justify-content: center;
    min-width: 44px; min-height: 44px;
    border: 2px solid var(--ink); background: var(--bg); color: var(--ink);
    border-radius: 12px; font-size: 18px; font-weight: 800; cursor: pointer;
  }
  .iconbtn:active { transform: scale(.96); }
  #progress {
    position: fixed; top: 0; left: 0; height: 3px; width: 0%;
    background: var(--accent); z-index: 50; transition: width .1s linear;
  }

  /* ── Mục lục trượt ── */
  .scrim {
    position: fixed; inset: 0; background: rgba(0,0,0,.45);
    opacity: 0; pointer-events: none; transition: opacity .22s; z-index: 60;
  }
  .scrim.open { opacity: 1; pointer-events: auto; }
  .drawer {
    position: fixed; top: 0; left: 0; bottom: 0; width: min(86vw, 360px);
    background: var(--bg); border-right: 1px solid var(--line);
    transform: translateX(-102%); transition: transform .26s cubic-bezier(.2,.7,.3,1);
    z-index: 70; display: flex; flex-direction: column;
    padding-top: max(12px, env(safe-area-inset-top));
  }
  .drawer.open { transform: none; }
  .drawer h2 {
    margin: 6px 16px 10px; font-size: 15px; text-transform: uppercase; letter-spacing: .04em;
    color: var(--muted); border: 0; padding: 0;
  }
  .drawer nav { overflow-y: auto; -webkit-overflow-scrolling: touch; padding: 0 8px 24px; }
  .drawer a {
    display: block; padding: 10px 12px; border-radius: 10px;
    color: var(--ink); text-decoration: none; font-size: 15px; line-height: 1.4;
  }
  .drawer a:active { background: var(--accent-soft); }
  .drawer a.toc-h2 { font-weight: 800; margin-top: 6px; }
  .drawer a.toc-h3 { padding-left: 24px; color: var(--muted); font-size: 14px; }

  /* ── Nội dung ── */
  .wrap h1 { font-size: 26px; line-height: 1.3; font-weight: 800; margin: 20px 0 12px; }
  .wrap h2 {
    font-size: 22px; font-weight: 800; line-height: 1.35;
    margin: 40px 0 12px; padding-bottom: 8px;
    border-bottom: 2px solid var(--ink);
    scroll-margin-top: 64px;
  }
  .wrap h3 {
    font-size: 19px; font-weight: 800; color: var(--accent);
    margin: 30px 0 8px; scroll-margin-top: 64px;
  }
  .wrap h4 { font-size: 17px; font-weight: 700; margin: 22px 0 6px; }
  .wrap p { margin: 12px 0; }
  .wrap ul, .wrap ol { padding-left: 22px; }
  .wrap li { margin: 6px 0; }
  a { color: var(--accent); }

  code {
    font-family: ui-monospace, "SF Mono", "Cascadia Code", Consolas, monospace;
    font-size: .86em; background: var(--code-bg); color: var(--ink);
    padding: 2px 6px; border-radius: 6px; word-break: break-word;
  }
  pre {
    margin: 0; padding: 14px; overflow-x: auto; -webkit-overflow-scrolling: touch;
    background: var(--code-bg); border: 1px solid var(--line); border-radius: var(--radius);
  }
  pre code { background: none; padding: 0; font-size: 13px; line-height: 1.55; white-space: pre; }

  /* Khối code gấp/mở */
  details.code-block {
    margin: 14px 0; border: 1px solid var(--line); border-radius: var(--radius);
    background: var(--code-bg); overflow: hidden;
  }
  details.code-block > summary {
    list-style: none; cursor: pointer; padding: 12px 14px;
    font-size: 14px; font-weight: 700; color: var(--accent);
    display: flex; align-items: center; gap: 8px; min-height: 44px;
  }
  details.code-block > summary::-webkit-details-marker { display: none; }
  details.code-block > summary::before { content: "▸"; font-size: 13px; }
  details.code-block[open] > summary::before { content: "▾"; }
  details.code-block > pre { border: 0; border-top: 1px solid var(--line); border-radius: 0; }

  /* Sơ đồ mermaid: giữ kích thước đọc được, cuộn ngang (không bị thu nhỏ li ti) */
  pre.mermaid {
    background: var(--bg); border: 1px solid var(--line);
    text-align: center; overflow-x: auto; -webkit-overflow-scrolling: touch; padding: 8px;
  }
  pre.mermaid svg { height: auto; }

  /* Bảng: giữ độ rộng tự nhiên (không bóp cột), cuộn ngang cả khối */
  .table-scroll {
    margin: 14px 0; overflow-x: auto; -webkit-overflow-scrolling: touch;
    border: 1px solid var(--line); border-radius: var(--radius);
  }
  table { border-collapse: collapse; width: max-content; min-width: 100%; font-size: 14.5px; }
  th, td { border-bottom: 1px solid var(--line); padding: 9px 12px; text-align: left; vertical-align: top; }
  th { background: var(--accent-soft); font-weight: 800; white-space: nowrap; }
  /* Ô chữ dài vẫn xuống dòng gọn; ô ngắn (số, mã) đứng một dòng, không bị bóp thành cột dọc */
  td { max-width: 72vw; }
  /* Cột đầu thường là "#"/"Method"/"ID"/"Bước" — giữ một dòng cho khỏi bóp chữ */
  td:first-child, th:first-child { white-space: nowrap; }
  tbody tr:nth-child(even) td { background: var(--card); }

  /* Trích dẫn / ghi chú */
  blockquote {
    margin: 14px 0; padding: 12px 14px;
    background: var(--card); border-left: 4px solid var(--accent);
    border-radius: 0 var(--radius) var(--radius) 0; color: var(--muted);
  }
  blockquote p { margin: 6px 0; }

  /* Checkbox (checklist Phụ lục C) — chạm được */
  .wrap li > input[type="checkbox"] {
    width: 20px; height: 20px; margin-right: 8px; vertical-align: -4px;
    accent-color: var(--accent);
  }

  hr { border: none; border-top: 1px solid var(--line); margin: 28px 0; }

  /* Nút lên đầu trang */
  #toTop {
    position: fixed; right: 16px; bottom: max(16px, env(safe-area-inset-bottom));
    width: 48px; height: 48px; border-radius: 50%;
    border: 2px solid var(--ink); background: var(--bg); color: var(--ink);
    font-size: 20px; font-weight: 800; cursor: pointer; z-index: 45;
    box-shadow: 0 6px 18px rgba(0,0,0,.16);
    opacity: 0; pointer-events: none; transition: opacity .2s;
  }
  #toTop.show { opacity: 1; pointer-events: auto; }

  .footnote { margin: 32px 0 0; font-size: 13px; color: var(--muted); text-align: center; }

  /* ── Khi in: giữ được, mỗi chương sang trang ── */
  @page { size: A4; margin: 14mm 12mm; }
  @media print {
    body { font-size: 11.5px; }
    .topbar, .drawer, .scrim, #toTop, #progress { display: none !important; }
    .wrap { max-width: none; padding: 0; }
    details.code-block { break-inside: avoid; }
    details.code-block[open] > pre { display: block; }
    h2 { break-before: page; }
    h2:first-of-type { break-before: avoid; }
    pre, table { break-inside: avoid; }
  }
</style>
</head>
<body>
<div id="progress"></div>

<div class="topbar">
  <button class="iconbtn" id="menuBtn" aria-label="Mở mục lục" aria-controls="drawer" aria-expanded="false">☰</button>
  <span class="ttl">${escapeHtml(h1)}</span>
</div>

<div class="scrim" id="scrim"></div>
<aside class="drawer" id="drawer" aria-label="Mục lục">
  <h2>Mục lục</h2>
  <nav>
    ${tocHtml}
  </nav>
</aside>

<main class="wrap">
${bodyWrapped}
  <p class="footnote">Bản mobile — sinh tự động từ <code>docs/demo-engflow-4-chuc-nang.md</code>.</p>
</main>

<button id="toTop" aria-label="Lên đầu trang">↑</button>

<!-- Mermaid: render sơ đồ. Offline thì giữ khối văn bản thô. -->
<script type="module">
  try {
    const mermaid = (await import("https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs")).default;
    const dark = matchMedia("(prefers-color-scheme: dark)").matches;
    mermaid.initialize({ startOnLoad: false, theme: dark ? "dark" : "neutral", securityLevel: "loose" });
    await mermaid.run({ querySelector: "pre.mermaid" });
    // Sơ đồ tuần tự rất rộng: giữ kích thước ĐỌC ĐƯỢC (không thu nhỏ li ti) rồi để khối cuộn ngang.
    document.querySelectorAll("pre.mermaid svg").forEach(function (svg) {
      var vb = svg.getAttribute("viewBox");
      if (!vb) return;
      var natural = parseFloat(vb.split(/\\s+/)[2]);
      if (natural && natural > window.innerWidth) {
        var w = Math.min(natural, 1600);
        svg.style.width = w + "px";
        svg.style.maxWidth = "none";
      }
    });
  } catch (e) {
    console.warn("Mermaid không tải được (offline?) — sơ đồ hiển thị dạng văn bản.", e);
  }
</script>

<script>
(function () {
  var drawer = document.getElementById("drawer");
  var scrim = document.getElementById("scrim");
  var menuBtn = document.getElementById("menuBtn");
  var toTop = document.getElementById("toTop");
  var bar = document.getElementById("progress");

  function setDrawer(open) {
    drawer.classList.toggle("open", open);
    scrim.classList.toggle("open", open);
    menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
  }
  menuBtn.addEventListener("click", function () { setDrawer(!drawer.classList.contains("open")); });
  scrim.addEventListener("click", function () { setDrawer(false); });
  document.addEventListener("keydown", function (e) { if (e.key === "Escape") setDrawer(false); });
  // Chạm một mục trong mục lục -> đóng drawer rồi mới nhảy tới mục.
  drawer.addEventListener("click", function (e) {
    if (e.target.closest("a")) setDrawer(false);
  });

  function onScroll() {
    var doc = document.documentElement;
    var max = doc.scrollHeight - doc.clientHeight;
    var y = window.scrollY || doc.scrollTop;
    bar.style.width = (max > 0 ? (y / max) * 100 : 0) + "%";
    toTop.classList.toggle("show", y > 600);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  toTop.addEventListener("click", function () { window.scrollTo({ top: 0, behavior: "smooth" }); });

  // Nhớ vị trí đọc (tiện lợi riêng của máy này). Lỗi thì bỏ qua, không chặn trang.
  var KEY = "engflow-demo-mobile-scroll";
  try {
    var saved = parseFloat(localStorage.getItem(KEY) || "0");
    if (saved > 200) window.scrollTo(0, saved);
  } catch (e) {}
  var t = null;
  window.addEventListener("scroll", function () {
    if (t) return;
    t = setTimeout(function () {
      t = null;
      try { localStorage.setItem(KEY, String(window.scrollY || 0)); } catch (e) {}
    }, 400);
  }, { passive: true });

  onScroll();
})();
</script>
</body>
</html>
`;

fs.writeFileSync(OUT, html, "utf-8");
console.log("Đã xuất: " + path.relative(ROOT, OUT).replace(/\\/g, "/"));
console.log("  nguồn : " + path.relative(ROOT, IN).replace(/\\/g, "/"));
console.log("  mục lục: " + toc.length + " mục (H2+H3)");
console.log("  kích thước: " + (Buffer.byteLength(html, "utf-8") / 1024).toFixed(1) + " KB");
