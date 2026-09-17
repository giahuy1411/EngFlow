import io, sys
p = "sweep/v8/ui/v4edit.js"
t = io.open(p, encoding="utf-8").read()
old = 'if ((await page.locator("body").innerText()).includes("\u0110\u00e3 l\u01b0u b\u00e0i h\u1ecdc video")) { toastSeen = true; break; }'
new = '// innerText tra ve chu HOA (CSS text-transform) -> so sanh khong phan biet hoa/thuong\n    if (/\u0111\u00e3 l\u01b0u b\u00e0i h\u1ecdc video/i.test(await page.locator("body").innerText())) { toastSeen = true; break; }'
if old not in t: print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t)
print("patched")
