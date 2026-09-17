import io, sys
p = "sweep/v8/ui/v4edit.js"
t = io.open(p, encoding="utf-8").read()
old = """  await page.getByRole("button", { name: /L\u01b0u b\u00e0i h\u1ecdc/ }).click();
  await page.waitForTimeout(2500);"""
new = """  await page.getByRole("button", { name: /L\u01b0u b\u00e0i h\u1ecdc/ }).click();
  // toast auto-dismiss nhanh -> poll thay vi cho 1 nhip co dinh
  let toastSeen = false;
  for (let i = 0; i < 20; i++) {
    await page.waitForTimeout(200);
    if ((await page.locator("body").innerText()).includes("\u0110\u00e3 l\u01b0u b\u00e0i h\u1ecdc video")) { toastSeen = true; break; }
  }"""
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
old2 = """  const toast = (await page.locator("body").innerText()).includes("\u0110\u00e3 l\u01b0u b\u00e0i h\u1ecdc video");
  note("toast th\u00e0nh c\u00f4ng hi\u1ec3n th\u1ecb", toast, true);"""
new2 = """  note("toast th\u00e0nh c\u00f4ng hi\u1ec3n th\u1ecb", toastSeen, true);"""
if old2 not in t:
    print("NOT FOUND 2"); sys.exit(1)
t = t.replace(old2, new2, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t)
print("patched")
