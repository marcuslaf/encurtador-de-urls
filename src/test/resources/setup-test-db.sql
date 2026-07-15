-- Script para criar o banco de dados de teste
-- Execute este script no seu PostgreSQL local

-- Criar banco de dados (se não existir)
-- Nota: Isso deve ser executado como superusuário (postgres)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'urlshortener_test') THEN
      PERFORM dblink_exec('dbname=postgres user=postgres', 'CREATE DATABASE urlshortener_test');
   END IF;
END
$$;

-- Conectar ao banco criado
\c urlshortener_test;

-- As tabelas serão criadas automaticamente pelo Flyway quando os testes rodarem
-- O arquivo V1__init.sql já contém o schema necessário

-- Verificar conexão
SELECT 'Banco de dados urlshortener_test pronto para testes!' AS status;