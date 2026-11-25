package com.quiroz.prestamoservice;

import tools.jackson.databind.json.JsonMapper;
import com.quiroz.prestamoservice.dto.LoginRequest;
import com.quiroz.prestamoservice.dto.PrestamoRequest;
import com.quiroz.prestamoservice.dto.RegisterRequest;
import com.quiroz.prestamoservice.repository.PrestamoRepository;
import com.quiroz.prestamoservice.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=NGYyODcyZTBmYmJlNGFlOWFlOGM5MjdhNDZmM2FlYzM3OTYzYjkyYTQwNzQzZDUxNmVlYWRmMWFiODIyNGU4Ng==",
        "jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class PrestamoApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private JsonMapper jsonMapper;

    private JsonMapper getMapper() {
        if (jsonMapper != null) {
            return jsonMapper;
        }
        return JsonMapper.builder().build();
    }

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @BeforeEach
    void cleanDatabase() {
        prestamoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private String obtenerToken(String email, String password, String nombre) throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .nombre(nombre)
                .email(email)
                .password(password)
                .build();

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return getMapper().readTree(response).get("token").asString();
    }

    @Test
    @DisplayName("Health check publico responde 200 y status UP")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("prestamoservice"));
    }

    @Test
    @DisplayName("Registro exitoso devuelve 201 y token Bearer")
    void testRegistroExitoso() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Prestamista Uno")
                .email("uno@example.com")
                .password("Clave123")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.nombre").value("Prestamista Uno"))
                .andExpect(jsonPath("$.email").value("uno@example.com"));
    }

    @Test
    @DisplayName("Registro con correo duplicado responde 409 Conflict")
    void testRegistroCorreoDuplicado() throws Exception {
        obtenerToken("duplicado@example.com", "Clave123", "User A");

        RegisterRequest request = RegisterRequest.builder()
                .nombre("User B")
                .email("duplicado@example.com")
                .password("Clave456")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Login exitoso responde 200 y credenciales invalidas responde 401")
    void testLoginYCredencialesInvalidas() throws Exception {
        obtenerToken("login@example.com", "Clave123", "User Login");

        LoginRequest loginOk = LoginRequest.builder()
                .email("login@example.com")
                .password("Clave123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(loginOk)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        LoginRequest loginFail = LoginRequest.builder()
                .email("login@example.com")
                .password("ClaveIncorrecta")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(loginFail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Acceso a endpoints protegidos sin token devuelve 401")
    void testAccesoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/prestamos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Acceso con token alterado devuelve 401")
    void testAccesoTokenAlteradoDevuelve401() throws Exception {
        mockMvc.perform(get("/prestamos")
                        .header("Authorization", "Bearer token_falso_invalido_123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Flujo completo CRUD de prestamo con calculo de totalCobrar")
    void testCrudPrestamoYCalculoTotalCobrar() throws Exception {
        String token = obtenerToken("prestamista@example.com", "Clave123", "Prestamista");

        PrestamoRequest crear = PrestamoRequest.builder()
                .cliente("Carlos Mendoza")
                .dni("12345678")
                .monto(new BigDecimal("1000.00"))
                .interes(new BigDecimal("10.00"))
                .fechaVencimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(30))
                .build();

        String postResponse = mockMvc.perform(post("/prestamos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(crear)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cliente").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.monto").value(1000.00))
                .andExpect(jsonPath("$.interes").value(10.00))
                .andExpect(jsonPath("$.totalCobrar").value(1100.00))
                .andExpect(jsonPath("$.pagado").value(false))
                .andReturn().getResponse().getContentAsString();

        long id = getMapper().readTree(postResponse).get("id").asLong();

        // Obtener por ID
        mockMvc.perform(get("/prestamos/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.totalCobrar").value(1100.00));

        // Actualizar y marcar como pagado
        PrestamoRequest actualizar = PrestamoRequest.builder()
                .cliente("Carlos Mendoza Modificado")
                .dni("12345678")
                .monto(new BigDecimal("1000.00"))
                .interes(new BigDecimal("10.00"))
                .fechaVencimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(30))
                .pagado(true)
                .build();

        mockMvc.perform(put("/prestamos/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(actualizar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Carlos Mendoza Modificado"))
                .andExpect(jsonPath("$.pagado").value(true));

        // Eliminar
        mockMvc.perform(delete("/prestamos/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verificar que ya no existe (404)
        mockMvc.perform(get("/prestamos/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Aislamiento entre prestamistas: Usuario B no puede ver ni modificar prestamo de Usuario A (404)")
    void testAislamientoEntrePrestamistas() throws Exception {
        String tokenA = obtenerToken("usuario_a@example.com", "Clave123", "Usuario A");
        String tokenB = obtenerToken("usuario_b@example.com", "Clave456", "Usuario B");

        PrestamoRequest crear = PrestamoRequest.builder()
                .cliente("Cliente de A")
                .dni("87654321")
                .monto(new BigDecimal("500.00"))
                .interes(new BigDecimal("5.00"))
                .fechaVencimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(15))
                .build();

        String response = mockMvc.perform(post("/prestamos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(crear)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long idA = getMapper().readTree(response).get("id").asLong();

        // Usuario B intenta consultar préstamo de A -> 404
        mockMvc.perform(get("/prestamos/" + idA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Usuario B intenta actualizar préstamo de A -> 404
        PrestamoRequest intentoEditar = PrestamoRequest.builder()
                .cliente("Intento Hack")
                .dni("87654321")
                .monto(new BigDecimal("500.00"))
                .interes(new BigDecimal("5.00"))
                .fechaVencimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(15))
                .pagado(true)
                .build();

        mockMvc.perform(put("/prestamos/" + idA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(intentoEditar)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Usuario B intenta eliminar préstamo de A -> 404
        mockMvc.perform(delete("/prestamos/" + idA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Lista de Usuario B debe estar vacía []
        mockMvc.perform(get("/prestamos")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Resumen de cartera calcula capitalPrestado, totalPorCobrar, gananciaEsperada y contadores")
    void testResumenCartera() throws Exception {
        String token = obtenerToken("cartera@example.com", "Clave123", "Cartera Tester");
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        // 1. Prestamo no pagado, futuro: 1000 monto, 10% interes (total 1100, ganancia 100)
        PrestamoRequest p1 = PrestamoRequest.builder()
                .cliente("Cliente 1")
                .dni("11111111")
                .monto(new BigDecimal("1000.00"))
                .interes(new BigDecimal("10.00"))
                .fechaVencimiento(hoy.plusDays(10))
                .build();
        mockMvc.perform(post("/prestamos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(getMapper().writeValueAsString(p1)))
                .andExpect(status().isCreated());

        // 2. Prestamo pagado: 2000 monto, 20% interes (total 2400, ganancia 400)
        PrestamoRequest p2 = PrestamoRequest.builder()
                .cliente("Cliente 2")
                .dni("22222222")
                .monto(new BigDecimal("2000.00"))
                .interes(new BigDecimal("20.00"))
                .fechaVencimiento(hoy.plusDays(20))
                .build();
        String p2Resp = mockMvc.perform(post("/prestamos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(getMapper().writeValueAsString(p2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long p2Id = getMapper().readTree(p2Resp).get("id").asLong();

        // Marcar p2 como pagado
        p2.setPagado(true);
        mockMvc.perform(put("/prestamos/" + p2Id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(getMapper().writeValueAsString(p2)))
                .andExpect(status().isOk());

        // Resumen esperado:
        // capitalPrestado = 1000 + 2000 = 3000.00
        // totalPorCobrar (solo no pagado p1) = 1100.00
        // gananciaEsperada = 100 + 400 = 500.00
        // cantidadPrestamos = 2
        // cantidadPendientes = 1
        // cantidadPagados = 1
        // cantidadVencidos = 0
        mockMvc.perform(get("/prestamos/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capitalPrestado").value(3000.00))
                .andExpect(jsonPath("$.totalPorCobrar").value(1100.00))
                .andExpect(jsonPath("$.gananciaEsperada").value(500.00))
                .andExpect(jsonPath("$.cantidadPrestamos").value(2))
                .andExpect(jsonPath("$.cantidadPendientes").value(1))
                .andExpect(jsonPath("$.cantidadPagados").value(1))
                .andExpect(jsonPath("$.cantidadVencidos").value(0));
    }

    @Test
    @DisplayName("Validacion: DNI debe tener 8 digitos y monto mayor a cero")
    void testValidacionesEntrada() throws Exception {
        String token = obtenerToken("valida@example.com", "Clave123", "Validador");
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        // DNI invalido
        PrestamoRequest dniInvalido = PrestamoRequest.builder()
                .cliente("Carlos")
                .dni("123")
                .monto(new BigDecimal("100.00"))
                .interes(new BigDecimal("10.00"))
                .fechaVencimiento(hoy.plusDays(5))
                .build();

        mockMvc.perform(post("/prestamos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(dniInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.dni").isNotEmpty());

        // Monto invalido
        PrestamoRequest montoInvalido = PrestamoRequest.builder()
                .cliente("Carlos")
                .dni("12345678")
                .monto(new BigDecimal("0.00"))
                .interes(new BigDecimal("10.00"))
                .fechaVencimiento(hoy.plusDays(5))
                .build();

        mockMvc.perform(post("/prestamos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getMapper().writeValueAsString(montoInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.monto").isNotEmpty());
    }

    @Test
    @DisplayName("Filtros de busqueda por cliente y estado pagado")
    void testFiltrosBusqueda() throws Exception {
        String token = obtenerToken("filtros@example.com", "Clave123", "Filtro Tester");
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        PrestamoRequest p1 = PrestamoRequest.builder()
                .cliente("Juan Perez")
                .dni("11112222")
                .monto(new BigDecimal("500.00"))
                .interes(new BigDecimal("5.00"))
                .fechaVencimiento(hoy.plusDays(10))
                .build();
        mockMvc.perform(post("/prestamos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(getMapper().writeValueAsString(p1)))
                .andExpect(status().isCreated());

        PrestamoRequest p2 = PrestamoRequest.builder()
                .cliente("Maria Gomez")
                .dni("33334444")
                .monto(new BigDecimal("800.00"))
                .interes(new BigDecimal("8.00"))
                .fechaVencimiento(hoy.plusDays(15))
                .build();
        mockMvc.perform(post("/prestamos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(getMapper().writeValueAsString(p2)))
                .andExpect(status().isCreated());

        // Buscar por texto "Juan"
        mockMvc.perform(get("/prestamos?buscar=Juan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente").value("Juan Perez"));

        // Buscar por DNI
        mockMvc.perform(get("/prestamos?buscar=33334444")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente").value("Maria Gomez"));
    }
}
