package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatViewController {

    private final String centrifugoWsUrl;

    public ChatViewController(@Value("${centrifugo.ws.url:}") String centrifugoWsUrl) {
        this.centrifugoWsUrl = centrifugoWsUrl;
    }

    @GetMapping("/chat")
    public String chat(HttpServletRequest request, Model model) {
        String wsUrl = (centrifugoWsUrl == null || centrifugoWsUrl.isBlank())
                ? (request.isSecure() ? "wss://" : "ws://") + request.getServerName() + "/connection/websocket"
                : centrifugoWsUrl;
        model.addAttribute("centrifugoWsUrl", wsUrl);
        return "chat";
    }
}
