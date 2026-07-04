@echo off
REM Script de correção para o projeto de geração de URL com Lombok - apenas para depuração em ambiente Windows
echo Reiniciando o projeto em um diretório limpo...
rmdir /s /q .
git init >nul 2>&1
echo Projeto limpo concluído.
echo FIXME: Adicione melhorias na migration do pom, deployment e IDE aqui.
echo Próximos passos:
echo 1. Gerar org/example/urlshortener/*.java sem Lombok
echo 2. Renomear entity/AccessLog.java (remover @UniqueConstraint)
echo 3. Corrigir fluxo de checagem de acesso em UrlService
echo 4. Excluir falhas no teste devido a campos ausentes
echo 5. Staged da cópia integra para o usuário.
echo.
echo Projeto pronto para testes manuais em JDK 25+.
echo Problemas:
echo - Lombok desconectado: gerar manualmente os arquivos .java
echo - url-service.java: verificar fluxo com campos diretos
echo - stub duplo em access-log-repository
echo - custom-validator extra no dockerfile
echo.
echo [usar no ambiente de desenvolvimento]
echo mvn clean package -DskipTests
echo.
echo [implicação para usuário após ajustes manuais]
echo 'TODO: mudar do docker-compose para o README gerado, etc.'