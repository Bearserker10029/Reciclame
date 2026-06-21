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
