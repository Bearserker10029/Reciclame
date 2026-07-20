package org.reciclame.demo.repository;

import org.reciclame.demo.model.Normativa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormativaRepository extends JpaRepository<Normativa, Long> {

    List<Normativa> findByEstadoActivoTrue();

    List<Normativa> findByEstadoActivoTrueAndEsPublicaTrue();

    List<Normativa> findByAmbito(String ambito);

    List<Normativa> findByClasificacion(String clasificacion);

    List<Normativa> findTop100ByOrderByCreatedAtDesc();

    // Registros cuyo archivo sigue en el bucket staging (estado PENDIENTE):
    // los usa el scheduler de recuperación para re-lanzar escaneos que
    // quedaron colgados (hilo async muerto, pool saturado o reinicio).
    List<Normativa> findByArchivoUrlStartingWith(String prefix);

    // Lookup directo por URL de staging para el polling de estado de subida
    // (antes recorría findAll() en memoria en cada request).
    List<Normativa> findByArchivoUrl(String archivoUrl);

    List<Normativa> findTop5ByEstadoActivoTrueAndEsPublicaTrueOrderByVisualizacionesCountDesc();

    /**
     * Búsqueda global en normativas activas y públicas.
     * Busca en título y descripción breve con LIKE case-insensitive.
     * Usado por SOCIO y VISUALIZADOR.
     */
    @Query("""
            SELECT n FROM Normativa n
            WHERE n.estadoActivo = true
              AND n.esPublica = true
              AND (LOWER(n.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(n.descripcionBreve) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY n.anio DESC, n.createdAt DESC
            """)
    List<Normativa> buscarPublico(@Param("q") String q, Pageable pageable);

    /**
     * Búsqueda global en TODAS las normativas activas (sin filtro de visibilidad).
     * Busca en título y descripción breve con LIKE case-insensitive.
     * Usado por ADMINISTRADOR y SUPERADMIN.
     */
    @Query("""
            SELECT n FROM Normativa n
            WHERE n.estadoActivo = true
              AND (LOWER(n.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(n.descripcionBreve) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY n.anio DESC, n.createdAt DESC
            """)
    List<Normativa> buscarTodos(@Param("q") String q, Pageable pageable);
}
