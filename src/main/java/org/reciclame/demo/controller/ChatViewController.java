package org.reciclame.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatViewController {

    private final String centrifugoWsUrl;

    public ChatViewController(@Value("${centrifugo.ws.url}") String centrifugoWsUrl) {
        this.centrifugoWsUrl = centrifugoWsUrl;
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("centrifugoWsUrl", centrifugoWsUrl);
        return "chat";
    }
}
