# Skript za simulaciju slanja komentara (stress test)
# Prosledi login token ili registruj novog korisnika prvo

$baseUrl = "http://localhost:8080/api"
$videoId = 1  # Koristi postojeći video
$sessionCookie = $null

# 1. Registruj novog korisnika
Write-Host "=== REGISTRACIJA ===" -ForegroundColor Green
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testEmail = "stress-test-$timestamp@test.com"
$testUsername = "stresstest_$timestamp"

$registerResponse = Invoke-WebRequest -Uri "$baseUrl/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body @{
        email = $testEmail
        username = $testUsername
        password = "Test@12345"
        passwordConfirm = "Test@12345"
        firstName = "Stress"
        lastName = "Test"
        address = "Test St."
    } | ConvertFrom-Json

Write-Host "Korisnik registrovan: $testEmail" -ForegroundColor Green

# 2. Aktiviraj korisnika (simulacija - u produkciji bi se slao email)
Write-Host ""
Write-Host "=== AKTIVACIJA ===" -ForegroundColor Green
Write-Host "Napomena: U realnom okruženju bi se slao email sa aktivacijskim linkom"
Write-Host "Za test, trebalo bi ručno pozvati /api/activate?token=... ili promeniti enabled flag direktno u DB"
Write-Host ""

# 3. Login
Write-Host "=== LOGIN ===" -ForegroundColor Green
$loginResponse = Invoke-WebRequest -Uri "$baseUrl/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body @{
        email = $testEmail
        password = "Test@12345"
    } `
    -SessionVariable session

Write-Host "Login uspešan" -ForegroundColor Green

# 4. Stress test - pošalji 65 komentara
Write-Host ""
Write-Host "=== STRESS TEST - SLANJE 65 KOMENTARA ===" -ForegroundColor Cyan
$successCount = 0
$rejectedCount = 0
$errorCount = 0

for ($i = 1; $i -le 65; $i++) {
    try {
        $commentText = "Test komentar broj $i sa vremenskom oznakom $(Get-Date -Format 'HH:mm:ss')"
        
        $response = Invoke-WebRequest -Uri "$baseUrl/comments/video/$videoId" `
            -Method POST `
            -ContentType "application/json" `
            -Body @{
                text = $commentText
            } | ConvertFrom-Json | ConvertTo-Json
        
        Write-Host "[✓] Komentar $i poslat uspešno" -ForegroundColor Green
        $successCount++
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 429) {
            Write-Host "[✗] Komentar $i ODBIJEN - Rate limit (429)" -ForegroundColor Red
            $rejectedCount++
        }
        elseif ($statusCode -eq 401) {
            Write-Host "[✗] Komentar $i ODBIJEN - Neautentifikovan (401)" -ForegroundColor Red
            $errorCount++
        }
        else {
            Write-Host "[✗] Komentar $i GREŠKA - Status: $statusCode" -ForegroundColor Yellow
            $errorCount++
        }
    }
    
    # Mali delay između zahteva
    Start-Sleep -Milliseconds 50
}

Write-Host ""
Write-Host "=== REZULTATI ===" -ForegroundColor Cyan
Write-Host "Prihvaćenih komentara: $successCount (očekivano: 60)" -ForegroundColor Green
Write-Host "Odbijenih komentara (rate limit): $rejectedCount (očekivano: 5)" -ForegroundColor $(if ($rejectedCount -ge 4) { 'Green' } else { 'Red' })
Write-Host "Grešaka: $errorCount" -ForegroundColor $(if ($errorCount -eq 0) { 'Green' } else { 'Red' })

# 5. Proveri rate limit info
Write-Host ""
Write-Host "=== RATE LIMIT INFO ===" -ForegroundColor Cyan
try {
    $rateLimitResponse = Invoke-WebRequest -Uri "$baseUrl/comments/rate-limit-info" `
        -Method GET `
        -WebSession $session | ConvertFrom-Json
    
    Write-Host "Komentara u poslednjem satu: $($rateLimitResponse.commentsInLastHour)" -ForegroundColor Cyan
    Write-Host "Može komentarisati: $($rateLimitResponse.canComment)" -ForegroundColor $(if ($rateLimitResponse.canComment) { 'Red' } else { 'Green' })
    if ($rateLimitResponse.nextAvailableAt) {
        Write-Host "Dostupno od: $($rateLimitResponse.nextAvailableAt)" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "Greška pri čitanju rate limit info" -ForegroundColor Red
}

Write-Host ""
Write-Host "TEST ZAVRŠEN" -ForegroundColor Cyan
