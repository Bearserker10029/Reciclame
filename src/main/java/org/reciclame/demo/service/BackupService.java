package org.reciclame.demo.service;

import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.ProgramacionBackup;
import org.reciclame.demo.model.SolicitudesRespaldo;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.BackupRepository;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.repository.ProgramacionBackupRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class BackupService {

    /*  
        @@%***************#*********************************************####%%######%%%%%%%%%%#%%%@@@@@@@@@@@@@@@
        @@#+-------===++=====-:::::::--======-===========----===+++++++++++++++++++++++++++++++++++++++++++++*@@@
        @@#+.                                                                                              -+*@@@
        @@#+.                                                                                              :+*%@@
        @@*+.                                                                                              -+*%@@
        @@*+.                                                                                              -+*%@@
        @@#+.                                                                                              :++%@@
        @@#+.                                                                                              .++#@@
        @@#+:                  :--.                   :--                    :-:                           .++#@@
        @@*+:                .#@@@%.                .%@@@%.                .%@@@%.                         .++#@@
        @@#+-                .#@@@%:                .%@@@%.                .%@@@%.                         .++#@@
        @@*+:                  :=-.                   -=-                    -=-                           :+*%@@
        @@*+:                                                                              .::.            :+*%@@
        @@*+:                                                                            :%#=+%#.          :+*%@@
        @%*+.                                                                            *#.  .%=          -+*@@@
        @@*+:                                                                            -@+::*%:          :+*%@@
        @@*+-                                      .:-=++*####%###**++=-:.                .=++-.           .++%@@
        @@*+-                              :=+#%%@@@%%##################%@@%#+-.                           .++#@@
        @%*+:                          -#@@@%#*******************************#%@@%-                        .++#@@
        @%*+:                     .:+%@%#***************************************+*%@%+:                    .++#@@
        @%*+:                  .=#%#***********************************************++*%%*:                 :++%@@
        @@*+-                =%%*+****************************************************+=*%%+.              :++#@@
        @@*+=              =@#=+*********************************************************==#@#.            :++#@@
        @@*+=            =@#=*************************************************************+==*@*.          :++#@@
        @@*+=          :##=+****************************************************************+==#%=         :++#@@
        @@*+-         -%+=*********#**********##%%%%%%%%%%%%%%%%%%%%%###**********************+=+%#.       :++#@@
        @@*+=        *@++*********#*****#%@%%###########################%%@%#******#%***********==#@:      :++#@@
        @@*++      .*%=+**********##%%%###########%%%%%%%%%%%%%%%%%%##########%%%##**%***********==+%=     .++#@@
        @@#++     .*%=**********#%%#####%%%%%%###*******************###%%%%%%#####%%%##***********==+%=    :++#@@
        @@*++     =%+**********#%##%%%##*************************************##%%%####%%***********==*%-   .++#@@
        @@*++    -%++**********@%##*********************##*****************#******#%@%#%#***********==#@.   ++*@@
        @@*++   .%#+*%#*******##************#**********#%#*********#%******#@*********#%%********++===+@#.  ++*@@
        @@*+=   +%=**%*******#%*********++*#%************#*********%*#******%%********%*#*=============#@-  =+*@@
        @@*+=  :%++*#%*******##***********#=#=========*=++========*+-*+=====***======-*+=#=========+====@#. =+*@@
        @@*++  +%=**##*******************#*-%******++*+:+========+*:-++=====+*++======*+-*+========**+=-#%: ++*@@
        @@*+= .#*+**%#******##**##*******#--%*******#+..+#********:..:*++++++#-*++++**##*#%********#%*+=+@= =+*@@
        @@*++ -%+**#%*******##**%#******%-.=#******#+...+#******#-....-#*****#--#*****#%**%#********%**==@+.-+*%@
        @@*+= -#+**#%*******%#*%+##****#*..+#*****#+:..:*#*****#=......-%****#-.+#****#%#*#%********%**+=%*.:+*%@
        @@*+= =#+**##******#%*%+-*#****%...*#****#*....:******%=........=#***#-..%****#+#*#@********%#*+=%*.:+*@@
        @@*+-.+#+#*%#******#%%+::+####%*===#%###%#:....:*****#:.......:::+####+--*%###%+*%#@#*******%#*+=%*.-+*@@
        @@*+-.+#+#*%#******#@*:..-#***#:..:#***#+......-#**##:............+#*#=...*#**#::+%@#*******%#*+=%*.-+*@@
        @@#+=.+#+**%#******#*:...:*#*#*...:#**#=.......=#*#*:..............+%#+...-#*#*...*@#*******%#*==@+.-+*@@
        @@#++.+#+*#%#******#*:....:%*#=..:*%*%*==--:...+#%+...........::---:-%+...:+#%-....%#*******%#*=+@= .+*%@
        @@#+= =#=*#%#******#*::%@@@@@@@%%%%@@*+=::....:##:............::.....:----=*@%=-::.+#******#@#+=#%: .+*@@
        @@*+= -%++#%#******#*:...:#--*%###%%%%%@@%=...-+:...........-%@@@@@@@@@@@@%%%@@%*=.=#******#%*==@*. .+*@@
        @@*+- :#*=#%#******#*:..  .  :**********%-.....................--.:#**********@+ ..+#******%%*=#@-  .+*@@
        @%*+:  *%#=*#******##:..  **+#******##**%-......................  .#%#********#+ ..+#******%@*+@+   :+*@@
        @%*+:  =@-=%#*******#-.. .#%===*****%+=*@-.....................+@%*=+******++##= ..*#******@--%%.   .+*@@
        @%*+.  +%:-*#*******#=....#+====+***====#-.....................=%====+*****===*-...##*****#%+=*%    .+*@@
        @%*+.  +@--+#*******#+....*@###**##****##......................:@#===========+*...:#******%*--*#     +*@@
        @%*+:  .#%:*%*******#+.....::...:.:::..:........................:=+*#######%%+....:%******%#-:#+     +*@@
        @@*+-   =@%-*#******#*:...........................................................=#*****#%=:*@+     -*%@
        @@*+-   .@**%%******##:::::.:::.::::.:::::....................::.:.::...:.::::.:..*#*****%++#+@*     -*%@
        @@*+-    ##+#%#******#:....:::.::...................::..........::.:::::..:::..:..##****#%#*++%%     =*@@
        @@*+-    ##+*#%******%-.............................::...........................:%*****%#**+=%%     =*@@
        @@*++++++++++***++++++*=++++=====++++++==========---::::--:-:-----::------:::::::*******#***++*#-::-:=*@@
        @@%%%%@@@@%%%%%##########%%##****#####*****************************+************+***************+*****#@@
    */



    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final BackupRepository backupRepository;
    private final ProgramacionBackupRepository programacionRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final MantenimientoGlobalService mantenimientoService;
    private final MinioPendingDeletionService minioPendingDeletionService;
    private final MinioStorageService minioStorageService;
    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${app.backup.path:/backups}")
    private String backupPath;

    public BackupService(BackupRepository backupRepository,
                         ProgramacionBackupRepository programacionRepository,
                         AuditoriaService auditoriaService,
                         UsuarioRepository usuarioRepository,
                         NotificacionService notificacionService,
                         MantenimientoGlobalService mantenimientoService,
                         MinioPendingDeletionService minioPendingDeletionService,
                         MinioStorageService minioStorageService,
                         EstudioRepository estudioRepository,
                         NormativaRepository normativaRepository) {
        this.backupRepository = backupRepository;
        this.programacionRepository = programacionRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.mantenimientoService = mantenimientoService;
        this.minioPendingDeletionService = minioPendingDeletionService;
        this.minioStorageService = minioStorageService;
        this.estudioRepository = estudioRepository;
        this.normativaRepository = normativaRepository;
    }

    @Transactional(readOnly = true)
    public List<SolicitudesRespaldo> findAll() {
        return backupRepository.findByEstadoActivoTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SolicitudesRespaldo> findByEstado(String estado) {
        return backupRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<SolicitudesRespaldo> findCompletados() {
        return backupRepository.findCompletados();
    }

    @Transactional(readOnly = true)
    public List<SolicitudesRespaldo> findFallidos() {
        return backupRepository.findFallidos();
    }

    @Transactional(readOnly = true)
    public List<SolicitudesRespaldo> findEnProceso() {
        return backupRepository.findEnProceso();
    }

    @Transactional(readOnly = true)
    public List<ProgramacionBackup> findAllProgramaciones() {
        return programacionRepository.findByEstadoActivoTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ProgramacionBackup> findProgramacionesActivas() {
        return programacionRepository.findProgramacionesActivas();
    }

    @Transactional(readOnly = true)
    public SolicitudesRespaldo findById(Long id) {
        return backupRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public ProgramacionBackup findProgramacionById(Long id) {
        return programacionRepository.findById(id).orElse(null);
    }

    public BackupStats getStats() {
        return new BackupStats(
                backupRepository.countTotal(),
                backupRepository.countCompletados(),
                backupRepository.countEnProceso(),
                backupRepository.countFallidos(),
                backupRepository.findUltimoCompletado()
        );
    }

    @Transactional
    public SolicitudesRespaldo crearBackupManual(Usuario solicitante, String tipo, String notas) {
        SolicitudesRespaldo backup = new SolicitudesRespaldo();
        backup.setSolicitadoPor(solicitante);
        backup.setTipo(tipo != null ? tipo : "COMPLETO");
        backup.setEstado("PENDIENTE");
        backup.setNotas(notas);
        backup.setEsBackupPreRestauracion(false);
        SolicitudesRespaldo saved = backupRepository.save(backup);

        // Auditoría: quién pidió el backup y de qué tipo. Notas como contexto.
        Long usuarioId = solicitante != null ? solicitante.getId() : null;
        String usuarioNombre = solicitante != null ? solicitante.getNombreCompleto() : "Sistema";
        String descripcion = "Solicitar backup tipo=" + saved.getTipo()
                + (notas != null && !notas.isBlank() ? " — notas: " + notas : "");
        auditoriaService.registrarAccion(usuarioId, usuarioNombre,
                AuditoriaService.ACCION_CREAR_BACKUP, AuditoriaService.MODULO_BACKUPS,
                descripcion);

        return saved;
    }

    @Async
    public CompletableFuture<Void> ejecutarBackupAsync(SolicitudesRespaldo backup, String ipAddress, String userAgent) {
        try {
            ejecutarBackup(backup, ipAddress, userAgent);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error ejecutando backup {}: {}", backup.getId(), e.getMessage(), e);
            marcarFallo(backup.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void ejecutarBackup(SolicitudesRespaldo backup) {
        ejecutarBackup(backup, null, null);
    }

    @Transactional
    public void ejecutarBackup(SolicitudesRespaldo backup, String ipAddress, String userAgent) {
        backup.setEstado("EN_PROCESO");
        backupRepository.save(backup);

        try {
            String dbName = extraerDbName(datasourceUrl);
            String filename = generarNombreArchivo(backup.getTipo());
            Path archivoPath = Paths.get(backupPath, filename);

            Files.createDirectories(Paths.get(backupPath));

            ProcessBuilder pb = new ProcessBuilder();
            pb.command("mysqldump",
                    "-h", extraerHost(datasourceUrl),
                    "-P", extraerPuerto(datasourceUrl),
                    "-u", datasourceUsername,
                    "-p" + datasourcePassword,
                    "--ssl=0",
                    "--no-create-db",
                    "--single-transaction",
                    "--quick",
                    "--lock-tables=false",
                    dbName);

            Process process = pb.start();

            try (FileOutputStream fos = new FileOutputStream(archivoPath.toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Deprecated program name")) {
                        continue;
                    }
                    bos.write(line.getBytes());
                    bos.write('\n');
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("mysqldump exit code: {}. Verificando si el archivo se creó correctamente...", exitCode);
            }

            long tamanhoBytes = Files.size(archivoPath);
            if (tamanhoBytes == 0) {
                throw new RuntimeException("mysqldump falló: archivo generado está vacío (exit code: " + exitCode + ")");
            }

            backup.setStorageKey(filename);
            backup.setTamanioBytes(tamanhoBytes);
            backup.setEstado("COMPLETADO");
            backup.setFechaCompletado(Instant.now());
            backupRepository.save(backup);

            log.info("Backup {} completado: {} ({} bytes, exit code: {})", backup.getId(), filename, tamanhoBytes, exitCode);

            // Auditoría: backup ejecutado OK. Atribuimos al usuario que lo solicitó
            // (en el caso de los programados, es el superAdmin que figure como solicitante;
            // si fue un reintento, sigue siendo el solicitante original).
            registrarAuditoriaEjecucion(backup, "EXITOSO", null, ipAddress, userAgent);

        } catch (Exception e) {
            backup.setEstado("FALLIDO");
            backupRepository.save(backup);
            log.error("Backup {} falló: {}", backup.getId(), e.getMessage(), e);

            // Auditoría: backup falló. El error.message queda en mensajeError para diagnóstico.
            registrarAuditoriaEjecucion(backup, "FALLIDO", e.getMessage(), ipAddress, userAgent);

            throw new RuntimeException("Backup falló: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void marcarFallo(Long backupId, String mensajeError) {
        backupRepository.findById(backupId).ifPresent(backup -> {
            backup.setEstado("FALLIDO");
            backupRepository.save(backup);
        });
    }

    @Transactional
    public void reintentarBackup(Long backupId) {
        SolicitudesRespaldo backup = backupRepository.findById(backupId).orElse(null);
        if (backup != null && "FALLIDO".equals(backup.getEstado())) {
            backup.setEstado("PENDIENTE");
            backupRepository.save(backup);

            // Auditoría: quién pidió el reintento. Si tenemos un usuario contexto
            // (vía controller), lo ideal sería pasarlo, pero la firma actual no lo
            // recibe, así que atribuimos al solicitante original del backup.
            Usuario solicitante = backup.getSolicitadoPor();
            Long usuarioId = solicitante != null ? solicitante.getId() : null;
            String usuarioNombre = solicitante != null ? solicitante.getNombreCompleto() : "Sistema";
            auditoriaService.registrarAccion(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_REINTENTAR_BACKUP, AuditoriaService.MODULO_BACKUPS,
                    "Reintento de backup ID: " + backupId + " (tipo=" + backup.getTipo() + ")");

            ejecutarBackupAsync(backup, null, null);
        }
    }

    @Async
    public CompletableFuture<Void> restaurarBackupAsync(Long backupId, Usuario usuario, boolean crearBackupSeguridad,
                                                        String ipAddress, String userAgent) {
        mantenimientoService.activar("Restauración de backup en curso");
        try {
            restaurarBackup(backupId, usuario, crearBackupSeguridad, ipAddress, userAgent);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error restaurando backup {}: {}", backupId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        } finally {
            mantenimientoService.desactivar();
        }
    }

    // Nota: sin @Transactional a propósito. La restauración ejecuta DDL/DML
    // crudo vía JDBC directo (ejecutarSqlViaJdbc) sobre una conexión aparte
    // del pool de Hibernate; envolverla en una transacción de Spring solo
    // mantenía una conexión del pool abierta e inútil durante todo el
    // proceso (varios minutos), sin ningún beneficio de atomicidad real.
    public void restaurarBackup(Long backupId, Usuario usuario, boolean crearBackupSeguridad) {
        restaurarBackup(backupId, usuario, crearBackupSeguridad, null, null);
    }

    public void restaurarBackup(Long backupId, Usuario usuario, boolean crearBackupSeguridad,
                                 String ipAddress, String userAgent) {
        SolicitudesRespaldo backup = backupRepository.findById(backupId).orElse(null);
        if (backup == null) {
            throw new RuntimeException("Backup no encontrado");
        }
        if (!"COMPLETADO".equals(backup.getEstado())) {
            throw new RuntimeException("Solo se pueden restaurar backups completados");
        }

        if (crearBackupSeguridad) {
            crearBackupPreRestauracion(usuario);
        }

        try {
            Path archivoPath = Paths.get(backupPath, backup.getStorageKey());
            if (!Files.exists(archivoPath)) {
                throw new RuntimeException("Archivo de backup no encontrado: " + backup.getStorageKey());
            }

            backup.setEstado("EN_PROCESO");
            backupRepository.save(backup);

            String jdbcUrl = buildJdbcUrl();

            log.info("Iniciando restauración via JDBC: db={}, archivo={}", extraerDbName(datasourceUrl), backup.getStorageKey());

            ejecutarSqlViaJdbc(jdbcUrl, archivoPath, 300);

            int purgasCanceladas = cancelarPurgasDeDocsActivos();
            log.info("Restauración: {} purgas de MinIO canceladas por documentos activos tras el restore", purgasCanceladas);

            backup.setRestauradoPor(usuario);
            backup.setFechaRestauracion(Instant.now());
            backup.setEstado("COMPLETADO");
            backupRepository.save(backup);

            String descripcionRestore = "Restauración desde backup ID: " + backupId
                    + " (" + backup.getStorageKey() + ")"
                    + (purgasCanceladas > 0 ? " — " + purgasCanceladas + " purga(s) de MinIO cancelada(s)" : "");
            registrarAuditoria(usuario, AuditoriaService.ACCION_RESTAURO_BACKUP,
                    AuditoriaService.MODULO_BACKUPS,
                    descripcionRestore, ipAddress, userAgent);

            log.info("Restauración completada desde backup {} por usuario {}", backupId, usuario.getNombreCompleto());

            List<Usuario> superAdmins = usuarioRepository.findByRolNombre("SUPERADMIN");
            for (Usuario sa : superAdmins) {
                notificacionService.crearNotificacion(
                        sa.getId(),
                        "Restauración de base de datos completada",
                        "El usuario " + usuario.getNombreCompleto() + " restauró la base de datos desde el backup \"" + backup.getStorageKey() + "\". La restauración se completó exitosamente.",
                        "SISTEMA"
                );
            }
            log.info("Notificaciones enviadas a {} superadmins sobre la restauración", superAdmins.size());

        } catch (Exception e) {
            backup.setEstado("FALLIDO");
            backupRepository.save(backup);
            log.error("Restauración {} falló: {}", backupId, e.getMessage(), e);
            throw new RuntimeException("Restauración falló: " + e.getMessage(), e);
        }
    }

    private String buildJdbcUrl() {
        String host = extraerHost(datasourceUrl);
        String port = extraerPuerto(datasourceUrl);
        String dbName = extraerDbName(datasourceUrl);
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true";
    }

    /**
     * Cancela las purgas pendientes de MinIO para cualquier documento (estudio o
     * normativa) que tras el restore quedó con {@code estadoActivo=true}. Sin
     * esto, el {@code MinioCleanupScheduler} procesaría la fila resurrecta y
     * borraría físicamente el archivo aunque el admin acaba de restaurar el
     * documento, dejándolo con URL rota.
     * <p>
     * Idempotente: si no hay archivoUrl, si la URL no es de MinIO, o si no hay
     * una purga pendiente, simplemente no hace nada para ese documento.
     * </p>
     *
     * @return cantidad de purgas efectivamente canceladas
     */
    private int cancelarPurgasDeDocsActivos() {
        int cancelados = 0;
        try {
            for (Estudio e : estudioRepository.findByEstadoActivoTrue()) {
                if (e.getArchivoUrl() != null && !e.getArchivoUrl().isBlank()) {
                    try {
                        String objectName = minioStorageService.getObjectNameFromUrl(e.getArchivoUrl());
                        if (minioPendingDeletionService.cancelarEliminacion(objectName)) {
                            cancelados++;
                            log.debug("Restore canceló purga pendiente de estudio {} (objectName={})", e.getId(), objectName);
                        }
                    } catch (Exception ex) {
                        log.warn("Restore no pudo evaluar purga de estudio {}: {}", e.getId(), ex.getMessage());
                    }
                }
            }
            for (Normativa n : normativaRepository.findByEstadoActivoTrue()) {
                if (n.getArchivoUrl() != null && !n.getArchivoUrl().isBlank()) {
                    try {
                        String objectName = minioStorageService.getObjectNameFromUrl(n.getArchivoUrl());
                        if (minioPendingDeletionService.cancelarEliminacion(objectName)) {
                            cancelados++;
                            log.debug("Restore canceló purga pendiente de normativa {} (objectName={})", n.getId(), objectName);
                        }
                    } catch (Exception ex) {
                        log.warn("Restore no pudo evaluar purga de normativa {}: {}", n.getId(), ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cancelando purgas post-restore (continúa de todas formas): {}", e.getMessage(), e);
        }
        return cancelados;
    }

    private void ejecutarSqlViaJdbc(String jdbcUrl, Path archivoPath, int timeoutSeconds) throws Exception {
        List<String> sentencias = parsearSql(archivoPath);
        log.info("Archivo SQL parseado: {} sentencias a ejecutar", sentencias.size());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, datasourceUsername, datasourcePassword);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

                int count = 0;
                for (String sql : sentencias) {
                    String trimmed = sql.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                        continue;
                    }
                    try {
                        stmt.execute(trimmed);
                        count++;
                    } catch (SQLException e) {
                        log.warn("Error ejecutando sentencia (continúa): {} - {}", e.getMessage(),
                                trimmed.length() > 100 ? trimmed.substring(0, 100) + "..." : trimmed);
                    }
                }

                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                log.info("Restauración JDBC completada: {} sentencias ejecutadas", count);
            } catch (SQLException e) {
                throw new RuntimeException("Error de conexión JDBC: " + e.getMessage(), e);
            }
        });

        executor.shutdown();
        if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            throw new RuntimeException("Restauración excedió el timeout de " + timeoutSeconds + " segundos");
        }

        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Error ejecutando SQL: " + cause.getMessage(), cause);
        }
    }

    private List<String> parsearSql(Path archivoPath) throws IOException {
        List<String> crudas = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        // Paso 1: partir el archivo en sentencias completas (respetando strings
        // y saltando LOCK/UNLOCK TABLES, que toman locks de tabla en vivo y
        // bloquean cualquier otra conexión del pool mientras dura la restauración,
        // innecesario en una restauración secuencial de una sola conexión).
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(archivoPath.toFile())))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*") || line.startsWith("mysqldump:") || line.startsWith("mariadb-dump:")) {
                    continue;
                }

                String upperLine = line.toUpperCase();
                if (upperLine.startsWith("LOCK TABLES") || upperLine.startsWith("UNLOCK TABLES")) {
                    continue;
                }

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);

                    if (inString) {
                        current.append(c);
                        if (c == stringChar && i > 0 && line.charAt(i - 1) != '\\') {
                            inString = false;
                        }
                    } else {
                        if (c == '\'' || c == '"') {
                            inString = true;
                            stringChar = c;
                            current.append(c);
                        } else if (c == ';') {
                            String stmt = current.toString().trim();
                            if (!stmt.isEmpty()) {
                                crudas.add(stmt);
                            }
                            current = new StringBuilder();
                        } else {
                            current.append(c);
                        }
                    }
                }

                if (!inString && current.length() > 0) {
                    current.append(" ");
                }
            }
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            crudas.add(remaining);
        }

        // Paso 2: descartar sentencias completas que toquen solicitudes_respaldo
        // (nunca se recrea/trunca esa tabla: es donde vive el propio historial
        // de backups/restauraciones). Se filtra sobre la sentencia ya armada,
        // no línea por línea, para no partir un CREATE TABLE multilínea a la
        // mitad (eso generaba fragmentos de columnas sueltos con error de
        // sintaxis, ver historial de este método).
        List<String> sentencias = new ArrayList<>();
        for (String stmt : crudas) {
            String upperStmt = stmt.toUpperCase();
            boolean esSolicitudesRespaldo = upperStmt.contains("SOLICITUDES_RESPALDO")
                    && (upperStmt.startsWith("DROP TABLE") || upperStmt.startsWith("CREATE TABLE") || upperStmt.startsWith("INSERT INTO"));
            if (esSolicitudesRespaldo) {
                continue;
            }
            sentencias.add(stmt);
        }

        return sentencias;
    }

    @Transactional
    public void crearBackupPreRestauracion(Usuario usuario) {
        SolicitudesRespaldo preBackup = new SolicitudesRespaldo();
        preBackup.setSolicitadoPor(usuario);
        preBackup.setTipo("COMPLETO");
        preBackup.setEstado("PENDIENTE");
        preBackup.setNotas("Backup de seguridad automático previo a restauración");
        preBackup.setEsBackupPreRestauracion(true);
        preBackup = backupRepository.save(preBackup);

        try {
            ejecutarBackup(preBackup);
        } catch (Exception e) {
            log.warn("No se pudo crear backup pre-restauración: {}", e.getMessage());
        }
    }

    @Transactional
    public ProgramacionBackup crearProgramacion(ProgramacionBackup programacion) {
        return programacionRepository.save(programacion);
    }

    @Transactional
    public ProgramacionBackup actualizarProgramacion(Long id, ProgramacionBackup programacion) {
        ProgramacionBackup existente = programacionRepository.findById(id).orElse(null);
        if (existente == null) {
            throw new RuntimeException("Programación no encontrada");
        }
        existente.setTipo(programacion.getTipo());
        existente.setFrecuencia(programacion.getFrecuencia());
        existente.setHoraEjecucion(programacion.getHoraEjecucion());
        existente.setDiaSemana(programacion.getDiaSemana());
        existente.setDiaMes(programacion.getDiaMes());
        existente.setHabilitado(programacion.getHabilitado());
        return programacionRepository.save(existente);
    }

    @Transactional
    public void eliminarProgramacion(Long id) {
        programacionRepository.findById(id).ifPresent(p -> {
            p.setEstadoActivo(false);
            programacionRepository.save(p);
        });
    }

    @Transactional
    public ProgramacionBackup toggleProgramacion(Long id) {
        ProgramacionBackup programacion = programacionRepository.findById(id).orElse(null);
        if (programacion != null && "EMERGENCIA".equalsIgnoreCase(programacion.getTipo())) {
            throw new IllegalStateException(
                    "La programación de backup de emergencia es obligatoria y no se puede desactivar. "
                  + "Si necesitás un mantenimiento programado, eliminá la programación y recreala después.");
        }
        if (programacion != null) {
            programacion.setHabilitado(!programacion.getHabilitado());
            return programacionRepository.save(programacion);
        }
        return null;
    }

    @Transactional
    public void eliminarBackup(Long id) {
        backupRepository.findById(id).ifPresent(backup -> {
            if (backup.getStorageKey() != null) {
                Path archivoPath = Paths.get(backupPath, backup.getStorageKey());
                try {
                    Files.deleteIfExists(archivoPath);
                } catch (IOException e) {
                    log.warn("No se pudo eliminar archivo de backup: {}", archivoPath);
                }
            }
            backup.setEstadoActivo(false);
            backupRepository.save(backup);

            // Auditoría: quién eliminó el backup. Como la firma del método no recibe
            // usuario, atribuimos al solicitante original del backup (o "Sistema" si
            // no hay). Si en el futuro el controller pasa el usuario actual, se puede
            // refactorizar para tener un campo 'eliminadoPor' en la entidad.
            Usuario solicitante = backup.getSolicitadoPor();
            Long usuarioId = solicitante != null ? solicitante.getId() : null;
            String usuarioNombre = solicitante != null ? solicitante.getNombreCompleto() : "Sistema";
            String descripcion = "Eliminar backup ID: " + id
                    + " (storageKey=" + backup.getStorageKey()
                    + ", tipo=" + backup.getTipo() + ")";
            auditoriaService.registrarAccion(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_ELIMINAR_BACKUP, AuditoriaService.MODULO_BACKUPS,
                    descripcion);
        });
    }

    /**
     * Busca el backup de emergencia anterior (el más reciente con
     * {@code storage_key=backup_emergencia.sql} y {@code estado_activo=true}) y lo
     * elimina: borra el archivo del disco y marca la fila como inactiva. La fila
     * queda en BD para auditoría.
     * <p>
     * Se usa antes de crear un nuevo backup de emergencia: por política solo existe
     * UN archivo de emergencia a la vez, el nuevo sobreescribe al anterior.
     * </p>
     *
     * @return id del backup anterior eliminado, o null si no había
     */
    @Transactional
    public Long eliminarBackupEmergenciaAnterior() {
        SolicitudesRespaldo anterior = backupRepository
                .findFirstByStorageKeyAndEstadoActivoOrderByIdDesc("backup_emergencia.sql", true);
        if (anterior == null) {
            return null;
        }
        try {
            Path archivoPath = Paths.get(backupPath, anterior.getStorageKey());
            Files.deleteIfExists(archivoPath);
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo de backup emergencia anterior: {}", e.getMessage());
        }
        anterior.setEstadoActivo(false);
        backupRepository.save(anterior);
        log.info("Backup de emergencia anterior (ID={}) reemplazado por uno nuevo", anterior.getId());
        return anterior.getId();
    }

    public boolean esHoraDeEjecutar(ProgramacionBackup programacion) {
        LocalTime ahora = LocalTime.now(ZoneId.of("America/Lima"));
        LocalTime horaProgramada = programacion.getHoraEjecucion();

        if (!programacion.getHabilitado()) {
            return false;
        }

        long diffMinutos = Math.abs(java.time.Duration.between(ahora, horaProgramada).toMinutes());
        return diffMinutos < 1;
    }

    private String extraerHost(String url) {
        if (url == null) return "localhost";
        String cleanUrl = url;
        int paramIndex = cleanUrl.indexOf("?");
        if (paramIndex != -1) {
            cleanUrl = cleanUrl.substring(0, paramIndex);
        }
        int protocolEnd = cleanUrl.indexOf("//");
        if (protocolEnd == -1) return "localhost";
        String afterProtocol = cleanUrl.substring(protocolEnd + 2);
        int portIndex = afterProtocol.indexOf(":");
        int dbIndex = afterProtocol.indexOf("/");
        int endIndex = afterProtocol.length();
        if (portIndex != -1 && portIndex < endIndex) endIndex = portIndex;
        if (dbIndex != -1 && dbIndex < endIndex) endIndex = dbIndex;
        return afterProtocol.substring(0, endIndex);
    }

    private String extraerPuerto(String url) {
        if (url == null) return "3306";
        String cleanUrl = url;
        int paramIndex = cleanUrl.indexOf("?");
        if (paramIndex != -1) {
            cleanUrl = cleanUrl.substring(0, paramIndex);
        }
        int protocolEnd = cleanUrl.indexOf("//");
        if (protocolEnd == -1) return "3306";
        String afterProtocol = cleanUrl.substring(protocolEnd + 2);
        int portIndex = afterProtocol.indexOf(":");
        if (portIndex == -1) return "3306";
        String afterPort = afterProtocol.substring(portIndex + 1);
        int slashIndex = afterPort.indexOf("/");
        if (slashIndex != -1) {
            afterPort = afterPort.substring(0, slashIndex);
        }
        return afterPort;
    }

    private String extraerDbName(String url) {
        if (url == null) return "reciclame_db";
        String cleanUrl = url;
        int paramIndex = cleanUrl.indexOf("?");
        if (paramIndex != -1) {
            cleanUrl = cleanUrl.substring(0, paramIndex);
        }
        int dbIndex = cleanUrl.lastIndexOf("/");
        if (dbIndex == -1) return "reciclame_db";
        String dbName = cleanUrl.substring(dbIndex + 1);
        return dbName.isEmpty() ? "reciclame_db" : dbName;
    }

    /**
     * Nombre del archivo .sql en disco.
     * <p>
     * Para tipo {@code EMERGENCIA} se usa un nombre fijo ({@code backup_emergencia.sql})
     * porque por política solo existe un backup de emergencia a la vez: el nuevo
     * sobreescribe al anterior. Para los demás tipos se usa un nombre con timestamp
     * (un archivo por corrida, conserva historial).
     * </p>
     */
    private String generarNombreArchivo(String tipo) {
        if ("EMERGENCIA".equalsIgnoreCase(tipo)) {
            return "backup_emergencia.sql";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = java.time.LocalDateTime.now(ZoneId.of("America/Lima")).format(formatter);
        return "backup_" + tipo.toLowerCase() + "_" + timestamp + ".sql";
    }

    /**
     * Audita el resultado de ejecutar un backup (manual, programado o por reintento).
     * Centralizado en {@link AuditoriaService} para que entre al hash chain con el
     * resto de la auditoría del sistema.
     */
    private void registrarAuditoriaEjecucion(SolicitudesRespaldo backup, String estado, String mensajeError) {
        registrarAuditoriaEjecucion(backup, estado, mensajeError, null, null);
    }

    private void registrarAuditoriaEjecucion(SolicitudesRespaldo backup, String estado, String mensajeError,
                                            String ipAddress, String userAgent) {
        Usuario solicitante = backup.getSolicitadoPor();
        Long usuarioId = solicitante != null ? solicitante.getId() : null;
        String usuarioNombre = solicitante != null ? solicitante.getNombreCompleto() : "Sistema";
        String descripcion = "Ejecutar backup ID: " + backup.getId()
                + " (tipo=" + backup.getTipo()
                + ", tamaño=" + (backup.getTamanioBytes() != null ? backup.getTamanioBytes() + " bytes" : "n/a")
                + ", storageKey=" + backup.getStorageKey() + ")";
        if ("FALLIDO".equals(estado)) {
            auditoriaService.registrarAccionFallida(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_EJECUTAR_BACKUP, AuditoriaService.MODULO_BACKUPS,
                    descripcion, mensajeError, ipAddress, userAgent);
        } else {
            auditoriaService.registrarAccion(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_EJECUTAR_BACKUP, AuditoriaService.MODULO_BACKUPS,
                    descripcion, null, null, "EXITOSO", null, ipAddress, userAgent);
        }
    }

    /**
     * Wrapper de compatibilidad para la única llamada histórica (restaurarBackup).
     * Mantiene la firma para no tocar el call site; delega en {@link AuditoriaService}.
     */
    private void registrarAuditoria(Usuario usuario, String accion, String modulo, String descripcion) {
        registrarAuditoria(usuario, accion, modulo, descripcion, null, null);
    }

    private void registrarAuditoria(Usuario usuario, String accion, String modulo, String descripcion,
                                    String ipAddress, String userAgent) {
        try {
            Long usuarioId = usuario != null ? usuario.getId() : null;
            String usuarioNombre = usuario != null ? usuario.getNombreCompleto() : "Sistema";
            auditoriaService.registrarAccion(usuarioId, usuarioNombre, accion, modulo, descripcion,
                    null, null, "EXITOSO", null, ipAddress, userAgent);
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría: {}", e.getMessage());
        }
    }

    public record BackupStats(
            long total,
            long completados,
            long enProceso,
            long fallidos,
            SolicitudesRespaldo ultimoCompletado
    ) {}
}