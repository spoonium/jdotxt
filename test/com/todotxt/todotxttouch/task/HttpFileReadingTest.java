package com.todotxt.todotxttouch.task;

import com.chschmid.jdotxt.SpoonOnlineTodo;
import com.sun.net.httpserver.*;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class HttpFileReadingTest {


    private static SSLContext properSSL;
    private static HttpServer sslServer;
    private static HttpServer otherServer;
    private final static String ticket = "<A spoon ticket>";

    @BeforeClass
    public static void setUp() throws Exception {
        properSSL = SSLContext.getDefault();
        sslServer = mainServer();
        otherServer = otherServer(sslServer);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        SSLContext.setDefault(properSSL);
        sslServer.stop(1);
        otherServer.stop(1);
    }


    @Test
    public void downloadOnlineFile() {
        int port = otherServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("http://localhost:" + port + "/source.txt", "ticket");
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }

    @Test
    public void downloadOnlineFileViaRedirect() {

        int port = otherServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("http://localhost:" + port + "/redirect.txt", ticket);
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }
    @Test
    public void downloadOnlineViaSSL() {

        int port = sslServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("https://localhost:" + port + "/source.txt", ticket);
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }
    @Test
    public void downloadOnlineFileViaSSLRedirect() {

        int port = sslServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("https://localhost:" + port + "/redirect.txt", ticket);
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }
    @Test
    public void upgradeToSSLRedirect() {

        int port = otherServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("http://localhost:" + port + "/to-ssl-redirect.txt", ticket);
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }
    @Test
    public void relativeRedirect() {

        int port = sslServer.getAddress().getPort();
        String todoFile = SpoonOnlineTodo.download("https://localhost:" + port + "/redirect-relative.txt", ticket);
        Assert.assertTrue(todoFile.startsWith("Proper todo"));
    }

    public static HttpServer otherServer(HttpServer sslServer) throws IOException {
        int port = freePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 1);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                int sslServerPort = sslServer.getAddress().getPort();
                if (httpExchange.getRequestURI().getPath().contains("to-ssl-redirect.txt")) {
                    redirectTo(httpExchange, "https://localhost:" + sslServerPort + "/source.txt");
                } else if (httpExchange.getRequestURI().getPath().contains("redirect.txt")) {
                    redirectTo(httpExchange,"http://localhost:" +port + "/source.txt");
                }  else if (httpExchange.getRequestURI().getPath().contains("source.txt")) {
                    serveTodoFile(httpExchange);

                } else {
                    httpExchange.sendResponseHeaders(500, 0);
                }
            }
        });
        server.setExecutor(null); // creates a default executor
        server.start();
        return server;
    }




    private static int freePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        try {
            return serverSocket.getLocalPort();
        } finally {
            serverSocket.close();
        }
    }

    public static HttpsServer mainServer() throws IOException,Exception {
        int port = freePort();

        SSLContext ssl;

        ssl = createSelfSignedTLS();
        SSLContext.setDefault(ssl);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 1);
        server.setHttpsConfigurator(new HttpsConfigurator(ssl) {

            public void configure (HttpsParameters params) {

                // get the remote address if needed
                InetSocketAddress remote = params.getClientAddress();

                SSLContext c = getSSLContext();

                // get the default parameters
                SSLParameters sslparams = c.getDefaultSSLParameters();

                params.setSSLParameters(sslparams);
            }
        });
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                if (httpExchange.getRequestURI().getPath().contains("redirect-relative.txt")) {
                    redirectTo(httpExchange, "/source.txt");
                } else if (httpExchange.getRequestURI().getPath().contains("redirect.txt")) {
                    redirectTo(httpExchange, "https://localhost:" + port + "/source.txt");
                } else if (httpExchange.getRequestURI().getPath().contains("source.txt")) {
                    serveTodoFile(httpExchange);

                } else {
                    httpExchange.sendResponseHeaders(500, 0);
                }
            }
        });
        server.setExecutor(null); // creates a default executor
        server.start();
        return server;
    }

    private static void serveTodoFile(HttpExchange httpExchange) throws IOException {
        String response = "Proper todo";
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes("UTF-8"));
        os.close();
    }

    private static SSLContext createSelfSignedTLS() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, InvalidKeyException, SignatureException, UnrecoverableKeyException, KeyManagementException {
        SSLContext ssl;
        int keysize = 1024;
        String commonName = "localhost";
        String organizationalUnit = "IT";
        String organization = "test";
        String city = "test";
        String state = "test";
        String country = "DE";
        long validity = 1096; // 3 years
        String alias = "tomcat";
        char[] keyPass = "changeit".toCharArray();


        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        {
            CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
            X500Name x500Name = new X500Name(commonName, organizationalUnit, organization, city, state, country);

            keypair.generate(keysize);
            PrivateKey privKey = keypair.getPrivateKey();

            X509Certificate[] chain = new X509Certificate[1];

            chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) 1 * 24 * 60 * 60);

            keyStore.setKeyEntry(alias, privKey, keyPass, chain);
        }
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keyPass);
        ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(),new TrustManager[]{new TrustEveryCertificate()} , null);
        return ssl;
    }

    static class TrustEveryCertificate implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    private static void redirectTo(HttpExchange httpExchange, String location) throws IOException {
        String response = "Redirect hint";
        httpExchange.getResponseHeaders().add("Location", location);
        httpExchange.getResponseHeaders().add("Content-Type","text/html" );
        httpExchange.sendResponseHeaders(301, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes("UTF-8"));
        os.close();
    }
}
