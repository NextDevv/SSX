package sharppvp.plugins.plugin.Service;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sharppvp.plugins.plugin.SSX;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigManager {
    private static File file;
    private static FileConfiguration customFile;

    private static File file2;
    private static FileConfiguration customFile2;

    private static File subdir;


    private static SSX plugin = SSX.getPlugin(SSX.class);

    //Finds or generates the custom config file
    public static void setup(){
        file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()){
            try{
                file.createNewFile();
                plugin.saveResource("messages.yml",true);
            }catch (IOException ignored){}
        }
        customFile = YamlConfiguration.loadConfiguration(plugin.getResource("messages.yml"));

        subdir = new File(plugin.getDataFolder().getPath() + System.getProperty("file.separator") + "Data");
        subdir.mkdir();

        file2 = new File(subdir.getPath() + System.getProperty("file.separator"), "location.yml");

        if(!file2.exists()) {
            try{
                file2.createNewFile();
                plugin.saveResource(file2.getPath(), true);
            }catch (IOException ignored){}
        }

        customFile2 = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("location.yml")));
    }

    public static FileConfiguration get(){
        return customFile;
    }

    public static FileConfiguration getLocationData(){
        return customFile2;
    }

    public static void save(){
        try{
            customFile.save(file);
        }catch (IOException e){
            System.out.println("Couldn't save file");
        }
    }

    public static void saveLocationData() {
        try {
            customFile2.save(file2);
        }catch (IOException e){
            System.out.println("Couldn't save file");
        }
    }

    public static void reloadLocationData() {
        if (file2 == null) {
            file2 = new File(subdir.getPath() + System.getProperty("file.separator"), "location.yml");
        }
        customFile2 = YamlConfiguration.loadConfiguration(file);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(plugin.getResource("location.yml"), "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customFile2.setDefaults(defConfig);
        }
    }

    public void reloadCustomConfig() throws UnsupportedEncodingException {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "messages.yml");
        }
        customFile = YamlConfiguration.loadConfiguration(file);

        // Look for defaults in the jar
        Reader defConfigStream = new InputStreamReader(plugin.getResource("messages.yml"), "UTF8");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customFile.setDefaults(defConfig);
        }
    }

    public static void saveDefaultLocation() {
        Reader stream;

        try {

            stream = new InputStreamReader(plugin.getResource("location.yml"), StandardCharsets.UTF_8);

            if (stream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(stream);
                customFile2 = YamlConfiguration.loadConfiguration(file2);
                customFile2.setDefaults(defConfig);
                stream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveDefaultConfig() {
        Reader stream;

        try {

            stream = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8);

            if (stream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(stream);
                customFile = YamlConfiguration.loadConfiguration(file);
                customFile.setDefaults(defConfig);
                stream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reloadMessages(){
        customFile = YamlConfiguration.loadConfiguration(file);
    }
}
