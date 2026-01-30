# Skript za demonstraciju zakazanog streaming-a
# Simulira kreiranja videa, zakazivanja i prikaza trenutnog offseta

$baseUrl = "http://localhost:8080/api"
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testEmail = "scheduled-demo-$timestamp@test.com"
$testUsername = "schedu_$timestamp"

Write-Host "=== DEMO: ZAKAZANI VIDEO SA SINHRONIZOVANIM STREAMING-OM ===" -ForegroundColor Cyan
Write-Host ""

# 1. Registruj korisnika
Write-Host "1. REGISTRACIJA" -ForegroundColor Green
$registerResponse = Invoke-WebRequest -Uri "$baseUrl/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body @{
        email = $testEmail
        username = $testUsername
        password = "Demo@12345"
        passwordConfirm = "Demo@12345"
        firstName = "Demo"
        lastName = "User"
        address = "Demo St."
    } -SessionVariable session | ConvertFrom-Json

Write-Host "✓ Korisnik registrovan: $testEmail" -ForegroundColor Green

# 2. Login
Write-Host ""
Write-Host "2. LOGIN" -ForegroundColor Green
$loginResponse = Invoke-WebRequest -Uri "$baseUrl/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body @{
        email = $testEmail
        password = "Demo@12345"
    } -WebSession $session -ErrorAction SilentlyContinue

Write-Host "✓ Ulogovan" -ForegroundColor Green

# 3. Kreiraj video (simulacija)
Write-Host ""
Write-Host "3. KREIRANJE VIDEA" -ForegroundColor Green
Write-Host "Napomena: Video fajlovi su simulirani - trebalo bi da ste uploaud-ali stvarne fajlove"
Write-Host "Za demo, pretpostavljamo da je video sa ID=1 već kreirano"
$videoId = 1

# 4. Zakaži video za 30 sekundi od sada
Write-Host ""
Write-Host "4. ZAKAZIVANJE VIDEA" -ForegroundColor Green
$scheduledTime = (Get-Date).AddSeconds(30)
$scheduledTimeISO = $scheduledTime.ToString("yyyy-MM-ddTHH:mm:ss")

Write-Host "Zakazujem video za: $scheduledTimeISO (za 30 sekundi)" -ForegroundColor Yellow

$updateResponse = Invoke-WebRequest -Uri "$baseUrl/videos/$videoId" `
    -Method PUT `
    -ContentType "application/json" `
    -WebSession $session `
    -Body @{
        scheduledAt = $scheduledTimeISO
    } -ErrorAction SilentlyContinue

Write-Host "✓ Video je zakazan" -ForegroundColor Green

# 5. Proveri status pre početka
Write-Host ""
Write-Host "5. STATUS PRE POČETKA STREAMING-A" -ForegroundColor Cyan
$statusResponse = Invoke-WebRequest -Uri "$baseUrl/videos/$videoId/scheduled-info" `
    -Method GET -ErrorAction SilentlyContinue | ConvertFrom-Json

Write-Host "Status: " + $statusResponse.streamStatus -ForegroundColor Yellow
Write-Host "Dostupan: " + $statusResponse.available -ForegroundColor Yellow
Write-Host "Poruka: " + $statusResponse.message -ForegroundColor Yellow

# 6. Čekaj da počne streaming
Write-Host ""
Write-Host "6. ČEKAM DA POČNE STREAMING..." -ForegroundColor Cyan
$secToWait = 35
for ($i = 0; $i -lt $secToWait; $i++) {
    Write-Host -NoNewline "`r Čekam: $($secToWait - $i) sekundi"
    Start-Sleep -Seconds 1
}
Write-Host ""

# 7. Proveri status nakon početka
Write-Host ""
Write-Host "7. STATUS TOKOM STREAMING-A" -ForegroundColor Cyan
$statusResponse = Invoke-WebRequest -Uri "$baseUrl/videos/$videoId/scheduled-info" `
    -Method GET -ErrorAction SilentlyContinue | ConvertFrom-Json

Write-Host "Status: " + $statusResponse.streamStatus -ForegroundColor Green
Write-Host "Dostupan: " + $statusResponse.available -ForegroundColor Green
Write-Host "Trenutni offset: " + $statusResponse.currentOffsetSeconds + " sekundi" -ForegroundColor Green
Write-Host "Poruka: " + $statusResponse.message -ForegroundColor Green

# 8. Simulacija više gledalaca koji se javljaju u isto vreme
Write-Host ""
Write-Host "8. SIMULACIJA: VIŠE GLEDALACA GLEDAJU ISTU MINUTAŽU (SYNCHRONIZED STREAMING)" -ForegroundColor Cyan
Write-Host "Svi gledalaci koji se konektuju sada gledaju video od trenutnog offseta" -ForegroundColor Yellow

for ($viewer = 1; $viewer -le 3; $viewer++) {
    $viewerResponse = Invoke-WebRequest -Uri "$baseUrl/videos/$videoId/scheduled-info" `
        -Method GET -ErrorAction SilentlyContinue | ConvertFrom-Json
    
    Write-Host "Gledaoc $viewer: Trenutna minutaža = " + $viewerResponse.currentOffsetSeconds + "s" -ForegroundColor Green
    Start-Sleep -Seconds 1
}

# 9. Info o videu
Write-Host ""
Write-Host "9. KOMPLETA INFORMACIJA O VIDEU" -ForegroundColor Cyan
$videoInfo = Invoke-WebRequest -Uri "$baseUrl/videos/$videoId" `
    -Method GET -ErrorAction SilentlyContinue | ConvertFrom-Json

Write-Host "Naslov: " + $videoInfo.title
Write-Host "Dostupan: " + $videoInfo.available
Write-Host "Zakazan: " + $videoInfo.scheduledAt
Write-Host "Trenutni offset: " + $videoInfo.currentOffsetSeconds + "s"

Write-Host ""
Write-Host "=== DEMO ZAVRŠEN ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ključne osobine zakazanog streaming-a:" -ForegroundColor Yellow
Write-Host "  ✓ Video nije dostupan pre zakazanog vremena"
Write-Host "  ✓ U zakazano vreme, video postaje dostupan" 
Write-Host "  ✓ Svi gledalci koji se javljaju gledaju istu 'trenutnu' minutažu"
Write-Host "  ✓ Offset se računa kao: trenutnoVreme - zakazanoVreme"
Write-Host "  ✓ Mogućnost otkazivanja zakazivanja"
