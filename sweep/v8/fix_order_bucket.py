import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "src/main/java/com/datn/engflow/security/RateLimitFilter.java"
s = open(p, encoding="utf-8").read()
old = '        } else if (requestURI.startsWith("/api/payments/create-order") || requestURI.startsWith("/api/premium")) {'
new = ('        } else if (requestURI.startsWith("/api/v1/payment/create-order")) {\n'
       '            // audit-v8: the previous prefixes (/api/payments/create-order, /api/premium)\n'
            # comment continues below
          + '            // do not exist in the app, so this bucket never matched and payment order\n'
          + '            // creation fell back to the 100/min global bucket (measured: 13 orders in a\n'
          + '            // row were all accepted, and only :global keys were written).')
assert s.count(old) == 1, "anchor count " + str(s.count(old))
s = s.replace(old, new)
open(p, "w", encoding="utf-8", newline="").write(s)
print("patched order bucket matcher")
