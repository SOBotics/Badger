package in.bhargavrao.stackoverflow.badger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bhargav.h on 21-Oct-16.
 */
public class RunBadger {

    private static final String apiKey = "kmtAuIIqwIrwkXm1*p3qqA((";
    private static final String docString = "[Badger](//codereview.stackexchange.com/questions/148570)";

    public static String dataFile = "./data/trackedBadges.txt";
    public static String propertiesFile = "./data/login.properties";
    public static Instant previousBadgeTimestamp = Instant.now();

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
            client = new StackExchangeClient(email, password);
            Room sobotics = client.joinRoom("stackoverflow.com" ,111347);

            sobotics.addEventListener(EventType.MESSAGE_REPLY, event->mention(sobotics, event, true));
            sobotics.addEventListener(EventType.USER_MENTIONED,event->mention(sobotics, event, false));

            sobotics.send(docString+" started");

            List<String> lines = new ArrayList<String>();
            try {
                lines = Files.readAllLines(Paths.get(dataFile));
            }
            catch (IOException e){
                e.printStackTrace();
            }
            for(String i: lines) {
                String badgeId = i.split(",")[0];
                String badgeName = i.split(",")[1];
                Runnable printer = () -> printBadges(sobotics, badgeId, badgeName);
                executeApp(printer);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void executeApp(Runnable printer) {
        /* Thanks to Tunaki */
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(printer, 0, 10, TimeUnit.MINUTES);
    }

    private static void printBadges(Room room, String badgeId, String badgeName) {
        JsonArray badges;
        try {
            badges = getBadges(badgeId).get("items").getAsJsonArray();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int numberOfBadges = badges.size();
        if(numberOfBadges!=0) {
            room.send("[ "+docString+" ] " + numberOfBadges + " new ["+badgeName+" badges](//stackoverflow.com/help/badges/"+badgeId+")");
        }
        previousBadgeTimestamp = Instant.now();
    }

    public static JsonObject getBadges(String badgeId) throws IOException{
        String badgeIdUrl = "https://api.stackexchange.com/2.2/badges/"+badgeId+"/recipients";
        return get(badgeIdUrl,"site","stackoverflow","pagesize","100","fromdate",String.valueOf(previousBadgeTimestamp.minusSeconds(1).getEpochSecond()),"key",apiKey);
    }

    public static boolean isNumeric(String str)
    {
        try{
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    public static void mention(Room room, PingMessageEvent event, boolean isReply) {
        String message = event.getMessage().getPlainContent();
        if(message.toLowerCase().contains("help")){
            room.send("I'm a bot that tracks badges");
        }
        else if(message.toLowerCase().contains("alive")){
            room.send("Yep");
        }
        else if(message.toLowerCase().contains("commands")){
            room.send("    alive    - Test to check if the bot is alive or not.\n" +
                      "    help     - Returns description of the bot\n" +
                      "    commands - Returns this list of commands\n" +
                      "    track    - Tracks a given badge. Syntax: `track badgeId badgeName`");
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
            if(parts.length==4 &&
                    parts[0].toLowerCase().startsWith("@bad") &&
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
                Runnable printer = () -> printBadges(room, badgeId, badgeName);
                executeApp(printer);
                room.replyTo(event.getMessage().getId(),"Tracking ["+badgeName+" badges](//stackoverflow.com/help/badges/"+badgeId+")");
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
        return root;
    }
}