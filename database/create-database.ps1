$password = Read-Host "Enter MySQL root password"

mysql -u root "-p$password" -e "CREATE DATABASE IF NOT EXISTS finance_reimbursement DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database finance_reimbursement is ready." -ForegroundColor Green
} else {
    Write-Host "Failed to create database. Please check the MySQL root password." -ForegroundColor Red
}
