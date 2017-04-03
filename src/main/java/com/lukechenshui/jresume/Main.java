package com.lukechenshui.jresume;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukechenshui.jresume.exceptions.InvalidEnvironmentVariableException;
import com.lukechenshui.jresume.exceptions.InvalidJSONException;
import com.lukechenshui.jresume.resume.Resume;
import com.lukechenshui.jresume.resume.items.Person;
import com.lukechenshui.jresume.resume.items.work.JobWork;
import com.lukechenshui.jresume.resume.items.work.VolunteerWork;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.tidy.Tidy;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static spark.Spark.*;

public class Main {
    static Config config;
    //Used to separate output folders generated by different requests.
    private static AtomicInteger outputPrefixNumber = new AtomicInteger(0);

    public static void main(String[] args) {
        try {
            config = new Config();
            new JCommander(config, args);
            if (Files.exists(Paths.get("data"))) {
                FileDeleteStrategy.FORCE.delete(new File("data"));
            }
            setupLogging();
            Files.createDirectory(Paths.get("data"));
            //createExample();

            if (config.serverMode) {
                if (config.sslMode) {
                    String keystoreLocation = Optional.ofNullable(System.getenv("jresume_keystore_location")).orElseThrow(
                            () -> new InvalidEnvironmentVariableException("jresume_keystore_location is not set in the environment"));

                    String keystorePassword = Optional.ofNullable(System.getenv("jresume_keystore_password")).orElseThrow(
                            () -> new InvalidEnvironmentVariableException("jresume_keystore_password is not set in the environment"));
                    File keystore = new File(keystoreLocation);
                    System.out.println("Keystore location:" + keystore.getAbsolutePath());
                    System.out.println("Keystore exists: " + keystore.exists());
                    System.out.println("Keystore can be read: " + keystore.canRead());
                    System.out.println("Keystore can write: " + keystore.canWrite());
                    System.out.println("Keystore can execute: " + keystore.canExecute());
                    secure(keystoreLocation, keystorePassword, null, null);
                }
                startListeningAsServer();
            } else {
                generateWebResumeAndWriteIt(null, new Runtime(config.getOutputDirectory(), outputPrefixNumber.incrementAndGet(), config), config.themeName);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        //createExample();
    }

    private static void setupLogging() throws FileNotFoundException {
        if (config.logFile != null) {
            PrintStream printStream = new PrintStream(new File(config.logFile));
            System.setErr(printStream);
            System.setOut(printStream);
        }
    }

    private static File generateWebResumeAndWriteIt(String json, Runtime runtime, String theme) throws Exception {
        if (json == null) {
            json = readJSONFromFile();
        }
        String html = generateWebResumeFromJSON(json, runtime, theme);
        File location = runtime.getOutputHtmlFile();
        FileWriter writer = new FileWriter(location, false);
        writer.write(html);
        //System.out.println(html);

        System.out.println("Success! You can find your resume at " + runtime.getOutputHtmlFile().getAbsolutePath());
        writer.close();
        return location;
    }


    public static void createExample(){
        Person person = new Person("John Doe", "Junior Software Engineer",
                "800 Java Road, OOP City", "+1(345)-335-8964", "johndoe@gmail.com",
                "http://johndoe.com");
        JobWork jobWork = new JobWork("Example Ltd.", "Software Engineer",
                "At Example Ltd., I did such and such.");

        jobWork.addHighlight("Worked on such and such");
        jobWork.addHighlight("Also worked on this");
        jobWork.addKeyWord("java");
        jobWork.addKeyWord("c++");
        jobWork.addKeyWord("c++");

        VolunteerWork volunteerWork = new VolunteerWork("Example Institution", "Volunteer",
                "At Example Institution, I did such and such.");
        Resume resume = new Resume();
        resume.setPerson(person);
        resume.addJobWork(jobWork);
        resume.addVolunteerWork(volunteerWork);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(resume);
        System.out.println(json);
        try(FileWriter writer = new FileWriter("example.json")){
            writer.write(json);
        }
        catch (Exception exc){
            exc.printStackTrace();
            stop();
        }
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });
    }

    private static void copyResourcesZip(Runtime runtime, File destination) throws Exception {
        String classUrl = Main.class.getResource("Main.class").toString();

        URL url = Main.class.getResource("/resources.zip");
        //System.out.println("JAR Resource Zip URL: " + url.toString());
        InputStream inputStream = url.openStream();

        if (destination == null) {
            File tempFile = new File("data/jresume-data-zip-" + runtime.getId());

            if (tempFile.exists()) {
                FileDeleteStrategy.FORCE.delete(tempFile);
            }
            Files.copy(inputStream, tempFile.toPath());
            runtime.unzipResourceZip(tempFile.getAbsolutePath());
            FileDeleteStrategy.FORCE.delete(tempFile);
        } else {
            if (!destination.exists()) {
                Files.copy(inputStream, destination.toPath());
            }

        }


    }

    private static void startListeningAsServer() throws Exception {
        copyResourcesZip(new Runtime(new File("."), -1, config), config.serverInitialResourceZip);
        threadPool(config.getMaxThreads());
        port(config.getServerPort());
        enableCORS("*", "POST, GET, OPTIONS, DELETE, PUT", "*");

        get("resources", (request, response) -> {
            HttpServletResponse rawResponse = response.raw();
            rawResponse.setContentType("application/octet-stream");
            rawResponse.setHeader("Content-Disposition", "attachment; filename=resume.zip");
            OutputStream out = rawResponse.getOutputStream();

            writeFiletoOutputStreamByteByByte(config.serverInitialResourceZip, out);
            return rawResponse;
        });

        post("/webresume", "application/json", (request, response) -> {
            return generateWebResumeInRoute("default", request, response);
        });

        post("/webresume/:theme", (request, response) -> {
            String desiredTheme = request.params(":theme");
            ArrayList<String> themes = getListOfThemes();
            if (themes.contains(desiredTheme)) {
                return generateWebResumeInRoute(desiredTheme, request, response);
            } else {
                response.type("application/json");
                response.status(400);

                JSONObject responseObj = new JSONObject();
                responseObj.put("error", "The theme you have selected does not exist");

                return responseObj.toString();
            }
        });

        get("/", (request, response) -> {
            return "Welcome to JResume!";
        });

        get("/themes", (request, response) -> {

            response.type("application/json");
            JSONObject responseObj = new JSONObject();
            JSONArray themeArr = new JSONArray();
            File themeFolder = new File("themes");
            for (String themeName : themeFolder.list()) {
                themeArr.put(FilenameUtils.getBaseName(themeName));
            }
            responseObj.put("themes", themeArr);
            return responseObj.toString();
        });
        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
        });
    }

    private static Object generateWebResumeInRoute(String theme, Request request, Response response) throws Exception {
        int currentReqId = outputPrefixNumber.incrementAndGet();
        File outputDirectory = new File("data/jresume" + currentReqId + ".tmp");
        Runtime runtime = new Runtime(outputDirectory, currentReqId, config);
        File location = generateWebResumeAndWriteIt(request.body(), runtime, theme);
        HttpServletResponse rawResponse = response.raw();
        rawResponse.setContentType("text/html");
        OutputStream out = rawResponse.getOutputStream();
        writeFiletoOutputStreamByteByByte(location, out);
        FileDeleteStrategy.FORCE.delete(outputDirectory);

        return rawResponse;
    }

    public static ArrayList<String> getListOfThemes() {
        ArrayList<String> themes = new ArrayList<>();
        File themeFolder = new File("themes");
        for (String themeName : themeFolder.list()) {
            themes.add(FilenameUtils.getBaseName(themeName));
        }
        return themes;
    }

    private static String generateWebResumeFromJSON(String json, Runtime runtime, String theme) throws Exception {
        if (!isValidJSON(json)) {
            throw new InvalidJSONException();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();



        copyResourcesZip(runtime, null);
        if (!Files.exists(Paths.get("output"))) {
            Files.createDirectory(Paths.get("output"));
        }

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setDirectoryForTemplateLoading(new File("themes"));
        cfg.setDefaultEncoding("UTF-8");
        Template temp = cfg.getTemplate(theme + ".html");
        //System.out.println(json);
        Resume resume = gson.fromJson(json, Resume.class);
        resume.getRidOfArraysWithEmptyElements();
        resume.setConfig(config);
        StringWriter htmlStringWriter = new StringWriter();
        temp.process(resume, htmlStringWriter);
        String html = htmlStringWriter.toString();
        return html;
    }

    private static String prettyPrintHTML(String html) {
        String prettyHTML;
        Tidy tidy = new Tidy();
        tidy.setIndentContent(true);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
        tidy.setTrimEmptyElements(false);

        StringReader htmlStringReader = new StringReader(html);
        StringWriter htmlStringWriter = new StringWriter();
        tidy.parseDOM(htmlStringReader, htmlStringWriter);
        prettyHTML = htmlStringWriter.toString();
        return prettyHTML;
    }

    private static String readJSONFromFile() throws Exception {
        String jsonResumePath = config.getInputFileName();
        String json = "";
        Scanner reader = new Scanner(new File(jsonResumePath));

        while (reader.hasNextLine()) {
            json += reader.nextLine();
            json += "\n";
        }
        reader.close();
        return json;
    }

    private static void writeFiletoOutputStreamByteByByte(File file, OutputStream out) throws IOException{
        FileInputStream input = new FileInputStream(file);
        int c;
        while((c = input.read()) != -1){
            out.write(c);
        }
        input.close();
        out.close();
    }

    private static boolean isValidJSON(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return true;
        } catch (JSONException exc) {
            exc.printStackTrace();
            System.out.println("Invalid JSON:" + json);
            return false;
        }
    }
}
