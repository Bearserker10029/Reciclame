# ♻️ Reciclame — Repositorio Circular

> Plataforma web tipo intranet desarrollada con Spring Boot y Thymeleaf que transforma el repositorio documental de la **Asociación Recíclame Perú** —hoy una carpeta de Drive y un Excel de normativas— en un portal digital estructurado, seguro y de acceso restringido para sus socios.

## 🏫 Contexto del Proyecto

| Dato | Detalle |
|------|---------|
| **Cliente** | Asociación Recíclame Perú (reciclame.org) — asociación civil sin fines de lucro que impulsa la transición hacia una **economía circular**, con foco en el reciclaje de envases y embalajes |
| **Contacto del cliente** | Alexandra Jessica Tapia Sarmiento |
| **Curso** | TEL137 — Gestión de Servicios de TICS (GTICS) |
| **Especialidad** | Ingeniería de las Telecomunicaciones, PUCP |
| **Ciclo** | 2026-1 (marzo – julio 2026) |
| **Profesora** | Brenda Tumbalobos Cubas |
| **Equipo** | Grupos de 4–5 alumnos por jefe de práctica |

> **Misión de Recíclame:** impulsar la transición hacia una economía circular, con un foco en el reciclaje. Ejes de trabajo: Gestión del conocimiento, Educación técnica y Programa REP (Responsabilidad Extendida del Productor).

## 📋 Tabla de Contenidos

- [Descripción](#-descripción-del-proyecto)
- [Contexto](#-contexto-del-proyecto)
- [Roles de Acceso](#-roles-de-acceso)
- [Tecnologías](#-tecnologías-usadas)
- [Estructura](#-estructura-principal)
- [Flujo Funcional](#-flujo-funcional-implementado)
- [Regla de Negocio](#-regla-de-negocio-flujo-de-validación-de-documentos)
- [Estado](#-estado-del-proyecto)
- [Ejecución](#-cómo-ejecutar)

---

## 📝 Descripción del Proyecto

**Problema actual:** el Repositorio Circular de Recíclame vive en Google Drive institucional (carpetas por temática + Excel maestro de normativas con estados como *Vigente, Proyecto publicado, Consulta pública, Derogada*), compartido manualmente con los correos de las empresas socias (Alicorp, Tetra Pak, Qroma, SMI, Arca Continental, Lindley, O-I). Esto impide trazabilidad, control de acceso y actualización continua.

**La solución:** una plataforma web tipo intranet donde los socios autorizados consultan, visualizan y descargan información según niveles de acceso definidos, con trazabilidad de uso, seguridad de la información y capacidad de actualización continua por parte de Recíclame.

La aplicación permite a los usuarios:

✅ Publicar y consultar normativas y estudios del sector  
✅ Visualizar documentos en línea (PDF, Word, Excel, PowerPoint)  
✅ Validar archivos subidos contra VirusTotal antes de publicarlos  
✅ Chatear en tiempo real entre usuarios de la plataforma  
✅ Interactuar con un chatbot de IA para consultas frecuentes  
✅ Gestionar usuarios, roles y solicitudes de registro (Admin / SuperAdmin)  
✅ Auditar la actividad del sistema y programar respaldos automáticos  
✅ Exportar reportes en PDF y Excel

## 👥 Roles de Acceso

La plataforma define niveles de acceso según los requerimientos del cliente:

| Rol | Descripción | Permisos principales |
|-----|-------------|----------------------|
| **Socio** | Usuario de una empresa asociada (registro validado por el administrador) | Consultar y visualizar estudios y normativas según clasificación (solo lectura o descargable), recibir notificaciones |
| **Visualizador** | Acceso de solo consulta | Visualizar documentos clasificados como solo lectura (estudios Recíclame y normas técnicas peruanas) |
| **Administrador** | Personal de Recíclame | CRUD de documentos y usuarios, validar solicitudes de registro, generar reportes de uso, auditar actividad |
| **SuperAdmin** | Rol propuesto por la PUCP como diferenciador adicional | Control total de la plataforma: configuración de vistas por rol, mantenimiento, respaldos e impersonación de usuarios |

**Clasificación de documentos por tipo de acceso:**
- 🔒 **Solo lectura** (visualización sin descarga): Estudios Recíclame y normas técnicas peruanas
- ⬇️ **Descargables**: documentos de acceso público como regulaciones y normas de dominio público

## 💻 Tecnologías Usadas

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 26 | Lenguaje base |
| Spring Boot | 4.0.6 | Framework MVC |
| Spring MVC | Incluido | Controladores HTTP |
| Spring Security + Session JDBC | Incluido | Autenticación y sesiones |
| Spring Data JPA | Incluido | Persistencia ORM |
| Spring Data Redis | Incluido | Sesiones distribuidas |
| Thymeleaf | Starter | Motor de plantillas |
| MySQL | 8.x | Base de datos relacional |
| MinIO | 8.6.0 | Almacenamiento de objetos (S3) |
| JasperReports | 7.0.7 | Reportes PDF |
| Apache POI | 5.2.3 | Exportación Excel |
| JJWT | 0.12.6 | Tokens JWT (Centrifugo) |
| kkfileview | — | Visualizador de documentos |
| Docker + nginx | — | Despliegue y balanceo |
| Maven | Wrapper | Gestor de dependencias |
| Lombok | Latest | Anotaciones de código |

## 📂 Estructura Principal

```
Reciclame/
├── src/
│   ├── main/
│   │   ├── java/org/reciclame/demo/
│   │   │   ├── DemoApplication.java
│   │   │   ├── config/            (14 archivos: Security, MinIO, Redis, etc.)
│   │   │   ├── controller/        (33 controladores REST y MVC)
│   │   │   ├── dto/               (39 DTOs)
│   │   │   ├── model/             (39 entidades JPA)
│   │   │   ├── repository/        (31 repositorios)
│   │   │   ├── service/           (42 servicios)
│   │   │   ├── scheduler/         (tareas programadas)
│   │   │   ├── helper/            (utilidades)
│   │   │   ├── exception/         (manejo de errores)
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/                (scripts SQL)
│   │       ├── static/
│   │       └── templates/         (82 vistas Thymeleaf)
│   └── test/
├── Dockerfile                     (multi-stage build)
├── nginx.conf                     (proxy inverso)
├── docker-compose.local.yml       (entorno local)
└── pom.xml
```

## 🔄 Flujo Funcional Implementado

```
┌─────────────────────────────────────────────────────────────┐
│               FLUJO DE VALIDACIÓN DE DOCUMENTOS             │
├─────────────────────────────────────────────────────────────┤
│  1. POST /api/estudios|normativas (upload)                  │
│     ↓ Archivo va a bucket MinIO *staging*                   │
│                                                             │
│  2. VirusTotal scan                                         │
│     ↓ Análisis antivirus del archivo                        │
│                                                             │
│  3. ¿Archivo limpio?                                        │
│     ↓ SÍ: se promueve a bucket definitivo (copy + delete)   │
│     ↓ NO: se rechaza y se registra en auditoría             │
│                                                             │
│  4. Registro en BD (MySQL) + indexación                     │
│     ↓ Documento disponible para búsqueda y visualización    │
│                                                             │
│  5. GET /visor → kkfileview proxy                           │
│     ↓ Render en línea sin descargar el archivo              │
└─────────────────────────────────────────────────────────────┘
```

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/login` | GET/POST | Autenticación de usuarios |
| `/registro` | GET/POST | Solicitud de registro en la plataforma |
| `/api/estudios` | REST | CRUD de estudios del sector |
| `/api/normativas` | REST | CRUD de normativas |
| `/api/chat` | REST/WebSocket | Mensajería en tiempo real |
| `/api/chatbot` | REST | Chatbot de IA y estadísticas de uso |
| `/api/buscar` | GET | Búsqueda global de documentos |
| `/api/backups` | REST | Programación y ejecución de respaldos |
| `/api/auditoria` | GET | Registro de actividad del sistema |
| `/admin`, `/superadmin` | GET | Paneles de administración |

## 🧮 Regla de Negocio: Flujo de Validación de Documentos

Todo archivo subido a la plataforma pasa por un proceso de dos etapas:

```math
documento_publicado = staging(MinIO) → scan(VirusTotal) → clean ? promover : rechazar
```

**Reglas actuales:**

| Regla | Comportamiento |
|-------|----------------|
| Bucket temporal | `reciclame-staging` — los archivos viven aquí solo durante la validación |
| Promoción | `copy + delete` al bucket definitivo `reciclame-docs` |
| Reemplazo/eliminación | Purga **diferida** en MinIO (no se borra de inmediato, se programa) |
| Límite de subida | 200 MB por archivo (alineado con kkfileview) |

**Beneficio:** ningún documento malicioso llega a publicarse, y la eliminación diferida evita perder archivos ante errores de concurrencia.

## ✅ Estado del Proyecto

### Requerimientos funcionales del cliente → Implementación

| # | Requerimiento (Recíclame / GTICS) | Estado |
|---|-----------------------------------|--------|
| 1 | **Gestión de usuarios** — registro con correos corporativos validados, autenticación segura, CRUD de usuarios | ✅ Solicitudes de registro validadas por el administrador, login con correo + contraseña, recuperación de contraseña, bloqueo por intentos fallidos y login con QR |
| 2 | **Gestión del repositorio** — visualización tipo base de datos filtrable, edición de contenido (CRUD) | ✅ Vista tabular filtrable de normativas (estado, temática, nacional/internacional) + visor en línea con kkfileview |
| 3 | **Trazabilidad y monitoreo** — registro de ingresos y descargas, reportes de uso | ✅ Auditoría completa (LogsActividad), reportes PDF (JasperReports) y Excel (Apache POI) |
| 4 | **Control de accesos y roles** — niveles definidos, acceso solo a usuarios autorizados | ✅ Roles Socio / Visualizador / Administrador / SuperAdmin con configuración de vistas por rol e impersonación |
| 5 | **Gestión de documentos** — clasificación solo-lectura vs. descargable | ✅ Clasificación por tipo de acceso; documentos de dominio público descargables, estudios y normas técnicas solo lectura |

### ✔️ Funcionalidades adicionales implementadas

- [x] Validación antivirus con VirusTotal antes de publicar (staging → promoción en MinIO)
- [x] Almacenamiento de objetos con MinIO (buckets `reciclame-docs` y `reciclame-staging`)
- [x] Chat en tiempo real entre usuarios (WebSocket + JWT para Centrifugo)
- [x] Chatbot de IA con registro de feedback y estadísticas de uso
- [x] Respaldo programado de base de datos y archivos
- [x] Búsqueda global de documentos por palabras clave
- [x] Notificaciones para todos los roles
- [x] Despliegue containerizado (Dockerfile multi-stage + nginx + MySQL)

### Requerimientos no funcionales

| Requerimiento | Estado |
|---------------|--------|
| Proyecto y servicios publicados en GitHub | ✅ |
| Despliegue en proveedor de nube (Google Cloud — créditos GTICS) | ✅ |
| Servicios cloud: correo, almacenamiento de archivos, chatbot | ✅ SMTP (Spring Mail), MinIO, chatbot IA |

### 💡 Observación de Mejora

⚠️ El proyecto incluye un `docker-compose` de referencia en `docker-compose.local.yml`, pero el archivo principal de orquestación (`docker-compose.yml`) y las credenciales (`.env`) **no están versionados** por seguridad.

**Recomendación:** Para levantar el entorno completo, cada desarrollador debe crear su propio `docker-compose.yml` y `.env` a partir de `docker-compose.local.yml` y `application.properties`, completando las credenciales reales (MySQL, MinIO, VirusTotal API key, SMTP).

## 🚀 Cómo Ejecutar

### Requisitos Previos

- Java 26+ (o compatible con Spring Boot 4.0.6)
- Maven 3.9+ (incluido como wrapper)
- MySQL 8+ en `localhost:3306` con la base de datos `reciclame_db`
- MinIO en `localhost:9000` (opcional para funcionalidad de archivos)

### Configurar la Base de Datos

```sql
CREATE DATABASE reciclame_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Los scripts de esquema se encuentran en `src/main/resources/db/`.

### Ejecutar la Aplicación

**En Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**En Linux/macOS:**

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### Acceder a la Aplicación

Una vez iniciada, abre tu navegador en:

```
http://localhost:8080
```

**Puerto por defecto:** 8080

---

## 📚 Recursos Adicionales

- [Recíclame Perú](https://reciclame.org/) — sitio oficial de la asociación cliente
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Guide](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [JasperReports](https://community.jaspersoft.com/)
- Documentación completa del proyecto: `Documentacion_Proyecto_Reciclame_Ecodevs.pdf`

---

## 📄 Licencia

Este proyecto es de uso académico y educativo como parte del curso GTICS de la universidad.
