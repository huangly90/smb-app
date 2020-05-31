package com.java.util;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Slf4j
@Component
public class SmbUtil {

    @Value("${smb.servername}")
    public   String SERVERNAME;
    @Value("${smb.username}")
    public   String USERNAME ;
    @Value("${smb.password}")
    public   String PASSWORD;
    @Value("${smb.domain}")
    public   String DOMAIN ;
    @Value("${smb.sharename}")
    public   String SHARENAME ;
    @Value("${smb.serverport}")
    public  int SERVERPORT;

    public void upload(String id, String message, List<MultipartFile> files) throws IOException {
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(SERVERNAME,SERVERPORT)) {
            AuthenticationContext ac = new AuthenticationContext(USERNAME, PASSWORD.toCharArray(), DOMAIN);
            Session session = connection.authenticate(ac);
            try (DiskShare share = (DiskShare) session.connectShare(SHARENAME)) {
                Set<FileAttributes> fileAttributes = new HashSet<>();
                fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
                Set<SMB2CreateOptions> createOptions = new HashSet<>();
                createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
                Set<AccessMask> accessMasks = new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL));
                if (!share.folderExists(id)) {
                    share.mkdir(id);
                }

                files.forEach(
                        multipartFile -> {
                            //存储文件
                            File file = share.openFile(
                                    id + "" + java.io.File.separator + multipartFile.getOriginalFilename(),
                                    accessMasks,
                                    fileAttributes,
                                    SMB2ShareAccess.ALL,
                                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                                    createOptions
                            );
                            try (InputStream inputStream = multipartFile.getInputStream(); OutputStream outputStream = file.getOutputStream()) {
                                IOUtils.copy(inputStream, outputStream);
                            } catch (Exception e) {
                                log.error("存储文件：{} 失败：", multipartFile.getName(), e);
                            }

                        }


                );

            }
        }
    }

    public List<Map> list(String fileid) throws IOException {

        List<Map> imgList = new ArrayList<>();
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(SERVERNAME,SERVERPORT)) {
            AuthenticationContext ac = new AuthenticationContext(USERNAME, PASSWORD.toCharArray(), DOMAIN);
            Session session = connection.authenticate(ac);
            try (DiskShare share = (DiskShare) session.connectShare(SHARENAME)) {
                List<FileIdBothDirectoryInformation> informations = share.list(fileid);


                Set<FileAttributes> fileAttributes = new HashSet<>();
                fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
                Set<SMB2CreateOptions> createOptions = new HashSet<>();
                createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
                Set<AccessMask> accessMasks = new HashSet<>(Collections.singletonList(AccessMask.GENERIC_ALL));

                informations.forEach(
                        information -> {
                            if (information.getAllocationSize() > 0) {
                                File file = share.openFile(
                                        fileid + "" + java.io.File.separator + information.getFileName(),
                                        accessMasks,
                                        fileAttributes,
                                        SMB2ShareAccess.ALL,
                                        SMB2CreateDisposition.FILE_OPEN,
                                        createOptions
                                );
                                try (InputStream inputStream = file.getInputStream(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                                    IOUtils.copy(inputStream, outputStream);
                                    HashMap map = new HashMap();
                                    map.put("name", information.getFileName());
                                    map.put("img", Base64Utils.encodeToString(outputStream.toByteArray()));
                                    imgList.add(map);
                                } catch (Exception e) {
                                    log.error("读取文件：{} 失败：", file.getFileName(), e);
                                }
                            }
                        }
                );

            }
        }
        return imgList;
    }

    public  void main(String[] args) throws IOException {
        List<Map> list = new SmbUtil().list("999999");

        list.forEach(map -> {

            System.out.println(map.get("name"));
            System.out.println(map.get("img"));

        });
    }
}
