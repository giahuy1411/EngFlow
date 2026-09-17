import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "src/main/java/com/datn/engflow/service/AiExerciseService.java"
s = open(p, encoding="utf-8").read()
old = """                if (opts.size() < 2) return "need >= 2 options";
            } catch (Exception e) { return \"invalid options JSON\"; }"""
new = """                if (opts.size() < 2) return "need >= 2 options";
                // audit-v8: duplicate options make the item ambiguous (two correct
                // letters) and are a measured failure mode of the local model
                // ("most expensive/more expensive/best/best" was accepted and saved).
                java.util.Set<String> seenOpts = new java.util.HashSet<>();
                for (Object o : opts) {
                    String key = String.valueOf(o).trim().toLowerCase();
                    if (!seenOpts.add(key)) return "MULTIPLE_CHOICE options must be distinct";
                }
            } catch (Exception e) { return \"invalid options JSON\"; }"""
assert s.count(old) == 1, "anchor " + str(s.count(old))
open(p, "w", encoding="utf-8", newline="").write(s.replace(old, new))
print("guard added")
