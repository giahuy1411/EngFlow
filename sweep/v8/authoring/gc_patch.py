import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = "src/main/java/com/datn/engflow/controller/GameController.java"
s = open(p, encoding="utf-8").read()

old_block = s[s.index("        String sessionId = (String) payload.get(\"sessionId\");"):s.index("        return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, correctAnswers));\n    }") + len("        return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, correctAnswers));\n    }")]

new_block = """        Object sessionIdRaw = payload.get("sessionId");
        if (!(sessionIdRaw instanceof String) || ((String) sessionIdRaw).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session ID không được để trống"));
        }
        String sessionId = (String) sessionIdRaw;
        // New secure path: if client sends detailed answers, server validates
        Object answersObj = payload.get("answers");
        if (answersObj != null && !(answersObj instanceof java.util.List)) {
            return ResponseEntity.badRequest().body(Map.of("error", "answers phải là một danh sách"));
        }
        Object correctRawObj = payload.get("correctAnswers");
        if (correctRawObj != null && !(correctRawObj instanceof Number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "correctAnswers phải là một số"));
        }
        Number correctRaw = (Number) correctRawObj;
        if (answersObj instanceof java.util.List) {
            java.util.List<Map<String, Object>> answers = (java.util.List<Map<String, Object>>) answersObj;
            int clientCorrect = correctRaw != null ? correctRaw.intValue() : 0;
            return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, clientCorrect, answers));
        }
        if (correctRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "correctAnswers không được để trống"));
        }
        int correctAnswers = correctRaw.intValue();

        return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, correctAnswers));
    }"""

assert s.count(old_block) == 1, "anchor not unique"
s = s.replace(old_block, new_block)
open(p, "w", encoding="utf-8", newline="").write(s)
print("patched GameController submitGameResult")
print(new_block[:200])
