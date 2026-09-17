import io
p = "sweep/v8/ui/v4edit.js"
t = io.open(p, encoding="utf-8").read()
old = """  await H.seedToken(page, admin);
  await page.goto(APP + "/admin/videos", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2500);"""
new = """  // dang nhap bang FORM THAT (router guard doc auth store, chi set localStorage.token la khong du)
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await page.fill('input[type="email"], input[name="email"]', "admin@gmail.com");
  await page.fill('input[type="password"]', "123456");
  await page.click('button[type="submit"]');
  await page.waitForTimeout(4000);
  await page.goto(APP + "/admin/videos", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3000);"""
assert old in t
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t)
print("patched")
