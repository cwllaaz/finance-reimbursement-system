$password = Read-Host "Enter MySQL root password"
$env:MYSQL_ROOT_PASSWORD = $password

Set-Location "$PSScriptRoot\backend"
.\mvnw.cmd spring-boot:run
