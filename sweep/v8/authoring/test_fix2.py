import io, sys, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = glob.glob("src/test/java/**/GameControllerSubmitTypeTest.java", recursive=True)[0]
s = open(p, encoding="utf-8").read()

# assert the guard message, not just the status, so a 400 from the session lookup
# cannot masquerade as a pass
s = s.replace(
    'import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;',
    'import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;\n'
    'import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;')

cases = [
    ('\\"correctAnswers\\":{\\"a\\":1}', 'correctAnswers phải là một số'),
    ('\\"correctAnswers\\":\\"999\\"', 'correctAnswers phải là một số'),
    ('\\"answers\\":{\\"a\\":1}', 'answers phải là một danh sách'),
    ('\\"answers\\":[\\"x\\",42]', 'mỗi phần tử answers phải là một đối tượng'),
]
lines = s.split(chr(10))
out = []
for ln in lines:
    out.append(ln)
s = chr(10).join(out)

def add_assert(src, marker, msg):
    i = src.index(marker)
    j = src.index('.andExpect(status().isBadRequest());', i)
    end = j + len('.andExpect(status().isBadRequest());')
    repl = '.andExpect(status().isBadRequest())\n                .andExpect(jsonPath("$.error").value("' + msg + '"));'
    return src[:src.index(marker)] + src[i:j] + repl + src[end:]

pairs = [
    ('correctAnswersAsObjectIs400Not500', 'correctAnswers phải là một số'),
    ('correctAnswersAsStringIs400Not500', 'correctAnswers phải là một số'),
    ('answersNotAListIs400Not500', 'answers phải là một danh sách'),
    ('answersListOfScalarsIs400Not500', 'mỗi phần tử answers phải là một đối tượng'),
]
for fname, msg in pairs:
    i = s.index('void ' + fname)
    j = s.index('.andExpect(status().isBadRequest());', i)
    end = j + len('.andExpect(status().isBadRequest());')
    s = s[:j] + '.andExpect(status().isBadRequest())\n                .andExpect(jsonPath("' + '$' + '.error").value("' + msg + '"));' + s[end:]
    print("annotated", fname)

open(p, "w", encoding="utf-8", newline="").write(s)
print("written")
