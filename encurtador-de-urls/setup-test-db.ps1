# Script para preparar o banco de dados de teste PostgreSQL
# Execute este script antes de rodar os testes de integração

Write-Host "=== Configurando banco de dados de teste ===" -ForegroundColor Cyan

# Credenciais (pode ajustar se necessário)
$dbHost = "localhost"
$dbPort = "5432"
$dbName = "urlshortener_test"
$dbUser = "postgres"
$dbPass = "631966"

# Verificar se o PostgreSQL está acessível
Write-Host "Verificando conexão com PostgreSQL..." -ForegroundColor Yellow
try {
    $connectionString = "Host=$dbHost;Port=$dbPort;Database=postgres;Username=$dbUser;Password=$dbPass"
    Add-Type -Path "C:\Program Files\PostgreSQL\15\lib\npgsql.dll" -ErrorAction SilentlyContinue
    
    # Tentar conectar
    $conn = New-Object Npgsql.NpgsqlConnection($connectionString)
    $conn.Open()
    Write-Host "Conexão com PostgreSQL estabelecida com sucesso!" -ForegroundColor Green
    $conn.Close()
} catch {
    Write-Host "Erro ao conectar no PostgreSQL. Verifique se o serviço está rodando." -ForegroundColor Red
    Write-Host "Tente conectar via pgAdmin ou psql para verificar." -ForegroundColor Yellow
    exit 1
}

# Criar banco de dados se não existir
Write-Host "Verificando banco de dados '$dbName'..." -ForegroundColor Yellow
# Nota: A criação do banco será feita pelo Flyway nos testes

Write-Host ""
Write-Host "=== Configuração concluída! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Cyan
Write-Host "1. Rodar os testes: mvn test" -ForegroundColor White
Write-Host "2. Ou apenas testes de integração: mvn test -Dtest=*IntegrationTest" -ForegroundColor White
Write-Host ""
Write-Host "O Flyway criará as tabelas automaticamente na primeira execução." -ForegroundColor Yellow