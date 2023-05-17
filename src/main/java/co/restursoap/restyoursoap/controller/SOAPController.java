package co.restursoap.restyoursoap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/soap")
public class SOAPController {
    @GetMapping(path= "/ping")
    public Map<String, String> ping(){
        return new HashMap<>(){
            {
                put("Ping", "pong");
            }
        };
    }

    @PostMapping(path = "/toREST")
    public Map getSOAPTranslated(){
        return new HashMap();
    }
}