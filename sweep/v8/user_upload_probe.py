import json, urllib.request, uuid

BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

tok = login("user@gmail.com", "123456")

def multipart(path, filename, content, ctype):
    b = uuid.uuid4().hex
    body = b"".join([
        ("--" + b + "\r\n").encode(),
        ('Content-Disposition: form-data; name="file"; filename="' + filename + '"\r\n').encode(),
        ("Content-Type: " + ctype + "\r\n\r\n").encode(),
        content, b"\r\n",
        ("--" + b + "--\r\n").encode(),
    ])
    req = urllib.request.Request(BASE + path, data=body, method="POST", headers={
        "Authorization": "Bearer " + tok,
        "Content-Type": "multipart/form-data; boundary=" + b,
    })
    try:
        r = urllib.request.urlopen(req)
        return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")

payload = b"<html><body><h1>USERUPLOAD-PROBE</h1></body></html>"
for path in ["/api/lesson-submissions/upload-audio", "/api/auth/avatar/upload"]:
    print(path)
    for fn in ["probe.html", "probe.txt"]:
        st, bd = multipart(path, fn, payload, "text/html")
        print("   ", fn, st, bd[:120])
