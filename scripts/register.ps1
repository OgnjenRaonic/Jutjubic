Invoke-WebRequest -Uri 'http://localhost:8080/register' -Method POST -Body @{ 
    email='test5@example.com';
    username='testuser5';
    password='Secret123';
    passwordConfirm='Secret123';
    firstName='Test';
    lastName='User';
    address='Local'
} -ContentType 'application/x-www-form-urlencoded' -UseBasicParsing -TimeoutSec 30

"Request completed with status: $($LASTEXITCODE)" | Out-Host