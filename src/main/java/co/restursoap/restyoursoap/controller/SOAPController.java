package co.restursoap.restyoursoap.controller;

import co.restursoap.restyoursoap.service.SOAPservice;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/soap")
public class SOAPController {

    @Autowired
    SOAPservice sService;
    private static String convertInputStreamToString(InputStream inputStream)
            throws IOException {

        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    @GetMapping(path= "/ping")
    public Map<String, String> ping(){
        return new HashMap<>(){
            {
                put("Ping", "pong");
            }
        };
    }

    @PostMapping(path = "/toREST")
    public ResponseEntity<?> getSOAPTranslated(@RequestParam("file") MultipartFile multipartFile)
            throws IOException{
        String xml = convertInputStreamToString(multipartFile.getInputStream());
        Pair<String, String> out = sService.translateToOpenAPI(xml);

        String headerValue = "attachment; filename=\"" + out.getValue0() + ".yaml" + "\"";

        return  ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/yaml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(out.getValue1());

    }

    @PostMapping(path = "/getProject")
    public ResponseEntity<?> getProject(@RequestParam("file") MultipartFile multipartFile)
            throws IOException{
        String xml = convertInputStreamToString(multipartFile.getInputStream());
        Pair<String, String> out = sService.getProject(xml);

        String headerValue = "attachment; filename=\"" + "package.json" + "\"";

        return  ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json"))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(out.getValue1());

    }
}