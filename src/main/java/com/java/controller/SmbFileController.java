package com.java.controller;


import com.java.util.SmbUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
public class SmbFileController {


    @Autowired
    SmbUtil smbUtil;

    @PostMapping("/smb/upload")
    public ResponseEntity upload(

            @RequestParam("id") String id,
            @RequestParam(value = "message",required = false) String message,
            @RequestParam("file") List<MultipartFile> files
           ) throws IOException {
        smbUtil.upload(id,message, files);
        return ResponseEntity.ok("success");
    }

    @GetMapping("/smb/list")
    public ResponseEntity list(@RequestParam("id") String id) throws IOException {
       return  ResponseEntity.ok(smbUtil.list(id));
    }

}
