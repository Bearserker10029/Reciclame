# Guía de Comandos — Recíclame Local (Docker Compose)

Entorno de desarrollo local con MySQL, MinIO, n8n, kkfileview y la app Spring Boot construida desde el `Dockerfile` del proyecto. La BD de **producción (RDS) NO se toca**.

---

## Información del Entorno

- **Stack**: Docker Compose (servicios `mysql`, `app`, `minio`, `minio-init`, `n8n`, `kkfileview`)
- **App**: 1 sola instancia (no hay balanceo, no hay `nginx_lb`)
- **Sesiones**: JDBC en MySQL (no se usa Redis en local)
- **Imagen de la app**: `reciclame-app:local` — se construye con tu `Dockerfile`
- **MySQL**: contenedor `mysql:8.0`, seed automático desde `src/main/resources/db/reciclame_db.sql`
- **Perfil Spring**: `local`

---

## Arquitectura

```
                    +-----------------------------+
                    |     reciclame-app :8080     |   (build local, perfil local)
                    |     Spring Boot + Thymeleaf |
                    +-------------+---------------+
                                  |
       +--------------------------+--------------------------+
       |              |           |           |              |
       v              v           v           v              v
  +---------+   +-----------+  +-------+  +-------+  +----------------+
  |  mysql  |   |   minio   |  |  n8n  |  | kkfv  |  | minio-init     |
  |  :3306  |   | :9000/01  |  | :5678 |  | :8012 |  | (one-shot)     |
  +---------+   +-----------+  +-------+  +-------+  +----------------+
       |              |
       v              v
  mysql_data      minio_data               n8n_data
  (volume)        (volume)                 (volume)
```

**Contenedores**: `reciclame-mysql-local`, `reciclame-app`, `reciclame-minio`, `reciclame-minio-init`, `reciclame-n8n`, `reciclame-kkfileview`

**Volúmenes Docker**: `reciclame_mysql_data`, `reciclame_minio_data`, `reciclame_n8n_data`

---

## 1. Prerrequisitos

- **Docker Desktop** con `docker compose` v2 (incluido en Docker 20.10+).
- **BuildKit** habilitado (Docker 29 lo trae por defecto). Si usas una versión vieja, exporta `DOCKER_BUILDKIT=1` antes de los builds.
- **Puertos libres en el host**: `8080`, `9000`, `9001`, `5678`, `8012`.
- **Archivo de seed**: `src/main/resources/db/reciclame_db.sql` (ya está en el repo, no se descarga nada externo).

---

## 2. Levantar el stack por primera vez

```powershell
cd E:\Github\Proyectos\GTICS\Reciclame

# Build de la imagen de la app + descarga del resto de imágenes
docker compose -f docker-compose.local.yml build

# Levantar todos los servicios en background
docker compose -f docker-compose.local.yml up -d
```

La primera vez tarda varios minutos:

1. Build de `reciclame-app:local` (Maven baja dependencias, compila y empaqueta — usa el cache de Docker para `~/.m2`).
2. Pull de `mysql:8.0`, `minio/minio`, `minio/mc`, `n8nio/n8n`, `keking/kkfileview`.
3. MySQL ejecuta el seed desde `src/main/resources/db/reciclame_db.sql` (tarda ~30–60 s en la primera inicialización).
4. MinIO arranca y `minio-init` crea el bucket `reciclame-docs`.

> **Si MySQL no carga el seed a tiempo** y la app falla al iniciar con un error de tabla inexistente, espera 1 minuto y reinicia solo el contenedor de la app:
> ```powershell
> docker compose -f docker-compose.local.yml restart app
> ```

---

## 3. Ver estado de los servicios

```powershell
# Estado actual
docker compose -f docker-compose.local.yml ps

# Ver qué servicio está unhealthy
docker compose -f docker-compose.local.yml ps --format json | Select-String -Pattern "health"
```

---

## 4. Ver logs

```powershell
# Todos los servicios, en tiempo real
docker compose -f docker-compose.local.yml logs -f

# Solo la app
docker compose -f docker-compose.local.yml logs -f app

# Últimas 200 líneas de MySQL (para diagnosticar el seed)
docker compose -f docker-compose.local.yml logs --tail=200 mysql
```

---

## 5. URLs de los servicios

| Servicio              | URL local                          | Credenciales                |
|-----------------------|------------------------------------|-----------------------------|
| App (Spring Boot)     | http://localhost:8080              | ver [Credenciales](#7-credenciales-de-usuarios-del-seed) |
| MinIO Console         | http://localhost:9001              | `minioadmin` / `minioadmin` |
| MinIO API             | http://localhost:9000              | — (la app genera URLs firmadas) |
| n8n                   | http://localhost:5678              | crear cuenta en primer arranque |
| kkfileview            | http://localhost:8012              | — (sin auth, lo llama la app) |
| MySQL (interno)       | `mysql:3306` (red Docker)          | `root` / `root`             |

> MySQL **no** expone el puerto 3306 al host a propósito. Se accede solo desde los contenedores de la red `reciclame-local`. Si necesitas conectarte desde MySQL Workbench u otro cliente, usa `docker exec reciclame-mysql-local mysql -uroot -proot reciclame_db`.

---

## 6. Healthcheck de la app

```powershell
# Confirmar que la app arrancó
curl http://localhost:8080/actuator/health

# Esperado: {"status":"UP"}
```

---

## 7. Credenciales de usuarios del seed

Definidas en `src/main/resources/db/reciclame_db.sql` (contraseñas cifradas con BCrypt):

| Correo                          | Contraseña   | Rol            |
|---------------------------------|--------------|----------------|
| `admin@reciclame.com`           | `admin123`   | ADMINISTRADOR  |
| `superadmin@reciclame.com`      | `super123`   | SUPERADMIN     |
| `socio@reciclame.com`           | `socio123`   | SOCIO          |
| `visualizador@reciclame.com`    | `vis123`     | VISUALIZADOR   |
| `pendiente@reciclame.com`       | `password`   | PENDIENTE      |

> Las URLs `archivo_url` en `normativas` apuntan a `http://localhost:9000/reciclame-docs/...`. Después de levantar MinIO, los archivos listados **no existirán** en el bucket hasta que los subas manualmente o ejecutes una migración. La app funcionará; solo no habrá PDFs que previsualizar.

---

## 8. Reconstruir la app tras cambios de código

```powershell
# Opción A: rebuild + recreate (recomendado, asegura que la nueva imagen se use)
docker compose -f docker-compose.local.yml up -d --build app

# Opción B: solo el build, luego restart manual
docker compose -f docker-compose.local.yml build app
docker compose -f docker-compose.local.yml up -d app
```

> El `Dockerfile` usa `RUN --mount=type=cache,target=/root/.m2`, así que las dependencias ya bajadas se reusan entre builds. Solo el código recompilado se vuelve a empaquetar.

---

## 9. Resetear la base de datos (re-seedear)

El seed de MySQL solo se ejecuta **la primera vez** que se inicializa el volumen `reciclame_mysql_data`. Para forzarlo de nuevo:

```powershell
# 1. Bajar todo y BORRAR el volumen de MySQL
docker compose -f docker-compose.local.yml down -v

# 2. Levantar de nuevo (MySQL ejecuta el seed desde cero)
docker compose -f docker-compose.local.yml up -d
```

> Esto borra **toda** la base local (incluyendo cambios de pruebas). No afecta al RDS de producción.

Si solo quieres borrar datos sin tocar tablas:

```powershell
docker exec -it reciclame-mysql-local mysql -uroot -proot reciclame_db -e "SHOW TABLES;"
# Luego, los TRUNCATE que necesites
```

---

## 10. Detener todo

```powershell
# Detener contenedores (conservando volúmenes)
docker compose -f docker-compose.local.yml down

# Detener + eliminar volúmenes (limpieza total)
docker compose -f docker-compose.local.yml down -v
```

---

## 11. Conectarse a MySQL desde la línea de comandos

```powershell
# Desde un cliente del host (no necesitas instalar mysql)
docker exec -it reciclame-mysql-local mysql -uroot -proot reciclame_db

# Dentro del cliente MySQL:
#   SHOW DATABASES;
#   SELECT id, correo_corporativo, rol_id FROM usuarios;
#   EXIT;
```

---

## 12. MinIO: subir un archivo de prueba

```powershell
# 1. Abre la consola: http://localhost:9001 (minioadmin / minioadmin)
# 2. Entra al bucket "reciclame-docs" (ya está creado por minio-init).
# 3. Click en "Upload" y sube un PDF cualquiera.
# 4. El archivo queda accesible vía URL firmada que la app genera
#    (no es público: anonymous=none, por seguridad).
```

Para generar una URL temporal con `mc` desde el host:

```powershell
docker exec -it reciclame-minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec -it reciclame-minio mc anonymous set download local/reciclame-docs/tu-archivo.pdf
# (esto lo hace público SOLO para ese archivo, no el bucket entero)
```

---

## 13. n8n: primer arranque

```powershell
# Abre http://localhost:5678 y crea tu cuenta (no hay usuario por defecto).
# El webhook que usa la app es:
#   POST http://n8n:5678/webhook/send-email         (envío de correos)
#   POST http://n8n:5678/webhook/chatbot-reciclame  (chatbot)
```

> En el compose local, el `N8N_CHATBOT_WEBHOOK_URL` está configurado como path relativo (`/webhook/chatbot-reciclame`). Si tu workflow de n8n escucha en otra ruta, ajusta la env var en `docker-compose.local.yml`.

---

## 14. Diferencias con el entorno de EC2 (`docker-compose.yml`)

| Aspecto                 | EC2 (`docker-compose.yml`)            | Local (`docker-compose.local.yml`)           |
|-------------------------|----------------------------------------|----------------------------------------------|
| Instancias de la app    | 3 (app1, app2, app3)                  | 1 (`app`)                                    |
| Balanceo               | `nginx_lb` (round-robin)              | No hay — la app expone 8080 directo          |
| HTTPS / dominio        | `nginx-proxy-manager` + DuckDNS       | No — todo en HTTP plano sobre localhost      |
| Base de datos          | AWS RDS (`reciclame-db...amazonaws.com`) | MySQL 8.0 local (contenedor `mysql`)         |
| Sesiones               | Redis (Spring Session Redis)          | JDBC en MySQL (Spring Session JDBC)          |
| Redis                  | Contenedor `redis:7.4`                | **No incluido**                              |
| kkfileview             | Contenedor compartido                 | Contenedor dedicado                          |
| Volúmenes de backup    | `backup_data` → `/backups` en la app  | **No incluido**                             |
| Perfil Spring          | `aws`                                  | `local`                                      |
| Credenciales DB        | `admin` / `Reciclame123`              | `root` / `root`                              |

---

## 15. Troubleshooting

| Síntoma                                                                 | Causa probable                                                   | Solución                                                                                       |
|-------------------------------------------------------------------------|------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| La app falla con `Unknown database 'reciclame_db'`                      | MySQL aún no terminó de inicializar el seed                      | `docker compose -f docker-compose.local.yml restart app`                                       |
| La app falla con `Table ... doesn't exist`                              | Seed no se cargó (mismo problema)                                | Ver logs de MySQL: `... logs --tail=200 mysql`; si está vacío, `down -v` y `up -d`            |
| `Bind for 0.0.0.0:8080 failed: port already allocated`                 | Otro proceso usa el 8080 en el host                              | Cierra el proceso o cambia el mapeo en el compose (`8081:8080`)                                |
| `Cannot connect to the Docker daemon`                                  | Docker Desktop no está corriendo                                  | Inicia Docker Desktop                                                                          |
| Build de la app muy lento                                              | Primera vez, baja todas las deps de Maven                        | Espera; el segundo build será mucho más rápido gracias al cache `~/.m2`                       |
| `kkfileview` no previsualiza PDFs                                      | `app.kk-accessible-url` mal configurado                          | Confirma que en el compose apunta a `http://reciclame-app:8080` (DNS interno de Docker)        |
| Login devuelve 500 y log dice `Communications link failure` con MySQL   | MySQL murió o no terminó el healthcheck                          | `docker compose -f docker-compose.local.yml ps mysql` y `logs --tail=100 mysql`               |
| Cambios en `application.properties` no se reflejan                    | El JAR fue compilado dentro de la imagen                         | `docker compose -f docker-compose.local.yml up -d --build app`                                |
| Olvidé la contraseña del seed                                          | El seed es de solo lectura                                       | `docker exec -it reciclame-mysql-local mysql -uroot -proot reciclame_db` y `UPDATE usuarios SET password_hash='...' WHERE correo_corporativo='...';` (genera un hash BCrypt nuevo con un script) |

---

## 16. Flujo de desarrollo típico

```powershell
# 1. Levantar
docker compose -f docker-compose.local.yml up -d --build

# 2. Esperar ~60s y comprobar
curl http://localhost:8080/actuator/health

# 3. Abrir http://localhost:8080/login y entrar con admin@reciclame.com / admin123

# 4. Modificar código en src/... y rebuild solo la app
docker compose -f docker-compose.local.yml up -d --build app

# 5. Al terminar el día
docker compose -f docker-compose.local.yml down
# (los volúmenes persisten; tus datos siguen ahí mañana)
```

---

## 17. Archivos relevantes

| Ruta                                               | Propósito                                                |
|----------------------------------------------------|----------------------------------------------------------|
| `docker-compose.local.yml`                         | Este stack de desarrollo (NO se sube a EC2)              |
| `docker-compose.yml`                               | Stack de producción en EC2 (no tocar)                    |
| `src/main/resources/db/reciclame_db.sql`           | Seed de la base de datos (montado en MySQL)              |
| `Dockerfile`                                       | Build multi-stage de la app (JDK 21)                     |
| `src/main/resources/application.properties`        | Config perfil `local` (defaults de `localhost`)          |
| `src/main/resources/application-aws.properties`    | Config perfil `aws` (env vars, RDS, Redis)               |
| `kkfileview-templates/web/`                        | Plantillas personalizadas de kkfileview                  |
| `guia-comandos-ec2.md`                             | Guía del entorno productivo                              |
| `guia-comandos-local.md`                           | Esta guía                                                |
