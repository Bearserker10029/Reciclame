package org.reciclame.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CentrifugoWsUrlAdvice {

    private final String centrifugoWsUrl;

    public CentrifugoWsUrlAdvice(@Value("${centrifugo.ws.url:}") String centrifugoWsUrl) {
        this.centrifugoWsUrl = centrifugoWsUrl;
    }

    @ModelAttribute
    public void addCentrifugoWsUrl(HttpServletRequest request, Model model) {
        if (model.containsAttribute("centrifugoWsUrl")) {
            return;
        }
        String wsUrl = (centrifugoWsUrl == null || centrifugoWsUrl.isBlank())
                ? (request.isSecure() ? "wss://" : "ws://") + request.getServerName() + "/connection/websocket"
                : centrifugoWsUrl;
        model.addAttribute("centrifugoWsUrl", wsUrl);
    }
}
