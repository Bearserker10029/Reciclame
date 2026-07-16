package org.reciclame.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Cliente para la API de consultas DNI/RUC de apiperu.dev.
 * Doc: https://docs.apiperu.dev — POST /api/dni y /api/ruc con
 * Authorization: Bearer {token} y body {"dni"|"ruc": "..."}.
 */
@Service
public class ApiPeruDevService {

    private static final Logger log = LoggerFactory.getLogger(ApiPeruDevService.class);
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{11}$");

    private final RestTemplate restTemplate;

    @Value("${apiperudev.dni.url}")
    private String dniUrl;

    @Value("${apiperudev.ruc.url}")
    private String rucUrl;

    @Value("${apiperudev.token}")
    private String token;

    public ApiPeruDevService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Consulta el nombre completo asociado a un DNI.
     * Devuelve Optional.empty() si el DNI es inválido, no se encuentra,
     * o el token no está configurado — nunca lanza excepción hacia el controller.
     */
    public Optional<String> consultarNombrePorDni(String dni) {
        if (dni == null || !DNI_PATTERN.matcher(dni).matches()) {
            return Optional.empty();
        }
        Optional<Map<String, Object>> data = consultar(dniUrl, "dni", dni);
        return data.map(d -> {
            Object nombreCompleto = d.get("nombre_completo");
            return nombreCompleto != null ? nombreCompleto.toString().trim() : null;
        }).filter(s -> s != null && !s.isBlank());
    }

    /**
     * Consulta los datos de una empresa asociada a un RUC.
     */
    public Optional<Map<String, Object>> consultarRuc(String ruc) {
        if (ruc == null || !RUC_PATTERN.matcher(ruc).matches()) {
            return Optional.empty();
        }
        return consultar(rucUrl, "ruc", ruc);
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> consultar(String url, String campo, String valor) {
        if (token == null || token.isBlank()) {
            log.warn("[ApiPeruDev] APIPERUDEV_TOKEN no configurado; se omite la consulta.");
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of(campo, valor), headers);
            Map<String, Object> body = restTemplate.postForObject(url, entity, Map.class);

            if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                return Optional.empty();
            }
            Object data = body.get("data");
            return data instanceof Map ? Optional.of((Map<String, Object>) data) : Optional.empty();
        } catch (HttpClientErrorException e) {
            log.info("[ApiPeruDev] {}={} no encontrado o error ({}): {}", campo, valor, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("[ApiPeruDev] Error consultando {}={}: {}", campo, valor, e.getMessage());
            return Optional.empty();
        }
    }
}
