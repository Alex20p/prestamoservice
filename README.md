# PrestaControl API (prestamoservice)

API REST de gestión de cartera de préstamos personales desarrollada con Spring Boot 4 / Java 21, Spring Security, JWT y PostgreSQL en Render, diseñada para integrarse con la aplicación móvil PrestaControl (Flutter).

---

## 1. Objetivo del sistema

PrestaControl permite a los prestamistas gestionar de forma independiente su cartera de clientes y préstamos, calcular de forma automática montos a cobrar e intereses, realizar búsquedas con filtros combinados, visualizar métricas consolidadas en tiempo real y garantizar el aislamiento absoluto entre diferentes prestamistas mediante autenticación stateless basada en JSON Web Tokens (JWT).

---

## 2. Tecnologías utilizadas

- **Lenguaje:** Java 21 (Microsoft OpenJDK 21.0.13.0.2)
- **Framework:** Spring Boot 4.2.0
- **Seguridad:** Spring Security 7.2.0, BCrypt Password Encoder
- **Autenticación:** JSON Web Token (JJWT 0.13.0) con algoritmo HMAC-SHA256 (HS256)
- **Persistencia:** Spring Data JPA / Hibernate ORM
- **Base de datos:** PostgreSQL (instancia alojada en Render Oregon) / H2 Database en memoria para pruebas automatizadas
- **Gestión de dependencias y construcción:** Gradle 9.9.2 (Gradle Wrapper)
- **Contenedorización:** Docker (Multi-stage build con Eclipse Temurin 21 JDK y JRE)
- **Zona horaria de negocio:** `America/Lima` (UTC-5)

---

## 3. Arquitectura por capas

El proyecto implementa una arquitectura desacoplada por capas:

```
src/main/java/com/quiroz/prestamoservice/
├── config/                  # Configuración de Spring Security, CORS y Beans
│   └── SecurityConfig.java
├── controller/              # Controladores REST expuestos
│   ├── AuthController.java
│   ├── HealthController.java
│   └── PrestamoController.java
├── dto/                     # Data Transfer Objects para peticiones y respuestas
│   ├── AuthResponse.java
│   ├── ErrorResponse.java
│   ├── LoginRequest.java
│   ├── PrestamoRequest.java
│   ├── PrestamoResponse.java
│   ├── RegisterRequest.java
│   └── ResumenCarteraResponse.java
├── entity/                  # Entidades JPA mapeadas a la base de datos
│   ├── Prestamo.java
│   └── Usuario.java
├── exception/               # Excepciones personalizadas y manejador global
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/             # Interfaces Spring Data JPA
│   ├── PrestamoRepository.java
│   └── UsuarioRepository.java
├── security/                # Componentes de autenticación y filtros JWT
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthFilter.java
│   └── JwtService.java
├── service/                 # Interfaces de lógica de negocio
│   ├── AuthService.java
│   ├── PrestamoService.java
│   └── impl/                # Implementaciones de servicios
│       ├── AuthServiceImpl.java
│       └── PrestamoServiceImpl.java
└── PrestamoserviceApplication.java
```

### Reglas de diseño
1. Los controladores consumen exclusivamente interfaces de servicio y retornan DTOs; nunca exponen entidades JPA directamente.
2. Los servicios implementan la lógica de negocio, validaciones y reglas de aislamiento multi-tenant.
3. El usuario autenticado se extrae del contexto de seguridad de Spring (`authentication.getName()`), impidiendo la suplantación de identidad.

---

## 4. Modelo de datos

La base de datos relacional contiene dos tablas principales:

### Tabla `usuario`
| Columna   | Tipo           | Restricciones                       | Descripción                                 |
|-----------|----------------|-------------------------------------|---------------------------------------------|
| id        | BIGINT         | PK, Autoincremental                 | Identificador único del prestamista         |
| nombre    | VARCHAR(100)   | NOT NULL                            | Nombre completo                             |
| email     | VARCHAR(120)   | NOT NULL, UNIQUE                    | Correo electrónico (utilizado para login)   |
| password  | VARCHAR(100)   | NOT NULL                            | Hash encriptado con BCrypt                  |
| rol       | VARCHAR(20)    | NOT NULL                            | Rol asignado (valor por defecto: `USER`)    |

### Tabla `prestamo`
| Columna            | Tipo           | Restricciones               | Descripción                                     |
|--------------------|----------------|-----------------------------|-------------------------------------------------|
| id                 | BIGINT         | PK, Autoincremental         | Identificador único del préstamo               |
| cliente            | VARCHAR(100)   | NOT NULL                    | Nombre del prestatario                          |
| dni                | VARCHAR(8)     | NOT NULL                    | Documento Nacional de Identidad (8 dígitos)    |
| monto              | DECIMAL(10,2)  | NOT NULL                    | Capital prestado (mayor que 0)                  |
| interes            | DECIMAL(5,2)   | NOT NULL                    | Porcentaje de interés pactado                   |
| fecha_prestamo     | DATE           | NOT NULL                    | Fecha de registro (automática: America/Lima)    |
| fecha_vencimiento  | DATE           | NOT NULL                    | Fecha límite de pago pactada                    |
| pagado             | BOOLEAN        | NOT NULL, DEFAULT false     | Estado de cobro del préstamo                    |
| usuario_id         | BIGINT         | FK NOT NULL -> usuario(id)  | Propietario del préstamo (aislamiento)          |

> Nota: El campo `totalCobrar` no se almacena en la base de datos para evitar redundancia de datos. Se calcula dinámicamente en el DTO `PrestamoResponse` mediante la fórmula:
> `totalCobrar = monto + (monto * interes / 100)` con redondeo `HALF_UP` a 2 decimales.

---

## 5. Variables de entorno

La aplicación se parametriza mediante variables de entorno:

| Variable        | Valor por defecto | Descripción                                                     |
|-----------------|-------------------|-----------------------------------------------------------------|
| `PORT`          | `8080`            | Puerto HTTP del servidor (inyectado dinámicamente en Render)    |
| `DB_HOST`       | -                 | Host de la base de datos PostgreSQL                             |
| `DB_PORT`       | `5432`            | Puerto de PostgreSQL                                            |
| `DB_NAME`       | `prestacontrol`   | Nombre de la base de datos                                      |
| `DB_USER`       | `prestacontrol_user` | Usuario de PostgreSQL                                        |
| `DB_PASSWORD`   | -                 | Contraseña de PostgreSQL                                        |
| `DB_SSL_MODE`   | `require`         | Modo SSL de conexión a PostgreSQL                               |
| `JWT_SECRET`    | -                 | Clave secreta HMAC-SHA256 codificada en Base64 (>= 256 bits)   |
| `JWT_EXPIRATION`| `86400000`        | Tiempo de vida del token en milisegundos (24 horas = 86400000)  |

---

## 6. Endpoints de la API REST

### Endpoints públicos

#### `GET /health`
Verificación de estado de salud del servicio (utilizado por el Health Check de Render).
- **Respuesta 200:**
```json
{
  "status": "UP",
  "service": "prestamoservice"
}
```

#### `POST /auth/register`
Registro de un nuevo prestamista.
- **Cuerpo:**
```json
{
  "nombre": "Prestamista de prueba",
  "email": "prestamista@example.com",
  "password": "Password123"
}
```
- **Respuesta 201:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "nombre": "Prestamista de prueba",
  "email": "prestamista@example.com"
}
```
- **Error 409:** Si el correo ya está registrado.

#### `POST /auth/login`
Autenticación de prestamista.
- **Cuerpo:**
```json
{
  "email": "prestamista@example.com",
  "password": "Password123"
}
```
- **Respuesta 200:** Misma estructura que el registro.
- **Error 401:** Credenciales incorrectas.

---

### Endpoints protegidos (Requieren cabecera `Authorization: Bearer <TOKEN>`)

#### `GET /prestamos`
Listado de préstamos pertenecientes al usuario autenticado. Acepta parámetros opcionales de búsqueda y filtrado:
- `buscar`: Coincidencia parcial insensible a mayúsculas sobre cliente o DNI.
- `pagado`: `true` o `false`.
- `vencido`: `true` o `false`.
- **Respuesta 200:** Lista de objetos `PrestamoResponse` (o `[]` si no existen).

#### `GET /prestamos/{id}`
Obtiene un préstamo individual del usuario autenticado.
- **Respuesta 200:**
```json
{
  "id": 1,
  "cliente": "Carlos Mendoza",
  "dni": "12345678",
  "monto": 1000.00,
  "interes": 10.00,
  "totalCobrar": 1100.00,
  "fechaPrestamo": "2026-11-25",
  "fechaVencimiento": "2026-12-30",
  "pagado": false
}
```
- **Error 404:** Si el préstamo no existe o pertenece a otro prestamista.

#### `POST /prestamos`
Registra un nuevo préstamo para el usuario autenticado. Asigna automáticamente `fechaPrestamo` (fecha actual en Perú), `pagado = false` y el prestamista autenticado.
- **Cuerpo:**
```json
{
  "cliente": "Carlos Mendoza",
  "dni": "12345678",
  "monto": 1000.00,
  "interes": 10.00,
  "fechaVencimiento": "2026-12-30"
}
```
- **Respuesta 201:** `PrestamoResponse`.

#### `PUT /prestamos/{id}`
Actualiza los datos del préstamo o lo marca como pagado. Conserva el propietario original y la fecha original de registro.
- **Cuerpo:**
```json
{
  "cliente": "Carlos Mendoza",
  "dni": "12345678",
  "monto": 1000.00,
  "interes": 10.00,
  "fechaVencimiento": "2026-12-30",
  "pagado": true
}
```
- **Respuesta 200:** `PrestamoResponse` actualizado.
- **Error 404:** Si pertenece a otro prestamista.

#### `DELETE /prestamos/{id}`
Elimina el préstamo si pertenece al usuario autenticado.
- **Respuesta 204:** Sin contenido.
- **Error 404:** Si pertenece a otro prestamista.

#### `GET /prestamos/resumen`
Retorna métricas consolidadas de la cartera del prestamista autenticado.
- **Respuesta 200:**
```json
{
  "capitalPrestado": 5000.00,
  "totalPorCobrar": 3300.00,
  "gananciaEsperada": 850.00,
  "cantidadPrestamos": 5,
  "cantidadPendientes": 3,
  "cantidadPagados": 2,
  "cantidadVencidos": 1
}
```
Si el usuario no tiene registros, devuelve valores numéricos en cero (`0.00` y `0`), nunca nulos.

---

## 7. Autenticación y Demostración del JWT

El token JWT generado por la API utiliza:
- **Algoritmo:** HMAC-SHA256 (`HS256`).
- **Subject (`sub`):** Correo electrónico del usuario.
- **Issued At (`iat`):** Marca de tiempo de emisión en segundos.
- **Expiration (`exp`):** Marca de tiempo de expiración en segundos.

### Demostración didáctica en jwt.io (sin exponer secretos de producción)

Para comprobar el contenido y decodificación de un token:

1. Inicia sesión en la API mediante `POST /auth/login` o `POST /auth/register`.
2. Copia el valor de la clave `token` devuelta en la respuesta JSON.
3. Abre en el navegador [https://jwt.io/](https://jwt.io/).
4. Pega el token en el campo **Encoded**.
5. En el panel **Decoded -> Payload**, se observará:
```json
{
  "sub": "prestamista@example.com",
  "iat": 1795622400,
  "exp": 1795708800
}
```
6. El campo `exp` es interpretado directamente por la aplicación Flutter para alertar al prestamista antes del vencimiento de su sesión.

---

## 8. Colección de pruebas en Postman

El proyecto incluye en el directorio `postman/`:
- `PrestaControl.postman_collection.json`: 19 peticiones automatizadas con tests JavaScript de validación de status codes, cálculo de `totalCobrar`, pruebas de aislamiento (HTTP 404), token ausente o alterado (HTTP 401), email duplicado (HTTP 409) y validaciones de campo (HTTP 400).
- `PrestaControl.postman_environment.json`: Variables de entorno (`base_url`, `token`, `prestamo_id`, `token_usuario_a`, `token_usuario_b`).

### Instrucciones de importación:
1. Abrir Postman -> **Import** -> Seleccionar ambos archivos JSON de la carpeta `postman/`.
2. Seleccionar el entorno **PrestaControl - Local Environment**.
3. Ejecutar la colección completa con **Run collection**.

---

## 9. Ejecución local

### Requisitos previos:
- Java 21 instalado (`C:\Users\quiro\.jdks\ms-21.0.13.0.2` o equivalente).
- Instancia de PostgreSQL disponible.

### Pasos en PowerShell:
```powershell
$env:JAVA_HOME = "C:\Users\quiro\.jdks\ms-21.0.13.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Compilar código
.\gradlew.bat compileJava --console=plain

# Ejecutar pruebas automatizadas (utilizan H2 en memoria)
.\gradlew.bat test --console=plain

# Generar JAR de producción
.\gradlew.bat bootJar --console=plain

# Ejecutar localmente con variables de entorno
$env:DB_HOST = "dpg-xxxx.oregon-postgres.render.com"
$env:DB_PORT = "5432"
$env:DB_NAME = "prestacontrol"
$env:DB_USER = "prestacontrol_user"
$env:DB_PASSWORD = "tu_password"
$env:JWT_SECRET = "NGYyODcyZTBmYmJlNGFlOWFlOGM5MjdhNDZmM2FlYzM3OTYzYjkyYTQwNzQzZDUxNmVlYWRmMWFiODIyNGU4Ng=="
$env:PORT = "8080"

java -jar build/libs/prestamoservice-0.0.1-SNAPSHOT.jar
```

---

## 10. Despliegue mediante Docker en Render

El repositorio cuenta con `Dockerfile` multi-etapa optimizado y `.dockerignore`:
- **Build stage:** `eclipse-temurin:21-jdk-jammy`, normaliza saltos de línea de `gradlew`, compila con `bootJar -x test`.
- **Runtime stage:** `eclipse-temurin:21-jre-jammy`, usuario sin privilegios `prestauser:prestagroup`, puerto dinámico `${PORT}`.

### Pasos para desplegar en Render:
1. En Render Dashboard -> **New Web Service**.
2. Conectar el repositorio GitHub del proyecto `prestamoservice`.
3. Seleccionar **Runtime: Docker**.
4. Configurar **Health Check Path:** `/health`.
5. En la sección **Environment Variables**, registrar:
   - `DB_HOST`: Host proporcionado por la base de datos Render.
   - `DB_PORT`: `5432`
   - `DB_NAME`: `prestacontrol`
   - `DB_USER`: `prestacontrol_user`
   - `DB_PASSWORD`: Contraseña asignada en Render.
   - `DB_SSL_MODE`: `require`
   - `JWT_SECRET`: Clave Base64 de al menos 256 bits.
   - `JWT_EXPIRATION`: `86400000`
6. Render iniciará la construcción y publicará la API bajo un dominio HTTPS público, listo para ser consumido por la app móvil Flutter.
