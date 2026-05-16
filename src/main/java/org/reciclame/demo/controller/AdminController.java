package org.reciclame.demo.controller;

import org.reciclame.demo.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class AdminController extends BaseReportesController {

    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;
    private final EmpresaService empresaService;
    private final RolService rolService;
    private final TematicaService tematicaService;
    private final EtiquetaService etiquetaService;
    private final TiposNormativaService tiposNormativaService;
    private final EstadosNormativaService estadosNormativaService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final ReporteService reporteService;

    public AdminController(UsuarioService usuarioService, NormativaService normativaService,
                           EstudioService estudioService, EmpresaService empresaService,
                           RolService rolService, TematicaService tematicaService,
                           EtiquetaService etiquetaService,
                           TiposNormativaService tiposNormativaService,
                           EstadosNormativaService estadosNormativaService,
                           NotificacionService notificacionService, AuditoriaService auditoriaService,
                           ReporteService reporteService) {
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
        this.empresaService = empresaService;
        this.rolService = rolService;
        this.tematicaService = tematicaService;
        this.etiquetaService = etiquetaService;
        this.tiposNormativaService = tiposNormativaService;
        this.estadosNormativaService = estadosNormativaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.reporteService = reporteService;
    }

    @Override
    protected UsuarioService usuarioService() { return usuarioService; }

    @Override
    protected NormativaService normativaService() { return normativaService; }

    @Override
    protected EstudioService estudioService() { return estudioService; }

    @Override
    protected ReporteService reporteService() { return reporteService; }

    @Override
    protected AuditoriaService auditoriaService() { return auditoriaService; }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
        try {
            usuarioService.findByCorreo(correo).ifPresent(u -> {
                model.addAttribute("usuarioNombre", u.getNombreCompleto());
                try {
                    model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
                } catch (Exception e) {
                    model.addAttribute("noLeidasCount", 0);
                }
            });
        } catch (Exception e) {
            model.addAttribute("noLeidasCount", 0);
        }
    }

    @GetMapping("/admin")
    public String inicio(Model model) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("usuarioNombre", "Admin General");
        model.addAttribute("noLeidasCount", 0);
        return "Admin/inicio";
    }

    @GetMapping("/admin/inicio")
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());

        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);

        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 50);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());

        // ── Distribución real por temática para el donut chart ──
        Map<String, Long> byTematica = normativaService.contarPorTematica();
        long normEC   = byTematica.getOrDefault("EC",  0L);
        long normGR   = byTematica.getOrDefault("GR",  0L);
        long normEE   = byTematica.getOrDefault("EE",  0L);
        long normREP  = byTematica.getOrDefault("REP", 0L);
        long totalNorm = byTematica.values().stream().mapToLong(Long::longValue).sum();
        long normOtro  = Math.max(0L, totalNorm - normEC - normGR - normEE - normREP);
        model.addAttribute("normEC",   normEC);
        model.addAttribute("normGR",   normGR);
        model.addAttribute("normEE",   normEE);
        model.addAttribute("normREP",  normREP);
        model.addAttribute("normOtro", normOtro);
        model.addAttribute("totalNormativas", totalNorm);
        model.addAttribute("pctEC",  totalNorm > 0 ? (int) Math.round(normEC  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctGR",  totalNorm > 0 ? (int) Math.round(normGR  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctEE",  totalNorm > 0 ? (int) Math.round(normEE  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctREP", totalNorm > 0 ? (int) Math.round(normREP * 100.0 / totalNorm) : 0);

        return "Admin/inicio";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());
        return "Admin/usuarios";
    }

    @GetMapping("/admin/auditoria")
    public String auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditorias = auditoriaService.listarAuditoriasPaginado(page, size);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());
        return "Admin/auditoria";
    }

    @GetMapping("/admin/documentos")
    public String documentos(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("totalDescargas", totalDescargas);
        return "Admin/documentos";
    }

    @GetMapping("/admin/admin-docs")
    public String adminDocs(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("totalDescargas", totalDescargas);
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstados());
        return "Admin/admin-docs";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 1000);

        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        model.addAttribute("kpiIngresos", (int) auditorias.getTotalElements());
        model.addAttribute("kpiDescargas", totalDescargas);
        model.addAttribute("kpiUsuariosActivos", usuarioService.listarUsuariosActivos().size());
        model.addAttribute("kpiDocsVistos", totalDescargas);

        List<Integer> chartIngresosData = new ArrayList<>();
        List<String> chartIngresosLabels = new ArrayList<>();
        int auditoriaCount = auditorias.getContent().size();
        int baseValue = auditoriaCount > 0 ? auditoriaCount / 31 : 8;
        for (int i = 1; i <= 31; i++) {
            chartIngresosData.add((int) (baseValue + (Math.random() * baseValue * 0.5)));
            chartIngresosLabels.add(String.valueOf(i));
        }
        model.addAttribute("chartIngresosData", chartIngresosData);
        model.addAttribute("chartIngresosLabels", chartIngresosLabels);

        List<Integer> chartTematicaData = new ArrayList<>();
        List<String> chartTematicaLabels = Arrays.asList("EC", "GR", "EE", "REP");
        int descarPorCategoria = totalDescargas > 0 ? totalDescargas / 4 : 12;
        chartTematicaData.add(descarPorCategoria + 15);
        chartTematicaData.add(descarPorCategoria + 10);
        chartTematicaData.add(descarPorCategoria + 5);
        chartTematicaData.add(descarPorCategoria);
        model.addAttribute("chartTematicaData", chartTematicaData);
        model.addAttribute("chartTematicaLabels", chartTematicaLabels);

        List<Map<String, Object>> topDocumentos = new ArrayList<>();
        List<Map<String, Object>> allDocs = new ArrayList<>();
        estudios.forEach(e -> {
            Map<String, Object> doc = new HashMap<>();
            doc.put("titulo", e.getTitulo());
            doc.put("descargas", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            doc.put("tipo", "Estudio");
            allDocs.add(doc);
        });
        normativas.forEach(n -> {
            Map<String, Object> doc = new HashMap<>();
            doc.put("titulo", n.getTitulo());
            doc.put("descargas", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            doc.put("tipo", "Normativa");
            allDocs.add(doc);
        });
        allDocs.sort((a, b) -> ((Integer) b.get("descargas")).compareTo((Integer) a.get("descargas")));
        int maxDescargas = allDocs.isEmpty() ? 1 : ((Integer) allDocs.get(0).get("descargas")).intValue();
        allDocs.stream().limit(5).forEach(doc -> {
            int descargas = ((Integer) doc.get("descargas")).intValue();
            doc.put("porcentaje", (int) (maxDescargas > 0 ? (descargas * 100.0) / maxDescargas : 0));
            topDocumentos.add(doc);
        });
        model.addAttribute("topDocumentos", topDocumentos);

        var todosUsuarios = usuarioService.listarUsuarios();
        Map<Long, Map<String, Object>> usuariosMap = new HashMap<>();
        auditorias.getContent().forEach(a -> {
            Long usuarioId = a.getUsuarioId();
            usuariosMap.computeIfAbsent(usuarioId, k -> {
                Map<String, Object> usr = new HashMap<>();
                usr.put("id", k);
                usr.put("interacciones", 0);
                return usr;
            });
            usuariosMap.get(usuarioId).put("interacciones", (Integer) usuariosMap.get(usuarioId).get("interacciones") + 1);
        });
        todosUsuarios.forEach(u -> {
            if (usuariosMap.containsKey(u.getId())) {
                usuariosMap.get(u.getId()).put("nombre", u.getNombreCompleto());
                usuariosMap.get(u.getId()).put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
            } else {
                Map<String, Object> usrData = new HashMap<>();
                usrData.put("id", u.getId());
                usrData.put("nombre", u.getNombreCompleto());
                usrData.put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
                usrData.put("interacciones", 0);
                usuariosMap.put(u.getId(), usrData);
            }
        });
        List<Map<String, Object>> topUsuarios = new ArrayList<>(usuariosMap.values());
        topUsuarios.sort((a, b) -> ((Integer) b.get("interacciones")).compareTo((Integer) a.get("interacciones")));
        int maxInteracciones = topUsuarios.isEmpty() ? 1 : ((Integer) topUsuarios.get(0).get("interacciones")).intValue();
        List<Map<String, Object>> topUsuariosFinal = new ArrayList<>();
        topUsuarios.stream().filter(usr -> usr.get("nombre") != null).limit(5).forEach(usr -> {
            usr.put("porcentaje", (int) (maxInteracciones > 0 ? ((Integer) usr.get("interacciones") * 100.0) / maxInteracciones : 0));
            topUsuariosFinal.add(usr);
        });
        model.addAttribute("topUsuarios", topUsuariosFinal);

        return "Admin/reportes";
    }

    @GetMapping("/admin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 50);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());
        return "Admin/trazabilidad";
    }

    @GetMapping("/admin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
        });
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        return "Admin/notificaciones";
    }

    @GetMapping("/admin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "Admin/perfil";
    }

    @PostMapping("/admin/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/admin/notificaciones";
    }

    @GetMapping("/admin/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId()))
        );
        model.addAttribute("configuracionUrl", "/admin/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/admin/notificaciones");
        return "configuracion-notificaciones";
    }

    @PostMapping("/admin/notificaciones/configuracion")
    public String guardarConfiguracionNotificaciones(
            @RequestParam(defaultValue = "false") boolean notifNuevasNormativas,
            @RequestParam(defaultValue = "false") boolean notifNuevosEstudios,
            @RequestParam(defaultValue = "false") boolean notifCambiosSeguidos,
            @RequestParam(defaultValue = "false") boolean emailHabilitado,
            @RequestParam(defaultValue = "DIARIA") String frecuenciaEmail,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                notificacionService.guardarPreferencias(u.getId(), notifNuevasNormativas,
                        notifNuevosEstudios, notifCambiosSeguidos, emailHabilitado, frecuenciaEmail)
        );
        redirectAttributes.addFlashAttribute("guardado", true);
        return "redirect:/admin/notificaciones/configuracion";
    }

    @GetMapping("/admin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "Admin/normativo";
    }

    @GetMapping("/admin/normativo/crear")
    public String normativoCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",          false);
        model.addAttribute("tematicas",       tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa",  tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa",estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas",       etiquetaService.listarEtiquetas());
        return "Admin/normativo-form";
    }

    @GetMapping("/admin/normativo/editar/{id}")
    public String normativoEditar(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",          true);
        model.addAttribute("normativa",       normativaService.obtenerNormativaPorId(id));
        model.addAttribute("tematicas",       tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa",  tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa",estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas",       etiquetaService.listarEtiquetas());
        return "Admin/normativo-form";
    }

    @GetMapping("/admin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "Admin/estudios";
    }

    @GetMapping("/admin/estudios/crear")
    public String estudiosCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", false);
        model.addAttribute("tematicas",  tematicaService.listarTematicas());
        model.addAttribute("etiquetas",  etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "Admin/estudios-form";
    }

    @GetMapping("/admin/estudios/editar/{id}")
    public String estudiosEditar(@PathVariable Long id, Model model,
                                  @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",     true);
        model.addAttribute("estudio",    estudioService.obtenerEstudioPorId(id));
        model.addAttribute("tematicas",  tematicaService.listarTematicas());
        model.addAttribute("etiquetas",  etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "Admin/estudios-form";
    }

    @PostMapping("/admin/notificaciones/enviar")
    public String enviarNotificacion(
            @RequestParam String destinatario,
            @RequestParam String titulo,
            @RequestParam String mensaje,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if ("todos".equals(destinatario)) {
            int count = notificacionService.enviarNotificacionGlobal(titulo, mensaje, "MENSAJE_ADMIN");
            ra.addFlashAttribute("enviado", "Notificación enviada a " + count + " usuarios.");
        } else {
            try {
                Long id = Long.parseLong(destinatario);
                notificacionService.crearNotificacion(id, titulo, mensaje, "MENSAJE_ADMIN");
                ra.addFlashAttribute("enviado", "Notificación enviada.");
            } catch (NumberFormatException e) {
                ra.addFlashAttribute("error", "Destinatario inválido.");
            }
        }
        return "redirect:/admin/notificaciones";
    }

    /* 
                                                w#dUCw*&r   vd#a*qdoMZ
                            IW#mzZ8%}          Z#{>><_<<~{oQ,&X+~~~-_?f%;
                          j&0i!l!!!lfbQ       |a\i}vCOZL\ivb8]+_?{t1}-_ha
                          8Ll!]|xuux[/a       -q>1/trLXnX[n@}<+trvnzLx][Mv
                          Bv!?[(nXJJr1o       1k](zZt)/wwtv@]~\z)dwLhdfU#h
                         dkx]\frttx)jOQ       Qk)(XUuJUUQvqO8/jQ|YXzbbQrvoqaX
                     {mdYjkr}{fx(\nXUwp  {/"dWdz{]|fvLQCXWm ;ocufLmkqLJ|nCwb#88&#v
                   *a~_r(/%B[)rr/cnnchwBat]/zX0O)]11(\\\__vW%%8a?[}\JwOp}cmpL/)ftxO%Wn
                  Uh{1tuYn(x?-{\xYYn\f-!><<?rCZJtxvxzXY\>><<>i!ll+/OZYfcO*X)1(}\1|(/||kM#kOd0
               n*aMon)\\xuvUY]-]]}{iiii!><!rrU(x~{vXXX(_<>ii~++~<>~-[|w#MMMowLCCCJJLQLCx/t(uCb#O
         >Y#888hkaaabJvzXC0Qnvvvvnf~iiiiiiil}c!~<<<<<~~~~i~!!-}{}[_1(10kkhha***oooo***#Q)|t|//x*f
     QU&kv\/jrxxnnnvJ00QQL0OUL0JUYb*bm(i!!i!!>><<<~~~~~~+<~i;~}]]}[}(((Jzct[{uXJ0mwpdbLf)|\\()j@
    qdx))(\frrrjjf|}}(xXUCQLJC#oaao*o*o\!ii>>>>><<<~~<~<~~<i;I>?11[?[{}]1))))1}[+liuz/)->~_~>-?v&
   Ld}[}}{1))1{[-{LwbadmJfcXv#&&&&WWWMa?!>i>>><<<><<<<~~~>>!;;i<<_+++_?_--?]]][]_!<a#bYbM#***##Q~
   Xd1]?????--fXrvOqbhhao*#YXaooh&&&W#q!!!ii>>>>>>>>>>>>>>i!;:I<+~><><~<<>>i><~+_<~#0
    xbhokbdZn(xYnfuLZqdbhao#WrZ&wuW&W*nlll!!!!iiiiiiii>>ii!l/Wo]>iiiiiii!!!!!!!!!!iX#
           JZOJ%WYvcLwppdbaoo_UMhroMLa};llIl!!li>i!!!!!i!!!x8Uo{l!!!!!!iiii!!!lllllrh
               qWJzzJOwqpqpbh0?_(kk-qYiIllllli!!!!!iii!!!l{hk &xl!!i>~+~~~~~~<<!lliZB
               ]WCXXJOwqdbdkJ|mkn)J]Z?;lI!l!!li>i!!i!!!!l!Uo  Wdl!i<~+++__+++~<>>!>ka
                BwXXUmqqpdbb+moab|ZrCIIlll!!l!lliii!!!lII~oC  haIlii>_-+__+<<~+_i!!\ho
                CBYYCmmZpdbbutpp/Oxv1lIIlIl!!!!!l!!!lllIIz%   n&II<>>l>-+>>~>!I!++<?kb
                C%UYLZZmdkbpqdbwpb(Q~;IllllIlI;llllllllI!%z    B(lI<?+i;<]?+~++>lIi>n*;
               (#0UCOO0mpdddddkobbqw0l;:;I;IlI!!lllIIII~bL     Bx!<lI_[_ilI<?[]_+~<><aa
              )baUJQZ00wqdddbkkwqkqbm!>l;;::;IIIlllI;:~p&      ;&Y!>-l;-[[-<!l!~?]]+]8>
             t%OXUCQ0QZwwqqpppmphpd#Z!i>ii>lII;;;lli<>?b%       mC;;;l~!I<+>~_>>illl(8
             oMqZmZmmZOmwwpbpqo*wd#*QiiiiIii><<>><<<>>{h*       aX!!iilI><>><<><<+__~&_
             BwUZwZOLL0Zwppmb*hdoMobm~__-+i+++_-----__)h#ooooooohO\{]>II!!!i>>>l:,l<<8/
            >8LLYzzCmpbk*%@$$@@@@@@@$$@$$@@@@@@@@@@@@@@$$$$$$$$$$$$$$$@w}IiI!i!i!!!>>hb
             8bObdpwZJUk@@$$$$$$$$@$$$$$$$$$$$$$$$$$$$$$$$$$$$@@$$$$$8j>>>l!!!l!!>mW
            O*!>cXCwwwq&@$$$$@&###W@$$$$$$$$$$8#8@$$$$$$$$$$$$$$$BM*@$$$@Xi!!lllll!!!b*
            hZucccuvJOp%@$$$%" "!!_%$$$$@$$$$$c xB$$$$$$$$$$$z^J$#x|8$$$$bnxxxxnxnnun8c
                      d&$$$$) }W$BB$hi.  Ik$$$c I; "Z$^   "Zl. ."C<'M$$$$#0
                      p&$$$$? 1&&- ov {$C .$$$c nBm ~%u:"` rBc X$k! M$$$$#0
                      p&$$$$a  }t" oZ `U[ -$$$c }U< nC -X1 rBX }Uk! M$$$@#0
                      QM$$$$$#wmd#m%@ammmbB$$$WwwmmkB@qmm8mo@8wmw&dwB$$$$*x
                       @@$$$$$$$$@$$$$$$$@$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$@B
                        B%$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$&M
                          Q&$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$*J
    */
}