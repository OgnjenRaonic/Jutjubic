# Performance Test Script za Local Trending
# Simulira različite scenarije i pravi CSV sa rezultatima

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$OutputDelay = 100  # ms između zahteva
)

$results = @()
$scenarios = @(
    @{
        name = "Small Radius (5km)";
        requests = 100;
        radius = 5;
        coords = @(
            @{lat=45.2671; lon=19.8335; name="Beograd"},
            @{lat=45.3150; lon=19.8015; name="Autokomanda"}
        )
    },
    @{
        name = "Medium Radius (20km)";
        requests = 100;
        radius = 20;
        coords = @(
            @{lat=45.2671; lon=19.8335; name="Beograd"},
            @{lat=45.2517; lon=19.8369; name="Novi Sad"}
        )
    },
    @{
        name = "Large Radius (50km)";
        requests = 50;
        radius = 50;
        coords = @(
            @{lat=45.2671; lon=19.8335; name="Beograd"},
            @{lat=43.3209; lon=21.8954; name="Nis"}
        )
    }
)

function Test-TrendingRequest {
    param(
        [string]$Url,
        [string]$Label,
        [string]$Scenario
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop -TimeoutSec 30
        $stopwatch.Stop()
        
        $resultCount = if ($response -is [array]) { $response.Count } elseif ($response -is [object]) { 1 } else { 0 }
        
        return @{
            label = $Label;
            scenario = $Scenario;
            time_ms = [Math]::Round($stopwatch.ElapsedMilliseconds, 2);
            status = "OK";
            result_count = $resultCount;
            timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff");
        }
    }
    catch {
        $stopwatch.Stop()
        return @{
            label = $Label;
            scenario = $Scenario;
            time_ms = [Math]::Round($stopwatch.ElapsedMilliseconds, 2);
            status = "ERROR";
            error = $_.Exception.Message;
            result_count = 0;
            timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff");
        }
    }
}

Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Local Trending Performance Test Started  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target: $BaseUrl"
Write-Host "Scenarios: $($scenarios.Count)"
Write-Host ""

$scenarioCount = 1
foreach ($scenario in $scenarios) {
    Write-Host "[$scenarioCount/$($scenarios.Count)] Running: $($scenario.name)" -ForegroundColor Yellow
    
    for ($i = 0; $i -lt $scenario.requests; $i++) {
        $coord = $scenario.coords[$i % $scenario.coords.Count]
        $url = "$BaseUrl/api/trending/local?lat=$($coord.lat)&lon=$($coord.lon)&radiusKm=$($scenario.radius)"
        
        $result = Test-TrendingRequest $url "$($coord.name)" $scenario.name
        $results += $result
        
        $statusColor = if ($result.status -eq "OK") { "Green" } else { "Red" }
        Write-Host "  [$($i+1)/$($scenario.requests)] $($result.time_ms)ms - $($result.result_count) videos" -ForegroundColor $statusColor
        
        Start-Sleep -Milliseconds $OutputDelay
    }
    
    $scenarioCount++
    Write-Host ""
}

# Analiza rezultata po scenariju
Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║           Performance Summary              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$results | Group-Object scenario | ForEach-Object {
    $times = $_.Group | Where-Object { $_.status -eq "OK" } | ForEach-Object { $_.time_ms }
    $errors = ($_.Group | Where-Object { $_.status -eq "ERROR" }).Count
    
    if ($times.Count -gt 0) {
        $avg = [Math]::Round(($times | Measure-Object -Average).Average, 2)
        $min = [Math]::Round(($times | Measure-Object -Minimum).Minimum, 2)
        $max = [Math]::Round(($times | Measure-Object -Maximum).Maximum, 2)
        
        # Percentile P95
        [array]::Sort($times)
        $p95Index = [Math]::Floor($times.Count * 0.95) - 1
        $p95 = if ($p95Index -ge 0) { [Math]::Round($times[$p95Index], 2) } else { $times[0] }
        
        Write-Host "Scenario: $($_.Name)"
        Write-Host "  Requests: $($times.Count) ✓ / $errors ✗"
        Write-Host "  Min: $($min)ms | Avg: $($avg)ms | Max: $($max)ms | P95: $($p95)ms"
        Write-Host ""
    }
}

# Eksportuj u CSV
$csvPath = Join-Path (Get-Location) "trending_performance_results.csv"
$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

Write-Host "✓ Results exported to: $csvPath" -ForegroundColor Green
Write-Host ""
Write-Host "Detailed results:" -ForegroundColor Yellow
$results | Where-Object { $_.status -eq "OK" } | Select-Object timestamp, scenario, label, time_ms, result_count | Format-Table -AutoSize
