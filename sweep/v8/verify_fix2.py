import json, urllib.request, urllib.error, uuid, os

BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

def upload(tok, endpoint, fname, content):
    b = uuid.uuid4().hex
    head = ("--" + b + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
            + fname + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
    body = (head.encode() + content + ("\r\n--" + b + "--\r\n").encode())
    req = urllib.request.Request(BASE + endpoint, data=body, method="POST", headers={
        "Authorization": "Bearer " + tok,
        "Content-Type": "multipart/form-data; boundary=" + b})
    try:
        r = urllib.request.urlopen(req)
        return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:100]

tok = login("user@gmail.com", "123456")
print("-- recorder sends NO filename (must land inert, not extensionless) --")
for fn in ["", "noext", "weird.HTML", "double.html.txt", "trail."]:
    code, body = upload(tok, "/api/lesson-submissions/upload-audio", fn, b"abc")
    print("  filename=%-16r -> %s %s" % (fn, code, body[:80]))

print("-- case-insensitivity / tricks on the audio path --")
for fn in ["a.Html", "a.cDn", "a.jpeg", "a.tar.gz", "a%00.html"]:
    code, body = upload(tok, "/api/lesson-submissions/upload-audio", fn, b"abc")
    print("  filename=%-14r -> %s %s" % (fn, code, body[:70]))
