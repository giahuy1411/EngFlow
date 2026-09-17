import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = "src/main/java/com/datn/engflow/controller/GameController.java"
s = open(p, encoding="utf-8").read()

old = """        if (answersObj instanceof java.util.List) {
            java.util.List<Map<String, Object>> answers = (java.util.List<Map<String, Object>>) answersObj;"""
new = """        if (answersObj instanceof java.util.List) {
            java.util.List<?> rawAnswers = (java.util.List<?>) answersObj;
            for (Object item : rawAnswers) {
                if (!(item instanceof java.util.Map)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "mỗi phần tử answers phải là một đối tượng"));
                }
            }
            java.util.List<Map<String, Object>> answers = (java.util.List<Map<String, Object>>) answersObj;"""

assert s.count(old) == 1
s = s.replace(old, new)
open(p, "w", encoding="utf-8", newline="").write(s)
print("patched answers element guard")
