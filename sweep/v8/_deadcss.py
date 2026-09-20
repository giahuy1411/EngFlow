import re, sys
LIVE = {"audio","card","markdown","radio"}
path="frontend/src/assets/design-system.css"
src=open(path,encoding="utf-8").read()
cls=re.compile(r"\.geo-([a-zA-Z0-9_-]+)")

def split_rules(text):
    # returns list of (kind, content) preserving whitespace; kind in {rule, atrule_open, atrule_close, decl, ws, comment}
    out=[]; i=0; n=len(text)
    while i<n:
        if text[i]=="\n" or text[i] in " \t\r":
            j=i
            while j<n and text[j] in " \t\r\n": j+=1
            out.append(("ws",text[i:j])); i=j; continue
        if text[i]=="/" and text[i+1:i+2]=="*":
            j=text.find("*/",i+2)
            j=n if j<0 else j+2
            out.append(("comment",text[i:j])); i=j; continue
        # read until { or ; or }
        j=i; depth=0
        while j<n:
            c=text[j]
            if c=="{": break
            if c=="}": break
            if c==";": break
            j+=1
        if j>=n:
            out.append(("raw",text[i:])); i=n; break
        if text[j]==";":
            out.append(("decl",text[i:j+1])); i=j+1; continue
        if text[j]=="}":
            out.append(("close",text[i:j+1])); i=j+1; continue
        # it's a block start
        sel=text[i:j]
        # find matching close brace, respecting nesting
        k=j+1; depth=1
        while k<n and depth>0:
            if text[k]=="{": depth+=1
            elif text[k]=="}": depth-=1
            k+=1
        body=text[j:k]  # includes { ... }
        out.append(("block",(sel,body))); i=k
    return out

def geo_classes_in(sel):
    return set(cls.findall(sel))

# For top-level, we process blocks recursively for @media
def process_blocks(segs, removed, kept, top=True):
    res=[]
    for kind,val in segs:
        if kind=="block":
            sel,body=val
            g=geo_classes_in(sel)
            if sel.strip().startswith("@") and "{" in body:
                # at-rule with nested blocks (e.g. @media): recurse into its body
                inner=body[1:-1]
                inner_segs=split_rules(inner)
                new_inner=process_blocks(inner_segs, removed, kept, top=False)
                ni="".join(x[1] if x[0]!="block" else x[1][0]+x[1][1] for x in new_inner)
                res.append(("block",(sel,"{"+ni+"}")))
            else:
                if g and not (g & LIVE):
                    removed.append(sel.strip()[:60]); continue
                else:
                    if g & LIVE: kept.append(sel.strip()[:40])
                    res.append(("block",(sel,body)))
        else:
            res.append((kind,val))
    return res

segs=split_rules(src)
removed=[]; kept=[]
new=process_blocks(segs, removed, kept)
out="".join(x[1] if x[0]!="block" else x[1][0]+x[1][1] for x in new)
# cleanup excessive blank lines
out=re.sub(r"\n{3,}","\n\n",out)
open("frontend/src/assets/design-system.css.new","w",encoding="utf-8",newline="\n").write(out)
print("removed blocks:",len(removed))
print("kept live-geo selectors:",sorted(set(kept)))
print("size before/after:",len(src),len(out))
for r in removed[:60]: print(" -",r)
