# EngFlow API sweep v7-R2 — corrected paths + real IDs
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$adminT = [IO.File]::ReadAllText("$PSScriptRoot\admin.token").Trim()
$userT  = [IO.File]::ReadAllText("$PSScriptRoot\user.token").Trim()
$results = [System.Collections.Generic.List[object]]::new()

function Probe($name, $method, $path, $token, $expect) {
  $args = @('-s','-X',$method,"$base$path",'-o',"$PSScriptRoot\body.tmp",'-w','%{http_code} %{time_total}')
  if ($token -eq 'admin') { $args += @('-H',"Authorization: Bearer $adminT") }
  elseif ($token -eq 'user') { $args += @('-H',"Authorization: Bearer $userT") }
  $resp = & curl.exe @args
  $parts = "$resp" -split ' '
  $code = [int]$parts[0]; $ms = [math]::Round([double]$parts[1]*1000)
  $exp = @($expect)
  $ok = $exp -contains $code
  $results.Add([pscustomobject]@{ name=$name; method=$method; path=$path; as=$token; code=$code; ms=$ms; expect=$exp -join '/'; pass=$ok })
}

# ---------- GETs with REAL ids ----------
Probe 'lesson-445'        'GET' '/api/lessons/445' 'admin' 200
Probe 'lesson-445-ex'     'GET' '/api/lessons/445/exercises' 'admin' 200
Probe 'lesson-445-content' 'GET' '/api/lessons/445/exercises/content' 'admin' 200
Probe 'lesson-445-ex-ans' 'GET' '/api/lessons/445/exercises?includeAnswers=true' 'admin' 200
Probe 'lesson-445-ex-ans-user' 'GET' '/api/lessons/445/exercises?includeAnswers=true' 'user' 403
Probe 'deck-10006'        'GET' '/api/decks/10006' 'admin' 200
Probe 'deck-10006-quiz'   'GET' '/api/games/quiz/10006' 'user' 200
Probe 'deck-10006-mem'    'GET' '/api/games/memory/10006' 'user' 200
Probe 'deck-10006-typing' 'GET' '/api/games/typing/10006' 'user' 200
Probe 'deck-10006-listen' 'GET' '/api/games/listening/10006' 'user' 200
Probe 'deck-10006-mixed'  'GET' '/api/games/mixed/10006' 'user' 200
Probe 'srs-due-10006'     'GET' '/api/srs/due/10006' 'user' 200
Probe 'srs-stats'         'GET' '/api/srs/stats' 'user' 200
Probe 'dash-stats'        'GET' '/api/dashboard/stats' 'user' 200
Probe 'progress'          'GET' '/api/users/progress' 'user' 200
Probe 'sub-grammar'       'GET' '/api/lesson-submissions/my/lesson/445/skill/GRAMMAR' 'user' 200
Probe 'speaking-history'  'GET' '/api/v1/speaking-submissions?page=0&size=5' 'user' 200
Probe 'speaking-hist-adm' 'GET' '/api/v1/admin/speaking-submissions?page=0&size=5' 'admin' 200
Probe 'video-hist'        'GET' '/api/v1/video-submissions?page=0&size=5' 'user' 200
Probe 'pay-status-bogus'  'GET' '/api/v1/payment/status?orderId=nope' 'user' @(200,404,400)
Probe 'admin-stats'       'GET' '/api/admin/stats' 'admin' 200
Probe 'admin-vocab'       'GET' '/api/admin/vocabulary?page=0&size=5' 'admin' 200
Probe 'admin-lesson-445'  'GET' '/api/admin/lessons/445' 'admin' 200
Probe 'admin-snap-445'    'GET' '/api/admin/lessons/445/snapshots' 'admin' 200
Probe 'ex-648903'         'GET' '/api/admin/exercises/648903' 'admin' 200
Probe 'flash-status'      'GET' '/api/flashcards/status/1' 'user' @(200,404)
Probe 'lesson-level-ok'   'GET' '/api/lessons?level=ELEMENTARY&page=0&size=3' 'user' 200
Probe 'lesson-sort-none'  'GET' '/api/lessons?page=0&size=3' 'user' 200
Probe 'lesson-bad-size'   'GET' '/api/lessons?size=99999' 'user' @(200,400)
Probe 'lesson-neg-page'   'GET' '/api/lessons?page=-5' 'user' @(200,400)
Probe 'lesson-q-xss'      'GET' '/api/lessons?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E' 'user' 200
Probe 'decks-noauth-detail' 'GET' '/api/decks/10006' 'none' 200
Probe 'decks-my-noauth'   'GET' '/api/decks/my' 'none' @(401)

$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 "$PSScriptRoot\v7-sweep-r2.json"
$fail = @($results | Where-Object { -not $_.pass })
Write-Host "TOTAL=$($results.Count) FAIL=$($fail.Count)"
$fail | ForEach-Object { Write-Host ("FAIL {0} {1} as={2} -> {3} (expect {4})" -f $_.method, $_.path, $_.as, $_.code, $_.expect) }
Write-Host '--- slowest 10 ---'
$results | Sort-Object ms -Descending | Select-Object -First 10 | ForEach-Object { Write-Host ("{0,-26} {1,7}ms  {2} {3}" -f $_.name, $_.ms, $_.method, $_.path) }
