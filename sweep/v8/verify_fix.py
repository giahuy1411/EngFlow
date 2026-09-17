import json, urllib.request, urllib.error, uuid, os

BASE = "http://localhost:8080"
APP = "http://localhost:5173"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

def upload(tok, endpoint, fname, content, ctype):
    b = uuid.uuid4().hex
    head = ("--" + b + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
            + fname + "\"\r\nContent-Type: " + ctype + "\r\n\r\n")
    body = (head.encode() + content + ("\r\n--" + b + "--\r\n").encode())
    req = urllib.request.Request(BASE + endpoint, data=body, method="POST", headers={
        "Authorization": "Bearer " + tok,
        "Content-Type": "multipart/form-data; boundary=" + b})
    try:
        r = urllib.request.urlopen(req)
        return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:120]

def get(path, tok=None):
    req = urllib.request.Request(APP + path)
    if tok:
        req.add_header("Authorization", "Bearer " + tok)
    try:
        r = urllib.request.urlopen(req, timeout=20)
        return r.status, dict(r.headers), r.read()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read()

tok = login("user@gmail.com", "123456")
admin = login("admin@gmail.com", "123456")
print("== LAYER 1: write-time allowlist ==")
for ep in ["/api/lesson-submissions/upload-audio", "/api/admin/upload"]:
    who = admin if "admin" in ep else tok
    for fn in ["evil.html", "evil.svg", "evil.js", "evil.xhtml", "noext"]:
        code, body = upload(who, ep, fn, b"<script>alert(1)</script>", "text/html")
        verdict = "BLOCKED" if code == 400 else "!!! ALLOWED " + str(code)
        print("  %-34s %-12s %s" % (ep, fn, verdict))
    for fn in ["rec.webm", "pic.png", "notes.txt"]:
        code, body = upload(who, ep, fn, b"\x1a\x45\xdf\xa3fake", "application/octet-stream")
        print("  %-34s %-12s %s %s" % (ep, fn, "OK" if code == 200 else "FAIL " + str(code), body[:70]))

print("== LAYER 2: pre-existing on-disk files neutralised ==")
d = "C:/Users/ASUS/Documents/LAPTRINH/engflow/uploads"
for f in sorted(os.listdir(d)):
    if f.endswith((".html", ".js", ".svg")):
        code, hdr, body = get("/api/resources/" + f)
        print("  %-46s -> %s CT=%s DISP=%s" % (f, code, hdr.get("Content-Type"),
                                               hdr.get("Content-Disposition")))
