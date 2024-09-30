package com.github.skopylov58.groovy.decompiler.http;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.skopylov58.groovy.decompiler.GroovyDecompiler;
import com.github.skopylov58.groovy.decompiler.GroovyVisitor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class MyHttpServer {

    static class StatusHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            var method = ex.getRequestMethod();
            if (method.equals("GET")) {
                var path = ex.getRequestURI().getPath();
                if (path.equals("/")) {
                    var form = getForm().replaceAll("__[a-z]+__", ""); //clean form
                    ex.sendResponseHeaders(200, form.length());
                    OutputStream os = ex.getResponseBody();
                    os.write(form.getBytes());
                } else {
                    ex.sendResponseHeaders(404, 0);
                }
            } else if (method.equals("POST")) {
                InputStream bodyInput = ex.getRequestBody();
                String body = new String(bodyInput.readAllBytes());
                Map<String, String> postParam = parsePostParameters(body);
                System.out.println(postParam);

                int resCode = 200;
                String groovyCode = "{\n" + postParam.get("groovy") + "}\n"; //Make it block
                try {
                    String decompiled = decompile(postParam.get("callsite"), groovyCode);
                    System.out.println(decompiled);
                    postParam.put("java", decompiled);

                } catch (Exception e) {
                    postParam.put("java", e.getMessage());
                    resCode = 500;
                }
                String outForm = template(postParam);
                ex.sendResponseHeaders(resCode, outForm.length());
                ex.getResponseBody().write(outForm.getBytes());

            } else {
                ex.sendResponseHeaders(405, 0);
            }
            ex.getResponseBody().flush();
            ex.getResponseBody().close();
            ex.close();
        }
    }

    static Map<String, String> parsePostParameters(String reqBody) {
        System.out.println(reqBody);
        var r = Arrays.stream(reqBody.split("&"))
                .map(s -> s.split("="))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
        r.replaceAll((k, v) -> URLDecoder.decode(v, StandardCharsets.UTF_8));
        return r;
    }

    static String getForm() throws IOException {
        InputStream input = MyHttpServer.class.getResourceAsStream("/form.html");
        if (input != null) {
            String s = new String(input.readAllBytes());
            input.close();
            return s;
        } else {
            throw new RuntimeException("form.html not found");
        }
    }

    static String decompile(String callSite, String groovy) {
        BlockStmt block = StaticJavaParser.parseBlock(groovy);

        String[] callSiteArray = GroovyDecompiler.loadCallSite(callSite.lines());
        if (callSiteArray.length == 0) {
            throw new RuntimeException("callsite not found");
        }
        GroovyVisitor gv = new GroovyVisitor(callSiteArray);
        block.accept(gv, 0);

        return block.toString();
    }

    static String template(Map<String, String> params) throws IOException {
        String res = getForm();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            res = res.replace("__" + k + "__", v);
        }
        return res;
    }

    public static void main(String[] args) throws IOException {
        int port = 8440;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 1024);
        server.createContext("/", new StatusHandler());
        server.start();
        System.out.println("Decompiler server started on port " + port);
    }
}
