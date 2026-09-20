/**
 * Dem chinh xac so lan moi font-weight xuat hien trong SOURCE.
 * Day la phep do ma comment audit-v7 trong index.html da lam (va ket luan
 * "800 = 0 luot dung"). Ket luan do bay gio con dung khong?
 */
const fs = require("fs");
const path = require("path");

const SRC = path.join(__dirname, "..", "..", "frontend", "src");
const files = [];
(function walk(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p);
    else if (/\.(vue|css|js)$/.test(e.name) && !/\.test\.js$/.test(e.name)) files.push(p);
  }
})(SRC);

const hits = {};
const byFile = {};
for (const f of files) {
  const src = fs.readFileSync(f, "utf8");
  // `font-weight: 800` trong CSS, va `font-extrabold` cua Tailwind
  for (const m of src.matchAll(/font-weight:\s*(\d{3})/g)) {
    hits[m[1]] = (hits[m[1]] || 0) + 1;
    (byFile[m[1]] = byFile[m[1]] || []).push(path.relative(SRC, f));
  }
  for (const m of src.matchAll(/font-(thin|extralight|light|normal|medium|semibold|bold|extrabold|black)\b/g)) {
    const map = { thin: "100", extralight: "200", light: "300", normal: "400",
      medium: "500", semibold: "600", bold: "700", extrabold: "800", black: "900" };
    const w = map[m[1]];
    hits[w] = (hits[w] || 0) + 1;
    (byFile[w] = byFile[w] || []).push(path.relative(SRC, f) + " [font-" + m[1] + "]");
  }
}

console.log("=== so lan moi font-weight xuat hien trong source ===");
console.log("(chi tinh .vue/.css/.js, KHONG tinh file test)");
console.log("");
const loaded = ["400", "500", "600", "700", "900"];
const sorted = Object.keys(hits).sort((a, b) => hits[b] - hits[a]);
for (const w of sorted) {
  const isLoaded = loaded.includes(w);
  console.log(`  ${w.padEnd(4)} ${String(hits[w]).padStart(4)} luot   ${isLoaded ? "[CO trong URL]" : "[*** KHONG CO trong URL ***]"}`);
}
console.log("");
console.log("=== chi tiet weight 800 (neu co) ===");
if (byFile["800"]) {
  const uniq = {};
  for (const f of byFile["800"]) uniq[f] = (uniq[f] || 0) + 1;
  for (const [f, n] of Object.entries(uniq)) console.log(`  ${String(n).padStart(3)}x  ${f}`);
} else {
  console.log("  (khong co)");
}
console.log("");
console.log("=== weight 600 (dang trong URL nhung co the khong dung) ===");
if (byFile["600"]) {
  const uniq = {};
  for (const f of byFile["600"]) uniq[f] = (uniq[f] || 0) + 1;
  for (const [f, n] of Object.entries(uniq)) console.log(`  ${String(n).padStart(3)}x  ${f}`);
} else {
  console.log("  (khong co)");
}
