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
        if(todoTxt.isPresent()){
            try {
                Files.write(location.toPath(),todoTxt.get().getBytes("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING);
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
        return argument.substring(split+1);
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
            URL url = target.toURL();
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("X-Spoon-Ticket",authToken);
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


    static class TodoFileDownloadFailed extends RuntimeException{
        public TodoFileDownloadFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
