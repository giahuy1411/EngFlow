import subprocess

files = subprocess.run(["git", "diff", "--name-only"], capture_output=True, text=True).stdout.split()
for f in files:
    if not f.endswith((".java", ".js", ".vue", ".css", ".md")):
        continue
    head = subprocess.run(["git", "show", "HEAD:" + f], capture_output=True).stdout
    work = open(f, "rb").read()
    if not head:
        continue
    head_crlf = b"\r\n" in head
    work_crlf = b"\r\n" in work
    if head_crlf == work_crlf:
        continue
    if head_crlf:
        fixed = work.replace(b"\r\n", b"\n").replace(b"\n", b"\r\n")
    else:
        fixed = work.replace(b"\r\n", b"\n")
    open(f, "wb").write(fixed)
    print("normalized", f, "->", "CRLF" if head_crlf else "LF")
