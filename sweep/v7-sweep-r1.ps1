# EngFlow API sweep — audit v7 (Round 1, GET + authz matrix)
# Reads tokens from sweep\*.token, writes sweep\v7-sweep-r1.json
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$adminT = [IO.File]::ReadAllText("$PSScriptRoot\admin.token").Trim()
$userT  = [IO.File]::ReadAllText("$PSScriptRoot\user.token").Trim()
$results = [System.Collections.Generic.List[object]]::new()

function Probe($name, $method, $path, $token, $expect) {
  $h = @{}
  if ($token -eq 'admin') { $h['Authorization'] = "Bearer $adminT" }
  elseif ($token -eq 'user') { $h['Authorization'] = "Bearer $userT" }
  try {
    $resp = curl.exe -s -X $method "$base$path" @($h.Keys | ForEach-Object { -join @('-H', "`"$_`: $($h[$_])`"") }) -o sweep-body.tmp -w '%{http_code} %{time_total}'
    $parts = $resp -split ' '
    $code = [int]$parts[0]; $time = [double]$parts[1]
  } catch { $code = -1; $time = -1 }
  $ok = if ($expect -is [array]) { $expect -contains $code } else { $code -eq $expect }
  $results.Add([pscustomobject]@{ name=$name; method=$method; path=$path; as=$token; code=$code; ms=[math]::Round($time*1000); expect=$expect -join '/'; pass=$ok })
}

# --- GET sweep as admin (content endpoints) ---
$getAdmin = @(
  @{n='lessons-list';p='/api/lessons?page=0&size=5'},
  @{n='lessons-q';p='/api/lessons?q=travel'},
  @{n='lessons-level';p='/api/lessons?level=B1'},
  @{n='lesson-detail';p='/api/lessons/1'},
  @{n='lesson-exercises';p='/api/lessons/1/exercises'},
  @{n='lesson-content';p='/api/lessons/1/exercises/content'},
  @{n='submissions-mine';p='/api/lesson-submissions/mine'},
  @{n='decks-public';p='/api/decks?page=0&size=5'},
  @{n='decks-my';p='/api/decks/my'},
  @{n='deck-detail';p='/api/decks/1'},
  @{n='vocabulary-list';p='/api/vocabulary?page=0&size=5'},
  @{n='vocabulary-search';p='/api/vocabulary/search?q=hap'},
  @{n='dictionary';p='/api/vocabulary/dictionary/happy'},
  @{n='streak-current';p='/api/streak/current'},
  @{n='streak-history';p='/api/streak/history'},
  @{n='dashboard';p='/api/dashboard'},
  @{n='leaderboard';p='/api/leaderboard'},
  @{n='progress';p='/api/users/me/progress'},
  @{n='srs-queue';p='/api/srs/queue'},
  @{n='flashcards';p='/api/flashcards'},
  @{n='games-stats';p='/api/games/stats'},
  @{n='speaking-prompts';p='/api/v1/speaking-prompts'},
  @{n='speaking-prompt-1';p='/api/v1/speaking-prompts/1'},
  @{n='speaking-history';p='/api/v1/speaking-submissions/history'},
  @{n='video-lessons';p='/api/v1/video-lessons'},
  @{n='video-lesson-1';p='/api/v1/video-lessons/1'},
  @{n='video-attempts';p='/api/v1/video-attempts'},
  @{n='speaking-prompts-admin';p='/api/v1/admin/speaking-prompts'},
  @{n='payments-mine';p='/api/v1/payments/me'},
  @{n='shop-items';p='/api/shop/items'},
  @{n='admin-dashboard';p='/api/admin/dashboard'},
  @{n='admin-users';p='/api/admin/users?page=0&size=5'},
  @{n='admin-lessons';p='/api/admin/lessons?page=0&size=5'},
  @{n='admin-exercises';p='/api/admin/exercises?page=0&size=5'},
  @{n='admin-exercise-1';p='/api/admin/exercises/1'},
  @{n='admin-videos';p='/api/v1/admin/video-lessons'},
  @{n='admin-video-attempts';p='/api/v1/admin/video-attempts'},
  @{n='admin-submissions';p='/api/v1/admin/speaking-submissions'},
  @{n='ai-progress-bogus';p='/api/admin/exercises/ai/progress/nope'},
  @{n='lesson-snapshots';p='/api/lessons/1/snapshots'},
  @{n='auth-me';p='/api/auth/me'}
)
foreach ($e in $getAdmin) { Probe $e.n 'GET' $e.p 'admin' 200 }

# --- authz matrix spot-checks (correct codes) ---
Probe 'decks-noauth-list'   'GET' '/api/decks'            'none' @(200)
Probe 'lessons-noauth'      'GET' '/api/lessons'          'none' @(200)
Probe 'admin-as-user'       'GET' '/api/admin/users'      'user' @(401,403)
Probe 'streak-as-user'      'GET' '/api/streak/current'   'user' @(200)
Probe 'dashboard-as-user'   'GET' '/api/dashboard'        'user' @(200)
Probe 'me-prog-as-user'     'GET' '/api/users/me/progress' 'user' @(200)
Probe 'srs-as-user'         'GET' '/api/srs/queue'        'user' @(200)
Probe 'history-as-user'     'GET' '/api/v1/speaking-submissions/history' 'user' @(200)
Probe 'decks-my-noauth'     'GET' '/api/decks/my'         'none' @(401,403,200)
Probe 'lesson-999999'       'GET' '/api/lessons/999999'   'admin' @(404)
Probe 'deck-999999'         'GET' '/api/decks/999999'     'admin' @(404)
Probe 'admin-create-lesson-user' 'POST' '/api/lessons' 'user' @(401,403)

$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 sweep\v7-sweep-r1.json
$fail = $results | Where-Object { -not $_.pass }
Write-Host "TOTAL=$($results.Count) FAIL=$($fail.Count)"
$fail | ForEach-Object { Write-Host ("FAIL {0} {1} as={2} -> {3} (expect {4})" -f $_.method, $_.path, $_.as, $_.code, $_.expect) }
Write-Host '--- slowest 8 ---'
$results | Sort-Object ms -Descending | Select-Object -First 8 | ForEach-Object { Write-Host ("{0,-28} {1,7}ms" -f $_.name, $_.ms) }
