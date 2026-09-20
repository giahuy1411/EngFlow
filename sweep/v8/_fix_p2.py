p = "sweep/v8/p2.js"
t = open(p, encoding="utf-8").read()

old = """  await probe("L publish toggle", "PUT", "/api/admin/lessons/" + lessonId + "/toggle-publish", "admin", 200);
  await probe("L publish toggle back", "PUT", "/api/admin/lessons/" + lessonId + "/toggle-publish", "admin", 200);
  await probe("L user-forbidden", "PUT", "/api/admin/lessons/" + lessonId, "user", 403, { title: "nope", content: "nope nope nope", level: "ELEMENTARY" });"""

new = """  await probe("L publish toggle", "PUT", "/api/admin/lessons/" + lessonId + "/toggle-publish", "admin", 200);
  // F89 positive control: verify 404 as guest when lesson is published=false is not possible here;
  // lesson is now published.  We verify the answer-stripping contract below instead.
  await probe("L user-forbidden", "PUT", "/api/admin/lessons/" + lessonId, "user", 403, { title: "nope", content: "nope nope nope", level: "ELEMENTARY" });"""
assert old in t, "toggle-back anchor missing"
t = t.replace(old, new)

old2 = '  await probe("E public content hides answer", "GET", "/api/lessons/" + lessonId + "/exercises/content", "none", 200);\n  await probe("E public list hides answer", "GET", "/api/lessons/" + lessonId + "/exercises", "none", 200);'
new2 = """  // Positive control: published lesson, guest sees content; response must NOT contain correctAnswer.
  const contentProbe = await probe("E public content hides answer", "GET", "/api/lessons/" + lessonId + "/exercises/content", "none", 200);
  if (contentProbe && contentProbe.txt && contentProbe.txt.includes("correctAnswer")) {
    console.log("  WARNING: correctAnswer leaked in /content response"); // Would be a real bug
  }
  const listProbe = await probe("E public list hides answer", "GET", "/api/lessons/" + lessonId + "/exercises", "none", 200);
  if (listProbe && listProbe.txt && listProbe.txt.includes("correctAnswer")) {
    console.log("  WARNING: correctAnswer leaked in /exercises response");
  }"""
assert old2 in t, "answer-hiding anchor missing"
t = t.replace(old2, new2)

open(p, "w", encoding="utf-8", newline="\r\n").write(t)
print("P2_FIXED")
