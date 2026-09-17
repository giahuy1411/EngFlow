import sys
p = "frontend/src/views/admin/AdminVideoLessons.vue"
with open(p, "r", encoding="utf-8", newline="") as f:
    t = f.read()
old = """              <router-link :to="`/videos/${l.id}`" class="text-xs font-black uppercase tracking-wider underline mr-3">Xem</router-link>\n              <AppButton @click="deleteLesson(l.id)" variant="danger" size="sm">X\u00f3a</AppButton>"""
new = """              <router-link :to="`/videos/${l.id}`" class="text-xs font-black uppercase tracking-wider underline mr-3">Xem</router-link>\n              <AppButton @click="openForm(l)" variant="secondary" size="sm" class="mr-2">S\u1eeda</AppButton>\n              <AppButton @click="deleteLesson(l.id)" variant="danger" size="sm">X\u00f3a</AppButton>"""
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
with open(p, "w", encoding="utf-8", newline="") as f:
    f.write(t)
print("patched", p)
