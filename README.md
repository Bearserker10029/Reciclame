# Descripción del Proyecto — Repositorio Circular RECÍCLAME
 
Es una plataforma para adherirse a algo ya implementado del proyecto.
 
Se brindarán los requerimientos mínimos por el cliente, pero el reto del curso será el siguiente:
 
> **"Cada grupo deberá brindar una solución única de acuerdo con su entendimiento de los requerimientos del cliente"**
 
---
 
## Roles
 
- Administrador
- Socio
- Visualizador
- **SuperAdmin** *(propuesto por la PUCP)*
 
> El SuperAdmin es un usuario que no existe, pero deberá contener algo adicional y diferenciador con respecto al administrador.
 
---
 
## Requerimientos Funcionales
 
### 1. Gestión de usuarios
 
- a. Registro y acceso con correos corporativos.
- b. La solicitud de registro deberá ser validada por el administrador.
- c. Autenticación segura del usuario.
- d. CRUD completo para el administrador.
 
### 2. Gestión del repositorio
 
- a. Visualización estructurada de normativas (como base de datos filtrable).
- b. Edición del contenido (CRUD de documentos).
 
### 3. Trazabilidad y monitoreo
 
- a. Registro de actividad de usuarios (logs de ingreso y descarga de documentos).
- b. Generación de reportes de uso.
 
### 4. Control de accesos y roles
 
- a. Diferentes niveles de acceso.
- b. Restricción de acceso exclusivamente a usuarios autorizados.
 
### 5. Gestión de documentos
 
- a. Clasificación de documentos según tipos de acceso:
  - i. Lectura (visualización)
  - ii. Descargables (documentos de dominio público)
 
---
 
## Requerimientos Adicionales
 
- El acceso de la página debe ser a través de un banner de la página de la empresa o similar (lo que permita solo copiar código).
- El fondo del login deberá tener imágenes relacionadas a la empresa "Recíclame".
- Una única página de acceso para todos los roles (correo + contraseña).
- Una segunda página de **"home"** luego del login deberá presentarse con dos secciones/banners/opciones indicando:
  - Estudios: reportes y ayudas memoria
  - Repositorio Normativo
 
### Estudios Recíclame
 
El módulo de estudios deberá contener:
 
- Dashboard con listado de documentos.
- Visualización en formato lista + cuadrícula (como Google Drive).
- Filtro máximo hasta 1 año (tomar en cuenta el current date).
- Buscador con palabras clave.
- Descarga (nombre, objetivo del estudio y el formato de descarga).
- Formato: presentación (`.pptx`) o infografía (`.pdf`).
 
### Repositorio Normativo
 
El módulo normativo deberá contener:
 
- Frame de colores distintivos en temáticas por colores (tipo la SBS).
- Seccionar por **Nacional / Internacional**.
 
---
 
## Funcionalidades Generales
 
- Todos los usuarios tienen una sección de notificaciones. Con excepción del SuperAdmin, que solo tiene cambio de contraseña.
- Todos los usuarios deberán registrarse con **DNI** y el **correo corporativo**.
 
---
 
## Requerimientos No Funcionales
 
- Proyectos y servicios web publicados en **GitHub**.
- Proyecto desplegado en un **proveedor de nube**.
- El proyecto debe usar servicios cloud de un proveedor para las funcionalidades que requieran procesamiento local, tales como:
  - Envío de correo.
  - Almacenamiento de data, archivos e imágenes.
  - Balanceo de carga.
  - Chatbot y/o asistente virtual (para mejor uso del usuario).
  - Otros servicios que sean requeridos por la plataforma.