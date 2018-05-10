package io.netninjas.management;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.http.MediaType.parseMediaType;

@RestController
public class ManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ManagementController.class);

    @RequestMapping(path = "/download/{filename:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@PathVariable("filename") String filename, HttpServletRequest request) throws IOException {

        HttpHeaders headers = new HttpHeaders();

        File file = new File("//root//" + filename);
        Path path = Paths.get(file.getAbsolutePath());
        logger.info(String.format("download request for file=%s, from IP=%s", file.getAbsolutePath(), request.getRemoteAddr()));
        System.out.println("..........will try to download " + file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(parseMediaType("application/octet-stream"))
                .body(resource);
    }

    @RequestMapping(value = "/generateNewClient/{clientName}", produces = "text/plain")
    public String generateNewClient(@PathVariable("clientName") String clientName, HttpServletRequest request) {
        if (clientName.isEmpty()) {
            throw new RuntimeException("client name is mandatory!!! please specify it!");
        }
        logger.info(String.format("generating new client certificate name=%s, from IP=%s", clientName, request.getRemoteAddr()));

        Runtime run = Runtime.getRuntime();
        String cmd = "//root//generateNewClient.sh " + clientName;
        try {
            Process pr = run.exec(cmd);
            pr.waitFor();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug(String.format("certificate name=%s, from IP=%s has been generated", clientName, request.getRemoteAddr()));
        return "new client has been generated: " + clientName + ".ovpn";
    }

    @RequestMapping(value = "/removeClient/{clientName}", produces = "text/plain")
    public String removeClient(@PathVariable("clientName") String clientName, HttpServletRequest request) throws IOException {
        if (clientName.isEmpty()) {
            throw new RuntimeException("client name is mandatory!!! please specify it!");
        }
        logger.info(String.format("[RemoveClient] name=%s, from IP=%s", clientName, request.getRemoteAddr()));

        if (!clientName.endsWith("key")){
            clientName = clientName + ".key";
        }
        FileUtils.forceDelete(new File("//etc//openvpn//easy-rsa//pki//private//" + clientName));
        logger.debug(String.format("certificate name=%s, from IP=%s has been REMOVED", clientName, request.getRemoteAddr()));
        return "file " + clientName + " has been removed!";
    }

    @RequestMapping(value = "/getPackets/{theip:.+}", produces = "text/plain")
    public String getPackets(@PathVariable("theip") String theip, HttpServletRequest request) throws IOException, InterruptedException {
        logger.debug(String.format("got request to count packets from IP=%s ", request.getRemoteAddr()));
        return checkPackets(theip);
    }

    @RequestMapping(value = "/getMyPackets", produces = "text/plain")
    public String getMyPackets(HttpServletRequest request) throws IOException, InterruptedException {
        logger.debug(String.format("got request to count packets from IP=%s ", request.getRemoteAddr()));
        return checkPackets(request.getRemoteAddr());
    }


    private String checkPackets(String theip) throws IOException, InterruptedException {
        String command1 = String.format("tshark -i any -T text  -qz io,stat,0,ip.src==%s,ip.dst==%s > /tmp/2.txt\n", theip, theip);
        logger.debug("executing shell command: " + command1);
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command1);

        Thread.sleep(2000);

        String command2 = String.format("cat /tmp/2.txt | grep \"%s\" | awk '{print $7}' | awk '{sum += $1} END {print sum}'", theip);
        logger.debug("executing shell command: " + command2);
        Runtime rt2 = Runtime.getRuntime();
        Process proc2 = rt.exec(command2);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc2.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc2.getErrorStream()));

        // read the output from the command

        String myOutput = stdInput.readLine();
        logger.debug("the output of the execution: " + myOutput);


// read any errors from the attempted command
        if (myOutput == null) {
            myOutput = stdError.readLine();
            logger.debug("the error of the execution: " + myOutput);
        }
        return myOutput;
    }
}
