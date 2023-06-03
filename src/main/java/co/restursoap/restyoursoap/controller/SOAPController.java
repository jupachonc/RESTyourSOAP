package co.restursoap.restyoursoap.controller;

import co.restursoap.restyoursoap.service.SOAPservice;
import jakarta.servlet.ServletOutputStream;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @PostMapping(path = "/getProject", produces = "application/zip")
    public ResponseEntity<?> getProject(@RequestParam("file") MultipartFile multipartFile,
                                        ServletOutputStream responseOutputStream)
            throws IOException{
        String xml = convertInputStreamToString(multipartFile.getInputStream());
        Pair<String, String> out = sService.getProject(xml);

        /*
            Create Zip
        */
        ZipOutputStream project = new ZipOutputStream(responseOutputStream);
        // Create package.json
        byte[] packagejson = out.getValue1().getBytes();
        ZipEntry pjsonEntry = new ZipEntry("package.json");
        project.putNextEntry(pjsonEntry);
        project.write(packagejson, 0, packagejson.length);
        project.closeEntry();

        // Create index.js
        byte[] indexjs = out.getValue1().getBytes();
        ZipEntry indexEntry = new ZipEntry("index.js");
        project.putNextEntry(indexEntry);
        project.write(indexjs, 0, indexjs.length);
        project.closeEntry();
        project.close();

        String headerValue = "attachment; filename=\"" + out.getValue0() + ".zip" + "\"";

        return  ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(out.getValue1());

    }
}