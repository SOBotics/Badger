package in.bhargavrao.stackoverflow.badger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.tunaki.stackoverflow.chat.ChatHost;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.MessagePostedEvent;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import org.sobotics.PingService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



/**
 * Created by bhargav.h on 21-Oct-16.
 */
public class RunBadger {

    private static final String apiKey = "kmtAuIIqwIrwkXm1*p3qqA((";
    private static final String docString = "[Badger](https://git.io/v1lYA)";

    private static String username = "bad";

    public static String dataFile = "./data/trackedBadges.txt";
    public static String propertiesFile = "./data/login.properties";
    public static Instant previousBadgeTimestamp = Instant.now();

    public static List<ScheduledExecutorService> services;

    public static void main(String[] args) {
        startApp();
    }

    private static void startApp() {
        StackExchangeClient client;
        Properties prop = new Properties();

        try {
            prop.load(new FileInputStream(propertiesFile));
            String email = prop.getProperty("email");
            String password = prop.getProperty("password");
            String redundaKey = prop.getProperty("redundaKey");
            username = prop.getProperty("username").substring(0,3).toLowerCase();
            int roomId = Integer.parseInt(prop.getProperty("roomId"));
            client = new StackExchangeClient(email, password);
            Room sobotics = client.joinRoom(ChatHost.STACK_OVERFLOW ,roomId);

            services = new ArrayList<>();

            PingService redunda = new PingService(redundaKey, "46c3719");
            redunda.start();

            sobotics.addEventListener(EventType.MESSAGE_REPLY, event->mention(sobotics, event, true, redunda));
            sobotics.addEventListener(EventType.USER_MENTIONED,event->mention(sobotics, event, false, redunda));
            sobotics.addEventListener(EventType.MESSAGE_POSTED ,event-> newMessage(sobotics, event, false));

            sobotics.send(docString+" started");

            boolean standbyMode = redunda.standby.get();
            System.out.println(standbyMode);
            startReporting(sobotics, redunda);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void startReporting(Room sobotics, PingService service) {
        List<String> lines = new ArrayList<String>();
        try {
            lines = Files.readAllLines(Paths.get(dataFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String i : lines) {
            String badgeId = i.split(",")[0];
            String badgeName = i.split(",")[1];
            Runnable printer = () -> printBadges(sobotics, badgeId, badgeName, service);
            executeApp(printer);
        }
    }

    private static void newMessage(Room room, MessagePostedEvent event, boolean b) {
        String message = event.getMessage().getPlainContent();
        int cp = Character.codePointAt(message, 0);
        if(message.trim().startsWith("@bots alive")){
            room.send("@Natty alive also? I'm alive");
        }
        else if (cp == 128642 || (cp>=128644 && cp<=128650)){
            CompletionStage<Long> messageId = room.send("@nat say \uD83D\uDE83");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            messageId.thenAccept(id -> room.edit(id, "\uD83D\uDE83"));
        }
    }

    private static void executeApp(Runnable printer) {
        /* Thanks to Tunaki */
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        services.add(executorService);
        executorService.scheduleAtFixedRate(printer, 0, 10, TimeUnit.MINUTES);
    }

    private static void stopApp(Room room, PingService service){
        room.send(docString+" stopped");
        for (ScheduledExecutorService e: services){
            e.shutdown();
        }
        room.leave();
    }

    private static void standbyApp(Room room, PingService service){
        room.send(docString+" on standby");
        for (ScheduledExecutorService e: services){
            e.shutdown();
        }
    }

    private static void rebootApp(Room room, PingService service){
        stopApp(room, service);
        startApp();
    }


    private static void printBadges(Room room, String badgeId, String badgeName, PingService service) {
        System.out.println(service.standby.get());
        if (!service.standby.get()) {
            int numberOfBadges = getBadgeCount(badgeId);
            if (numberOfBadges > 0) {
                room.send("[ " + docString + " ] " + numberOfBadges + " new " + prettyPrintBadge(badgeId, badgeName, numberOfBadges != 1));
            }
            previousBadgeTimestamp = Instant.now();
        }
    }

    private static String prettyPrintBadge(String badgeId, String badgeName, boolean plural) {
        return "[" + badgeName + " " + (plural ? "badges" : "badge") + "](//stackoverflow.com/help/badges/" + badgeId + ")";
    }

    private static int getBadgeCount(String badgeId){
        JsonArray badges;
        try {
            badges = getBadges(badgeId).get("items").getAsJsonArray();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return badges.size();
    }

    private static JsonObject getBadges(String badgeId) throws IOException{
        String badgeIdUrl = "https://api.stackexchange.com/2.2/badges/"+badgeId+"/recipients";
        return get(badgeIdUrl,"site","stackoverflow","pagesize","100","fromdate",String.valueOf(previousBadgeTimestamp.minusSeconds(1).getEpochSecond()),"key",apiKey);
    }

    private static boolean isNumeric(String str)
    {
        try{
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    public static void mention(Room room, PingMessageEvent event, boolean isReply, PingService service) {
        String message = event.getMessage().getPlainContent();
        if(message.toLowerCase().contains("help")){
            room.send("I'm a bot that tracks badges");
        }
        else if(message.toLowerCase().contains("reboot") &&
                (room.getUser(event.getUserId()).isModerator() || room.getUser(event.getUserId()).isRoomOwner())){
            rebootApp(room,service);
        }
        else if(message.toLowerCase().contains("standby") &&
                (room.getUser(event.getUserId()).isModerator() || room.getUser(event.getUserId()).isRoomOwner())){
            standbyApp(room,service);
        }
        else if(message.toLowerCase().contains("report") &&
                (room.getUser(event.getUserId()).isModerator() || room.getUser(event.getUserId()).isRoomOwner())){
            startReporting(room,service);
        }
        else if(message.toLowerCase().contains("alive")){
            room.send("Yep");
        }
        else if(message.toLowerCase().contains("commands")){
            room.send("    alive    - Test to check if the bot is alive or not.\n" +
                      "    help     - Returns description of the bot\n" +
                      "    commands - Returns this list of commands\n" +
                      "    track    - Tracks a given badge. Syntax: `track badgeId badgeName`\n" +
                      "    untrack  - UnTracks a given badge.\n" +
                      "    tracked  - Returns a list of tracked badges.");
        }
        else if(message.toLowerCase().contains("tracked")){
            List<String> lines = new ArrayList<>();
            try {
                lines = readFile(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String trackedList = "";
            for(String line: lines){
                trackedList+="    "+line.trim().split(",")[0]+" : "+line.trim().split(",")[1]+"\n";
            }
            room.send(trackedList);
        }
        else if(message.toLowerCase().contains("untrack")){
            if (!(room.getUser(event.getUserId()).isModerator() || room.getUser(event.getUserId()).isRoomOwner())){
                room.replyTo(event.getMessage().getId(),"You must be a room owner like @\u200Bpetter or @\u200Btuna, or a moderator, like @bhargav to run this command.");
                return;
            }
            String parts[] = message.toLowerCase().split(" ");
            List<String> lines = new ArrayList<>();
            if(parts.length==3 && parts[1].toLowerCase().equals("untrack")) {
                try {
                    lines = readFile(dataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<String> newLines = new ArrayList<>();
                String toBeUntracked = parts[2].trim().toLowerCase();

                for (String line : lines) {
                    if(isNumeric(toBeUntracked)) {
                        if (line.trim().toLowerCase().startsWith(toBeUntracked + ",")) {
                            room.send("Badge Number "+toBeUntracked+" is untracked");
                            continue;
                        }
                    }
                    else{
                        if (line.trim().toLowerCase().endsWith(","+toBeUntracked)) {
                            room.send("Badge "+toBeUntracked+" is untracked");
                            continue;
                        }
                    }
                    newLines.add(line);
                }
                try {
                    Files.write(Paths.get(dataFile), newLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                rebootApp(room, service);
            }
            else{
                room.send("Wrong format. The syntax is `untrack roomName|roomId`");
            }
        }
        else if(message.toLowerCase().contains("track")){
            String parts[] = message.split(" ");
            if(isReply){
                room.replyTo(event.getMessage().getId(),"It must be a mention and not a reply.");
                return;
            }
            if(parts.length>4){
                room.replyTo(event.getMessage().getId(),"The badge name must be a single word");
                return;
            }
            if (!(room.getUser(event.getUserId()).isModerator() || room.getUser(event.getUserId()).isRoomOwner())){
                room.replyTo(event.getMessage().getId(),"You must be a room owner like @\u200Bpetter or @\u200Btuna, or a moderator, like @bhargav to run this command.");
                return;
            }
            if(parts.length==4 &&
                    parts[0].toLowerCase().startsWith("@"+username) &&
                    parts[1].toLowerCase().equals("track") &&
                    isNumeric(parts[2])){

                String badgeId = parts[2];
                String badgeName = parts[3];
                String word = badgeId+","+badgeName;
                try{
                    Files.write(Paths.get(dataFile), Arrays.asList(word), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                Runnable printer = () -> printBadges(room, badgeId, badgeName, service);
                executeApp(printer);
                room.replyTo(event.getMessage().getId(),"Tracking " + prettyPrintBadge(badgeId,badgeName,true));
            }
            else {
                room.replyTo(event.getMessage().getId(),"Wrong format. The syntax is `track badgeId badgeName`");
            }
        }
    }
    public static JsonObject get(String url, String... data) throws IOException {
        /* Thanks to Tunaki */
        Connection.Response response = Jsoup.connect(url).data(data).method(Connection.Method.GET).ignoreContentType(true).ignoreHttpErrors(true).execute();
        String json = response.body();
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " fetching URL " + (url) + ". Body is: " + response.body());
        }
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        handleBackoff(root);
        return root;
    }
    public static void handleBackoff(JsonObject root) {
        /* Thanks to Tunaki */
        if (root.has("backoff")) {
            int backoff = root.get("backoff").getAsInt();
            try {
                Thread.sleep(1000 * backoff);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public static List<String> readFile(String filename) throws IOException{
        return Files.readAllLines(Paths.get(filename));
    }
}
