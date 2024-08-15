package com.storedobject.idea;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

final class Service {

    private static Project project;
    private static Service service;
    private final static int commOK = 0;
    private final static int commERROR = 1;
    private final static int commDISCONNECT = 2;
    private final static int commLOGIN = 3;
    private final static int commDEVELOPER = 5;
    private final static int commLOAD = 50;
    private final static int commLIST = 51;
    private final static int commSAVE = 52;
    private String host, login, password, session;
    private URLConnection url;
    private Writer writer;
    private boolean more;
    private Reader reader;
    private boolean loggedIn = false;

    private Service() {
    }

    public static Service get() {
        if(service == null) {
            service = new Service();
        }
        return service;
    }

    void setHost(String host, String login, String password) {
        if(url != null) {
            if(Objects.equals(this.host, host) && Objects.equals(this.login, login) && Objects.equals(this.password, password)) {
                return;
            }
            close();
        }
        this.host = host;
        this.login = login;
        this.password = password;
    }

    static boolean noConnection() {
        Login login = Login.get();
        Service service = get();
        if(login.login()) {
            try {
                if (!service.loggedIn) {
                    if (!service.login()) {
                        service.close();
                        Action.message("Login failed!");
                    }
                }
            } catch (Exception e) {
                service.close();
                Action.message("Error connecting to Server: " + service.login + "@" + service.host);
            }
            return !service.loggedIn;
        }
        return true;
    }

    static boolean changeProject(Project project) {
        if(Service.project == project) {
            return false;
        }
        if(Service.project != null) {
            service.disconnect();
        }
        Service.project = project;
        return true;
    }

    static Project getProject() {
        return project;
    }

    String getHost() {
        return host;
    }

    String getLogin() {
        return login;
    }

    String getPassword() {
        return password;
    }

    private boolean login() {
        try {
            action(commLOGIN);
            startReading();
            int code = getInt();
            if(code != commOK) {
                if(code == commDEVELOPER) {
                    Action.notify("Not a developer", "Login", NotificationType.ERROR);
                }
                Login.get().logOff();
                return false;
            }
        } catch(Exception e) {
            close();
            Login.get().logOff();
            return false;
        }
        loggedIn = true;
        return true;
    }

    private void createConnection() throws IOException {
        Login login = Login.get();
        if(!login.login()) {
            throw new IOException("Not logged in!");
        }
        more = false;
        while (!host.isEmpty() && host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        URL u;
        try {
            u = new URI(host + "DEVELOPER").toURL();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        url = u.openConnection();
        if(session != null) {
            url.setRequestProperty("Cookie","JSESSIONID=" + session);
        }
        url.setDoInput(true);
        url.setDoOutput(true);
        writer = new BufferedWriter(new OutputStreamWriter(url.getOutputStream(), StandardCharsets.UTF_8));
    }

    private void startReading() throws IOException {
        writer.flush();
        reader = new BufferedReader(new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8));
        session = getString();
    }

    private void action(int action) throws IOException {
        createConnection();
        put("a", action);
        int version = 23;
        put("v", version);
        put("u", login);
        put("p", password);
    }

    String getServerInfo() {
        StringBuilder s = new StringBuilder("Project: ");
        Project project = Service.getProject();
        if(project == null) {
            s.append("<Not set>");
        } else {
            s.append(project.getName());
        }
        s.append("\nSource Folder: ");
        s.append(Action.getJavaFolderName());
        s.append("\nResource Folder: ");
        s.append(Action.getResourceFolderName());
        s.append("\nServer: ");
        if(host == null) {
            s.append("Not connected!");
            return s.toString();
        }
        if(login != null) {
            s.append(login).append('@');
        }
        s.append(host);
        if(url == null) {
            s.append(" (Not connected!)");
        }
        return s.toString();
    }

    void switchServer() {
        if(!loggedIn) {
            Login.get().logOff();
            return;
        }
        try {
            action(commDISCONNECT);
            startReading();
        } catch (Exception ignored) {
        } finally {
            close();
        }
        loggedIn = false;
        Login.get().logOff();
    }

    void disconnect() {
        switchServer();
        login = null;
        password = null;
        url = null;
    }

    private void close() {
        try {
            reader.close();
        } catch (Exception ignored) {
        }
        try {
            writer.close();
        } catch (Exception ignored) {
        }
        url = null;
    }

    private void put(String parameter, Object data) throws IOException {
        if(data != null && !(data instanceof String)) {
            data = data.toString();
        }
        if(data == null) {
            data = "";
        }
        String s = URLEncoder.encode(data.toString(), StandardCharsets.UTF_8);
        if(more) {
            writer.write("&");
        } else {
            more = true;
        }
        writer.write(parameter);
        writer.write("=");
        writer.write(s);
    }

    private int getInt() throws IOException {
        try {
            return Integer.parseInt(getString());
        } catch(IOException ioException) {
            throw ioException;
        } catch(Throwable e) {
            throw new IOException("Not an int");
        }
    }

    private String getString() throws IOException {
        int n = reader.read();
        char first = (char) n;
        if(n == -1) {
            throw new IOException("Unexpected EOS(1)");
        }
        int m = reader.read();
        char second = (char) m;
        if(m == -1) {
            throw new IOException("Unexpected EOS(2)");
        }
        n = n | (m << 16);
        if(n < 0) {
            throw new IOException(n + " < 0");
        }
        if(n == 0) {
            return "";
        }
        char[] cb = new char[n];
        int r;
        m = 0;
        while(m < n) {
            r = reader.read(cb, m, n - m);
            if(r == -1) {
                throw new IOException(m + " != " + n + "\n" + first + second + new String(cb, 0, m));
            }
            m += r;
        }
        return new String(cb);
    }

    class Stream extends InputStream {

        private int c;

        private Stream() throws IOException {
            c = r();
        }

        @Override
        public int read() throws IOException {
            if(c == -1) {
                return c;
            }
            int pc = c;
            c = r();
            return pc;
        }

        private int r() throws IOException {
            c = reader.read();
            if(c == 0) {
                c = -1;
            }
            return c;
        }
    }

    InputStream load(String fileName) throws Exception {
        action(commLOAD);
        put("f", fileName);
        startReading();
        if(getInt() != commOK) {
            return null;
        }
        return new Stream();
    }

    Reader send(String fileName, String content) throws IOException {
        if(fileName.endsWith(".java")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        action(commSAVE);
        put("f", fileName);
        put("c", content);
        startReading();
        int result = getInt();
        if(result == commOK) {
            return null;
        }
        if(result != commERROR) {
            throw new IOException("Unknown command " + result);
        }
        return new InputStreamReader(new Stream(), StandardCharsets.UTF_8);
    }

    ArrayList<String> list(String fileNamePart) throws Exception {
        fileNamePart = toDBFileName(fileNamePart);
        ArrayList<String> v = new ArrayList<>();
        action(commLIST);
        put("f", fileNamePart);
        startReading();
        if(getInt() != commOK) {
            throw new Exception();
        }
        String s;
        while(true) {
            s = getString();
            if(s.isEmpty()) {
                break;
            }
            v.add(s);
        }
        return v;
    }

    private static String toDBFileName(String fileName) {
        if(fileName.contains("(")) {
            fileName = toFileName(fileName);
        }
        int p = fileName.indexOf(".java");
        if(p > 0) {
            fileName = fileName.substring(0, p);
        }
        String s = "src" + File.separatorChar;
        if(fileName.startsWith(s)) {
            fileName = fileName.substring(s.length());
        }
        return fileName.replace(File.separatorChar, '.');
    }

    private static String toFileName(String displayName) {
        int p = displayName.indexOf('(');
        if(p > 0) {
            displayName = (displayName.substring(p + 1, displayName.length() - 1) + "." + displayName.substring(0, p - 1));
        }
        return "src" + File.separatorChar + displayName.replace('.', File.separatorChar) + ".java";
    }
}