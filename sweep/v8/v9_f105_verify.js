const lib = require("./lib.js");
const { probe } = lib;
(async () => {
  await lib.initTokens();
  const stamp = Date.now();
  const r = await probe("create draft", "POST", "/api/admin/lessons", "admin", 200,
    { title: "ZZ v9 draft " + stamp, content: "draft lesson for F105 live verification.", level: "ELEMENTARY", category: "GRAMMAR", durationMinutes: 5, isPublished: false, orderIndex: 9999 });
  const lessonId = JSON.parse(r.txt).id;
  const re = await probe("add exercise", "POST", "/api/admin/exercises", "admin", 200,
    { lessonId: lessonId, question: "F105 probe 2+2?", options: JSON.stringify(["5","4"]), correctAnswer: "4", exerciseType: "MULTIPLE_CHOICE", difficulty: "EASY", orderIndex: 1 });
  const exId = JSON.parse(re.txt).id;
  const g = await probe("student grade on DRAFT (expect 404)", "POST", "/api/lessons/" + lessonId + "/exercises/grade", "user", 404,
    { answers: [{ exerciseId: exId, userAnswer: "4" }] });
  console.log("  grade body:", g.txt.slice(0, 80));
  const s = await probe("student submit on DRAFT (expect 404)", "POST", "/api/lessons/" + lessonId + "/exercises/submit", "user", 404,
    { answers: [{ exerciseId: exId, userAnswer: "4" }] });
  console.log("  submit body:", s.txt.slice(0, 80));
  const ap = await probe("admin preview grade (expect 200)", "POST", "/api/lessons/" + lessonId + "/exercises/grade", "admin", 200,
    { answers: [{ exerciseId: exId, userAnswer: "4" }] });
  console.log("  admin grade body:", ap.txt.slice(0, 120));
  await probe("cleanup delete", "DELETE", "/api/admin/lessons/" + lessonId, "admin", [200,204]);
  lib.report("F105-VERIFY");
})().catch(e => { console.error(e); process.exit(1); });
