package org.reciclame.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Flag global en memoria que bloquea toda la app (salvo SUPERADMIN) mientras
 * dura una restauración de backup. Existe porque la restauración manipula
 * directamente las tablas en vivo, y dejar entrar tráfico normal durante ese
 * proceso es lo que provoca los cuelgues/errores en el resto de la app.
 */
@Service
public class MantenimientoGlobalService {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoGlobalService.class);

    private final AtomicBoolean activo = new AtomicBoolean(false);
    private volatile String motivo;

    public void activar(String motivo) {
        this.motivo = motivo;
        activo.set(true);
        log.info("Mantenimiento ACTIVADO: {}", motivo);
    }

    public void desactivar() {
        activo.set(false);
        log.info("Mantenimiento DESACTIVADO");
        this.motivo = null;
    }

    public boolean estaActivo() {
        return activo.get();
    }

    public String getMotivo() {
        return motivo;
    }
}
