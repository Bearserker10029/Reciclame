-- El cliente mysqldump instalado en la imagen de la app (Alpine/MariaDB) no
-- soporta el plugin caching_sha2_password que MySQL 8 usa por defecto para
-- root. Sin esto, BackupService no puede generar backups (mysqldump falla
-- al conectar). Se cambia a mysql_native_password, compatible con ambos
-- clientes.
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'root';
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
FLUSH PRIVILEGES;
