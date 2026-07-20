package org.reciclame.demo.repository;

import org.reciclame.demo.model.Estudio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long> {

    List<Estudio> findByEstadoActivoTrue();

    List<Estudio> findByAnio(Integer anio);

    List<Estudio> findByEsPublicoTrue();

    List<Estudio> findTop100ByOrderByCreatedAtDesc();

    // Registros cuyo archivo sigue en el bucket staging (estado PENDIENTE):
    // los usa el scheduler de recuperación para re-lanzar escaneos colgados.
    List<Estudio> findByArchivoUrlStartingWith(String prefix);

    // Lookup directo por URL de staging para el polling de estado de subida.
    List<Estudio> findByArchivoUrl(String archivoUrl);

    @Query("SELECT COUNT(e) FROM Estudio e WHERE e.createdAt >= :desde")
    long contarNuevosDesde(@Param("desde") Instant desde);

    @Query("SELECT COUNT(e) FROM Estudio e WHERE e.createdAt >= :desde AND e.createdAt < :hasta")
    long contarEntreFechas(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Búsqueda global en estudios activos y públicos.
     * Busca en título y descripción breve con LIKE case-insensitive.
     * Usado por SOCIO y VISUALIZADOR.
     */
    @Query("""
            SELECT e FROM Estudio e
            WHERE e.estadoActivo = true
              AND e.esPublico = true
              AND (LOWER(e.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.descripcionBreve) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.anio DESC, e.createdAt DESC
            """)
    List<Estudio> buscarPublico(@Param("q") String q, Pageable pageable);

    /**
     * Búsqueda global en TODOS los estudios activos (sin filtro de visibilidad).
     * Busca en título y descripción breve con LIKE case-insensitive.
     * Usado por ADMINISTRADOR y SUPERADMIN.
     */
    @Query("""
            SELECT e FROM Estudio e
            WHERE e.estadoActivo = true
              AND (LOWER(e.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.descripcionBreve) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.anio DESC, e.createdAt DESC
            """)
    List<Estudio> buscarTodos(@Param("q") String q, Pageable pageable);
}
