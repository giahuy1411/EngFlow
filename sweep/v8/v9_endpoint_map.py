#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Task 5 (part 2) — emit endpoint-map.md: every mapping annotated with the
SecurityConfig rule that actually claims it.

Spring Security evaluates `authorizeHttpRequests` rules in DECLARATION ORDER and
the FIRST match wins (this is exactly the bug class audit-v7 F54 hit). So the
rule table below is applied in source order, not by specificity.
"""
import json
import os
import re

ROOT = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"
INV = os.path.join(ROOT, ".specify", "specs", "audit-v9-full", "evidence", "endpoint-inventory.json")
OUT = os.path.join(ROOT, ".specify", "specs", "audit-v9-full", "endpoint-map.md")

# SecurityConfig rules in DECLARATION ORDER (verbatim from SecurityConfig.java:76-118)
RULES = [
    ("ANY",  ["/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password"], "permitAll"),
    ("GET",  ["/api/vocabulary/search", "/api/vocabulary/dictionary/*"], "permitAll"),
    ("GET",  ["/api/leaderboard"], "permitAll"),
    ("GET",  ["/api/lessons/*/exercises/attempts/**"], "authenticated"),
    ("GET",  ["/api/lessons/**"], "permitAll"),
    ("GET",  ["/api/v1/speaking-prompts", "/api/v1/speaking-prompts/**"], "permitAll"),
    ("GET",  ["/api/v1/video-prompts", "/api/v1/video-prompts/**"], "permitAll"),
    ("GET",  ["/api/v1/video-lessons", "/api/v1/video-lessons/**"], "permitAll"),
    ("GET",  ["/api/decks/**"], "permitAll"),
    ("ANY",  ["/api/webhook/sepay"], "permitAll"),
    ("POST", ["/api/ai/generate-vocab", "/api/ai/enrich-word"], "authenticated"),
    ("POST", ["/api/ai/save-vocab"], "authenticated"),
    ("POST", ["/api/auth/**"], "authenticated"),
    ("POST", ["/api/lessons/*/exercises/submit"], "authenticated"),
    ("POST", ["/api/lessons/*/exercises/grade"], "authenticated"),
    ("GET",  ["/api/lessons/*/exercises/attempts/**"], "authenticated"),
    ("POST", ["/api/lessons", "/api/lessons/**"], "ROLE_ADMIN"),
    ("PUT",  ["/api/lessons/**"], "ROLE_ADMIN"),
    ("DELETE", ["/api/lessons/**"], "ROLE_ADMIN"),
    ("POST", ["/api/vocabulary", "/api/vocabulary/**"], "authenticated"),
    ("PUT",  ["/api/vocabulary/**"], "ROLE_ADMIN"),
    ("DELETE", ["/api/vocabulary/**"], "ROLE_ADMIN"),
    ("ANY",  ["/api/exercises/submit/**"], "authenticated"),
    ("ANY",  ["/api/exercises/submissions/**"], "authenticated"),
    ("ANY",  ["/api/exercises/**"], "ROLE_ADMIN"),
    ("ANY",  ["/api/v1/admin/**"], "ROLE_ADMIN"),
    ("ANY",  ["/api/admin/**"], "ROLE_ADMIN"),
    ("GET",  ["/audio/**"], "permitAll"),
    ("GET",  ["/api/v1/media/**"], "permitAll"),
    ("GET",  ["/api/resources/**"], "permitAll"),
    ("ANY",  ["**"], "authenticated"),
]

# RateLimitFilter buckets, in the SAME if/else order as RateLimitFilter.java:47-75
BUCKETS = [
    ("auth",   ["/api/auth/login", "/api/auth/register"], 20, "prefix"),
    ("mail",   ["/api/auth/forgot-password", "/api/auth/reset-password"], 5, "prefix"),
    ("ai",     ["/api/ai/"], 10, "prefix"),
    ("upload", ["/api/admin/upload", "/api/admin/audio-upload"], 15, "prefix-or-contains-submissions-or-video-attempts"),
    ("order",  ["/api/v1/payment/create-order"], 10, "prefix"),
    ("global", ["**"], 100, "fallback"),
]


def ant_match(pattern, method, path):
    if pattern == "**":
        return True
    p_segs = [s for s in pattern.split("/") if s != ""]
    c_segs = [s for s in path.split("/") if s != ""]
    if "**" in p_segs:
        i = p_segs.index("**")
        head, tail = p_segs[:i], p_segs[i + 1:]
        if len(c_segs) < len(head) + len(tail):
            return False
        return all(_seg(a, b) for a, b in zip(head, c_segs[:len(head)])) and \
               all(_seg(a, b) for a, b in zip(reversed(tail), reversed(c_segs)))
    if len(p_segs) != len(c_segs):
        return False
    return all(_seg(a, b) for a, b in zip(p_segs, c_segs))


def _seg(pat, con):
    if pat == "*":
        return True
    if pat.startswith('{'):
        return True
    return pat == con


def sec_for(method, path):
    for m, pats, verdict in RULES:
        if m != "ANY" and m != method:
            continue
        for p in pats:
            if ant_match(p, method, path):
                return verdict, (m + " " + p)
    return "authenticated", "fallback"


def bucket_for(method, path):
    # mirror RateLimitFilter exactly
    if path.startswith("/api/auth/login") or path.startswith("/api/auth/register"):
        return "auth", 20
    if path.startswith("/api/auth/forgot-password") or path.startswith("/api/auth/reset-password"):
        return "mail", 5
    if path.startswith("/api/ai/"):
        return "ai", 10
    if method == "POST" and (path.startswith("/api/admin/upload")
                             or path.startswith("/api/admin/audio-upload")
                             or "/submissions" in path
                             or path.startswith("/api/v1/video-attempts")):
        return "upload", 15
    if path.startswith("/api/v1/payment/create-order"):
        return "order", 10
    return "global", 100


def main():
    with open(INV, encoding="utf-8") as fh:
        inv = json.load(fh)

    cov_path = os.path.join(ROOT, ".specify", "specs", "audit-v9-full",
                            "evidence", "coverage-round1.json")
    uncovered = set()
    if os.path.exists(cov_path):
        with open(cov_path, encoding="utf-8") as fh:
            c = json.load(fh)
        for u in c.get("uncovered", []):
            uncovered.add((u["method"], u["path"]))

    lines = []
    lines.append("# Endpoint map — audit-v9-full (Round 1)\n")
    lines.append("> Sinh tự động từ SOURCE: `sweep/v8/endpoint_inventory.py` → "
                 "`evidence/endpoint-inventory.json`.\n"
                 "> Auth/role suy ra bằng cách áp **đúng thứ tự khai báo** của "
                 "`SecurityConfig.java:76-118` (Spring chọn rule KHỚP ĐẦU TIÊN — "
                 "đây chính là bug class của audit-v7 F54).\n"
                 "> Bucket suy ra bằng cách áp **đúng thứ tự if/else** của "
                 "`RateLimitFilter.java:47-75`.\n")
    lines.append("**Tổng số mapping**: %d · **controller**: %d · "
                 "**đã probe**: %d/%d\n" % (inv["mappingCount"], inv["controllerCount"],
                                             inv["mappingCount"] - len(uncovered), inv["mappingCount"]))
    lines.append("**Phân bố HTTP method**: " +
                 ", ".join("`%s`=%d" % (k, v) for k, v in inv["byHttpMethod"].items()) + "\n")

    lines.append("\n## Bảng mapping\n")
    lines.append("| # | Method | Path | Controller | Auth rule | Rate-limit bucket | Probed |")
    lines.append("|---|---|---|---|---|---|---|")
    for i, e in enumerate(inv["endpoints"], 1):
        verdict, rule = sec_for(e["httpMethod"], e["path"])
        b, lim = bucket_for(e["httpMethod"], e["path"])
        probed = "**NO**" if (e["httpMethod"], e["path"]) in uncovered else "yes"
        lines.append("| %d | `%s` | `%s` | %s | %s | `:%s` (%d/min) | %s |"
                     % (i, e["httpMethod"], e["path"], e["controller"], verdict, b, lim, probed))

    lines.append("\n## Negative cases bắt buộc (PLAN.md Task 5)\n")
    lines.append("| Case | Cách đo | Trạng thái |")
    lines.append("|---|---|---|")
    lines.append("| missing auth | probe với `as=none` trên toàn bộ endpoint không permitAll | p1/p4a/p6 role matrix |")
    lines.append("| wrong role | probe `as=user` trên mọi endpoint `/api/admin/**`, `/api/v1/admin/**` | p1/p4a/p6 |")
    lines.append("| malformed body | JSON sai kiểu / thiếu field bắt buộc | p1/p4b/p6 (exercise, prompt, lesson, game submit) |")
    lines.append("| missing resource | id không tồn tại (999999999) | p6 + p4c-fleet |")
    lines.append("| duplicate resource | register trùng email/username → 409 | p5 |")
    lines.append("| invalid enum/type | `level=FOO`, `skill=NOT_A_SKILL`, `level=B1` trên video | p1/p5/p6 |")
    lines.append("| upload extension/content mismatch | `.html/.svg/.js` → 400; octet-stream + attachment khi serve | `AuditV8UploadXssTest` + `xss_poc.js` |")

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print("wrote %s (%d endpoints)" % (OUT, inv["mappingCount"]))

    # quick sanity: show distribution of verdicts
    from collections import Counter
    c = Counter(sec_for(e["httpMethod"], e["path"])[0] for e in inv["endpoints"])
    print("auth verdicts:", dict(c))


if __name__ == "__main__":
    main()
