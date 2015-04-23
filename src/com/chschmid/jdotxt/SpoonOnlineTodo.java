package com.chschmid.jdotxt;

import javax.swing.text.html.Option;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Optional;

public final class SpoonOnlineTodo {

    public static void downloadAndStoreTodo(File location,Collection<String> args){
        Optional<String> todoTxt = downloadTodo(args);
        location.getParentFile().mkdirs();
        if(todoTxt.isPresent()){
            try {
                Files.write(location.toPath(),todoTxt.get().getBytes("UTF-8"), StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new TodoFileDownloadFailed(e.getMessage(),e);
            }
        }

    }

    public static Optional<String> downloadTodo(Collection<String> args){
        Optional<String> remote = args.stream().filter(e -> e.startsWith("http")).findAny();
        Optional<String> ticket = args.stream()
                .filter(e -> e.startsWith("--ticket"))
                .map(SpoonOnlineTodo::valueOfArgument)
                .map(t -> t.replace('"','\'')).findAny();
        return remote.map(url->download(url,ticket.orElse("")));
    }

    static private String valueOfArgument(String argument){
        int split = argument.indexOf('=');
        return argument.substring(split + 1);
    }


    static public String download(String target, String authToken) {
        try {
            return download(new URI(target),authToken);
        } catch (URISyntaxException e) {
            throw new TodoFileDownloadFailed(e.getMessage(),e);
        }
    }
    static public String download(URI target, String authToken) {
        try {
            HttpURLConnection connection = connectFollowRedirects(target, authToken);
            StringBuilder buffer = new StringBuilder();
            try(InputStream read = connection.getInputStream()){

                BufferedReader br = new BufferedReader(new InputStreamReader(read,"UTF-8"));

                String line;
                while ( (line = br.readLine()) != null){
                    buffer.append(line).append("\n");
                }
                return buffer.toString();
            }
        } catch (IOException e) {
            throw new TodoFileDownloadFailed(e.getMessage(),e);
        }
    }


    private static HttpURLConnection connectFollowRedirects(URI target, String authToken) throws IOException {
        int maxAttempts = 3;
        URL url = target.toURL();
        for(int i=0;i<maxAttempts;i++){
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("X-Spoon-Ticket", authToken);
            connection.setInstanceFollowRedirects(true);

            switch (connection.getResponseCode())
            {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    String location = connection.getHeaderField("Location");
                    URL absolutizeNext     = new URL(url, location);  // Deal with relative URLs
                    url = absolutizeNext;
                    continue;
            }
            return connection;
        }
        throw new IOException("To many redirects. Redirected: "+maxAttempts+" times");
    }

    static class TodoFileDownloadFailed extends RuntimeException{
        public TodoFileDownloadFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
