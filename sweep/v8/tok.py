import base64, json, time, urllib.request

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request("http://localhost:8080/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

for who in ["user@gmail.com", "admin@gmail.com"]:
    t = login(who, "123456")
    p = t.split(".")[1]
    p += "=" * (-len(p) % 4)
    d = json.loads(base64.urlsafe_b64decode(p))
    print("%-18s ttl=%ss  now-left=%ss" % (who, d["exp"] - d["iat"], d["exp"] - int(time.time())))
