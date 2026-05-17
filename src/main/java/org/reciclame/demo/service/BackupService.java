package org.reciclame.demo.service;

import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.model.ProgramacionBackup;
import org.reciclame.demo.model.SolicitudesRespaldo;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.AuditoriaRepository;
import org.reciclame.demo.repository.BackupRepository;
import org.reciclame.demo.repository.ProgramacionBackupRepository;
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
    private final AuditoriaRepository auditoriaRepository;

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
                         AuditoriaRepository auditoriaRepository) {
        this.backupRepository = backupRepository;
        this.programacionRepository = programacionRepository;
        this.auditoriaRepository = auditoriaRepository;
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
        return backupRepository.save(backup);
    }

    @Async
    public CompletableFuture<Void> ejecutarBackupAsync(SolicitudesRespaldo backup) {
        try {
            ejecutarBackup(backup);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error ejecutando backup {}: {}", backup.getId(), e.getMessage(), e);
            marcarFallo(backup.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void ejecutarBackup(SolicitudesRespaldo backup) {
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

        } catch (Exception e) {
            backup.setEstado("FALLIDO");
            backupRepository.save(backup);
            log.error("Backup {} falló: {}", backup.getId(), e.getMessage(), e);
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
            ejecutarBackupAsync(backup);
        }
    }

    @Transactional
    public void restaurarBackup(Long backupId, Usuario usuario, boolean crearBackupSeguridad) {
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

            backup.setRestauradoPor(usuario);
            backup.setFechaRestauracion(Instant.now());
            backup.setEstado("COMPLETADO");
            backupRepository.save(backup);

            registrarAuditoria(usuario, "RESTAURAR_BACKUP", "BACKUPS",
                    "Restauración desde backup ID: " + backupId + " (" + backup.getStorageKey() + ")");

            log.info("Restauración completada desde backup {} por usuario {}", backupId, usuario.getNombreCompleto());

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
        List<String> sentencias = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;
        boolean skipStatement = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(archivoPath.toFile())))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*") || line.startsWith("mysqldump:") || line.startsWith("mariadb-dump:")) {
                    continue;
                }

                if (line.toUpperCase().startsWith("DROP TABLE")) {
                    skipStatement = true;
                    continue;
                }

                if (skipStatement && (line.toUpperCase().startsWith("CREATE TABLE") || line.toUpperCase().startsWith("INSERT"))) {
                    skipStatement = false;
                }

                if (skipStatement) {
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
                                sentencias.add(stmt);
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
            sentencias.add(remaining);
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
        });
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

    private String generarNombreArchivo(String tipo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = java.time.LocalDateTime.now(ZoneId.of("America/Lima")).format(formatter);
        return "backup_" + tipo.toLowerCase() + "_" + timestamp + ".sql";
    }

    private void registrarAuditoria(Usuario usuario, String accion, String modulo, String descripcion) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuarioId(usuario.getId());
            auditoria.setUsuarioNombre(usuario.getNombreCompleto());
            auditoria.setAccion(accion);
            auditoria.setModulo(modulo);
            auditoria.setDescripcion(descripcion);
            auditoria.setEstado("EXITOSO");
            auditoriaRepository.save(auditoria);
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