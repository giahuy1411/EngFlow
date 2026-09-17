import os, time, json
d = "C:/Users/ASUS/Documents/LAPTRINH/engflow/uploads"
rows = []
for f in sorted(os.listdir(d)):
    st = os.stat(os.path.join(d, f))
    rows.append({"f": f, "size": st.st_size, "created": time.strftime("%Y-%m-%d %H:%M", time.localtime(st.st_ctime))})
for r in rows:
    print(r["created"], "%6d" % r["size"], r["f"])
print("total", len(rows))
json.dump(rows, open("uploads-before.json", "w"), indent=1)
