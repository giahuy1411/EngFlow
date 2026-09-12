# EngFlow CRUD sweep v7-R3 (create->read->update->delete via real HTTP)
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
function RefreshTokens() {
  $utf8 = New-Object System.Text.UTF8Encoding($false)
  [IO.File]::WriteAllText("$PSScriptRoot\..\login-user.json", '{"email":"user@gmail.com","password":"123456"}', $utf8)
  [IO.File]::WriteAllText("$PSScriptRoot\..\login-admin.json", '{"email":"admin@gmail.com","password":"123456"}', $utf8)
  $u = (curl.exe -s -X POST "$base/api/auth/login" -H 'Content-Type: application/json' --data-binary "@$PSScriptRoot\..\login-user.json") | ConvertFrom-Json
  $a = (curl.exe -s -X POST "$base/api/auth/login" -H 'Content-Type: application/json' --data-binary "@$PSScriptRoot\..\login-admin.json") | ConvertFrom-Json
  [IO.File]::WriteAllText("$PSScriptRoot\user.token", $u.token)
  [IO.File]::WriteAllText("$PSScriptRoot\admin.token", $a.token)
}
RefreshTokens
$adminT = [IO.File]::ReadAllText("$PSScriptRoot\admin.token").Trim()
$userT  = [IO.File]::ReadAllText("$PSScriptRoot\user.token").Trim()
$utf8 = New-Object System.Text.UTF8Encoding($false)
$log = [System.Collections.Generic.List[object]]::new()

function Api($name, $method, $path, $token, $bodyFile) {
  $a = @('-s','-X',$method,"$base$path",'-w','%{http_code}')
  if ($token -eq 'admin') { $a += @('-H',"Authorization: Bearer $adminT") }
  elseif ($token -eq 'user') { $a += @('-H',"Authorization: Bearer $userT") }
  if ($bodyFile) { $a += @('-H','Content-Type: application/json; charset=utf-8','--data-binary',"@$bodyFile") }
  $tmp = "$PSScriptRoot\resp-r3.tmp"
  $a += @('-o', $tmp)
  $code = [int]((& curl.exe @a) -join '')
  $body = if (Test-Path $tmp) { [IO.File]::ReadAllText($tmp) } else { '' }
  $one = ($body -replace '\s+',' ')
  $log.Add([pscustomobject]@{step=$name; code=$code; body=$one.Substring(0,[Math]::Min(220,$one.Length))})
  return @{code=$code; body=$body}
}
function J($obj) { $f = "$PSScriptRoot\body-r3.json"; [IO.File]::WriteAllText($f, ($obj | ConvertTo-Json -Depth 6 -Compress), $utf8); return $f }

# ---------- 1. LESSON CRUD (admin) ----------
$l = Api 'L1 create'   'POST' '/api/admin/lessons' 'admin' (J @{title='ZZTEST Lesson v7'; level='ELEMENTARY'; category='Testing'; durationMinutes=10; content='probe content'; isPublished=$false})
$lid = $null; if ($l.code -eq 200) { $lid = ($l.body | ConvertFrom-Json).id }
$l2 = Api 'L2 read'    'GET'  "/api/admin/lessons/$lid" 'admin' $null
$l3 = Api 'L3 update'  'PUT'  "/api/admin/lessons/$lid" 'admin' (J @{title='ZZTEST Lesson v7b'; level='ELEMENTARY'; category='Testing'; durationMinutes=12; content='probe content updated'; isPublished=$false})
$l3b= Api 'L3b partial-null-after-update' 'GET' "/api/admin/lessons/$lid" 'admin' $null
$l4 = Api 'L4 user-create-403' 'POST' '/api/lessons' 'user' (J @{title='hack'; level='ELEMENTARY'})
$l5 = Api 'L5 noauth-create-401/403' 'POST' '/api/lessons' 'none' (J @{title='hack'; level='ELEMENTARY'})
$l6 = Api 'L6 validation (blank title)' 'POST' '/api/admin/lessons' 'admin' (J @{title=''; level='ELEMENTARY'})
$l7 = Api 'L7 validation (bad level)' 'POST' '/api/admin/lessons' 'admin' (J @{title='x'; level='NOT_A_LEVEL'})

# ---------- 2. EXERCISE CRUD (admin) ----------
if ($lid) {
  $e = Api 'E1 create' 'POST' '/api/admin/exercises' 'admin' (J @{lessonId=$lid; question='ZZTEST what is 2+2?'; exerciseType='MULTIPLE_CHOICE'; options='4|five|six'; correctAnswer='4'; explanation='basic'; difficulty='EASY'; orderIndex=1})
  $eid = $null; if ($e.code -in 200,201) { $eid = ($e.body | ConvertFrom-Json).id }
  $e2 = Api 'E2 read'  'GET'  "/api/admin/exercises/$eid" 'admin' $null
  $e3 = Api 'E3 update' 'PUT'  "/api/admin/exercises/$eid" 'admin' (J @{lessonId=$lid; question='ZZTEST q updated'; exerciseType='MULTIPLE_CHOICE'; options='4|five|six'; correctAnswer='4'; explanation='upd'; difficulty='EASY'; orderIndex=1})
  $e4 = Api 'E4 validation missing question' 'POST' '/api/admin/exercises' 'admin' (J @{lessonId=$lid; exerciseType='MULTIPLE_CHOICE'; correctAnswer='x'})
}

# ---------- 3. SPEAKING PROMPT CRUD (admin) + null-guard regression (F20 v6) ----------
$p = Api 'P1 create' 'POST' '/api/v1/admin/speaking-prompts' 'admin' (J @{title='ZZTEST prompt v7'; prompt='Describe your morning routine in 60 seconds.'; description='probe'; level='B1'; category='PROBE'; orderIndex=99; referenceMediaUrl='https://example.com/x'; lessonId=$null; mode='FREE_SPEAKING'})
$pid_ = $null; if ($p.code -in 200,201) { $pid_ = ($p.body | ConvertFrom-Json).id }
if ($pid_) {
  $p2 = Api 'P2 update-partial (F20 null-guard)' 'PUT' "/api/v1/admin/speaking-prompts/$pid_" 'admin' (J @{title='ZZTEST prompt v7b'; description='probe2'})
  $p3 = Api 'P3 verify fields kept' 'GET' "/api/v1/speaking-prompts/$pid_" 'none' $null
  $p4 = Api 'P4 delete' 'DELETE' "/api/v1/admin/speaking-prompts/$pid_" 'admin' $null
}
$p5 = Api 'P5 user-forbidden' 'POST' '/api/v1/admin/speaking-prompts' 'user' (J @{title='hack'; prompt='x'; description='x'; level='B1'})

# ---------- 4. DECK CRUD (user) + ownership ----------
$d = Api 'D1 create' 'POST' '/api/decks' 'user' (J @{name='ZZTEST deck v7'; description='probe'; source='AI_GENERATED'; cefrLevel='B1'; isPublic=$false})
$did = $null; if ($d.code -in 200,201) { $did = ($d.body | ConvertFrom-Json).id }
$d2 = Api 'D2 read private as owner' 'GET' "/api/decks/$did" 'user' $null
$d3 = Api 'D3 read private as admin(non-owner)' 'GET' "/api/decks/$did" 'admin' $null
$d4 = Api 'D4 update by admin(non-owner) expect 4xx' 'PUT' "/api/decks/$did" 'admin' (J @{name='hijack'; source='AI_GENERATED'; cefrLevel='B1'})
$d5 = Api 'D5 delete by admin(non-owner) expect 4xx' 'DELETE' "/api/decks/$did" 'admin' $null
$d6 = Api 'D6 delete by owner' 'DELETE' "/api/decks/$did" 'user' $null
$d7 = Api 'D7 read-after-delete expect 404' 'GET' "/api/decks/$did" 'user' $null

# ---------- 5. AUTH ----------
$reg = @{email="zzprobe$(Get-Random -Maximum 99999)@example.com"; password='123456'; fullName='ZZ Probe'; username="zzprobe$(Get-Random -Maximum 99999)"}
$a1 = Api 'A1 register' 'POST' '/api/auth/register' 'none' (J $reg)
$a2 = Api 'A2 register dup' 'POST' '/api/auth/register' 'none' (J $reg)
$a3 = Api 'A3 login wrong pw' 'POST' '/api/auth/login' 'none' (J @{email='user@gmail.com'; password='wrong'})
$a4 = Api 'A4 forgot-password (user exists)' 'POST' '/api/auth/forgot-password' 'none' (J @{email='user@gmail.com'})
$a5 = Api 'A5 forgot-password (unknown)' 'POST' '/api/auth/forgot-password' 'none' (J @{email='nobody@nowhere.zz'})
$a6 = Api 'A6 reset bogus token' 'POST' '/api/auth/reset-password' 'none' (J @{token='bogus'; password='123456'})
$a7 = Api 'A7 me' 'GET' '/api/auth/me' 'user' $null

# ---------- 6. STREAK / SEARCH / SORT ----------
$s1 = Api 'S1 streak current' 'GET' '/api/streak/current' 'user' $null
$s2 = Api 'S2 streak history' 'GET' '/api/streak/history' 'user' $null
$s2b= Api 'S2b streak history noauth' 'GET' '/api/streak/history' 'none' $null
$x1 = Api 'X1 lessons q' 'GET' '/api/lessons?q=travel&page=0&size=3' 'none' $null
$x2 = Api 'X2 lessons sort order param unsupported check' 'GET' '/api/lessons?sort=title,asc&page=0&size=2' 'none' $null
$x3 = Api 'X3 decks search' 'GET' '/api/decks?q=oxford&page=0&size=3' 'none' $null
$x4 = Api 'X4 vocab search' 'GET' '/api/vocabulary/search?q=happy' 'none' $null
$x5 = Api 'X5 leaderboard' 'GET' '/api/leaderboard' 'none' $null
$x6 = Api 'X6 leaderboard weekly' 'GET' '/api/leaderboard?period=WEEKLY' 'none' $null

# ---------- 7. SUBMIT EXERCISE (user, real grading) ----------
if ($eid) {
  $g1 = Api 'G1 submit correct' 'POST' "/api/lessons/$lid/exercises/submit" 'user' (J @{answers=@(@{exerciseId=$eid; userAnswer='4'})})
  $g2 = Api 'G2 attempts list' 'GET' "/api/lessons/$lid/exercises/attempts" 'user' $null
}

# ---------- 8. cleanup lesson cascade ----------
if ($lid) { $c1 = Api 'C1 delete lesson (cascade)' 'DELETE' "/api/admin/lessons/$lid" 'admin' $null }

$log | ConvertTo-Json -Depth 3 | Set-Content -Encoding utf8 "$PSScriptRoot\v7-crud-r3.json"
Write-Host ("steps={0}" -f $log.Count)
$log | ForEach-Object { Write-Host ("{0,-42} {1} {2}" -f $_.step, $_.code, $_.body.Substring(0,[Math]::Min(110,$_.body.Length))) }
