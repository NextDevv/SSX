package sharppvp.plugins.plugin.Service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import sharppvp.plugins.plugin.SSX;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DataBase {
    private static Connection connection;
    static String prefix = "[SSX] (DataBase) ";

    static SSX plugin = SSX.getPlugin(SSX.class);
    static Configuration config = plugin.getConfig();
    public static Connection getConnection() {
        if(connection != null) {
            return connection;
        }
        String host = plugin.getConfig().getString("host");
        String name = plugin.getConfig().getString("name");
        String username = plugin.getConfig().getString("user");
        String password = plugin.getConfig().getString("password");
        if(Objects.equals(host, "")) {
            plugin.getLogger().warning("The database info is empty plase fill it.");
            return null;
        }

        String url = "jdbc:mysql://"+host+"/"+name+"?characterEncoding=utf8";
        String url2 = "jdbc:mysql://u16337_NHjun5v66k:4tINWb43fMdCer8b^@E7d.!!@sql1.revivenode.com:3306/s16337_HiddenChests?characterEncoding=utf8";

        try {
            connection = DriverManager.getConnection(url, username, password);

            System.out.println(prefix+"Connected to the database");


        } catch (SQLException e) {
            System.out.println(prefix+"Failed to connect to the database: " + e.getMessage());
        }

        return connection;
    }

    public static void InitilizeDatabase() {
        try {
            Statement statement = getConnection().createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS SSLocation(Id int, locX double, locY double, locZ double, world text);";
            statement.execute(sql);

            System.out.println(prefix+"Successfully created the table in the database");
            statement.close();
        } catch (SQLException e) {
            System.out.println(prefix+"Failed to create the table in the database: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static Location getSSLocation(int ID) {
        try {
            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM SSLocation WHERE Id = ?");
            statement.setInt(1, ID);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String id = rs.getString("Id");
                double x = rs.getDouble("locX");
                double y = rs.getDouble("locY");
                double z = rs.getDouble("locZ");
                String world = rs.getString("world");



                statement.close();

                return new Location(Bukkit.getWorld(world), x, y, z);
            }

        } catch (SQLException e) {
            System.out.println(prefix+"Unable to get player data: " + e.getMessage());
            System.out.println(prefix+"At: " + e.getErrorCode());
            e.printStackTrace();
        }

        return null;
    }


    public void createSSlocation(int d, Location loc) {
        try {
            PreparedStatement statement = getConnection()
                    .prepareStatement("INSERT INTO SSLocation(Id, locX, locY, locZ, world) VALUES(?,?,?,?,?)");
            statement.setInt(1, d);
            statement.setDouble(2, loc.getX());
            statement.setDouble(3, loc.getY());
            statement.setDouble(4, loc.getZ());
            statement.setString(5, loc.getWorld().getName());

            statement.executeUpdate();

            statement.close();
        } catch (SQLException e) {
            System.out.println(prefix+"Unable to create ss location database: " + e.getMessage());
            System.out.println(prefix+"Error code: " + e.getErrorCode());
            System.out.println(prefix+"Cause: "+e.getCause());
        }

    }





    public void updateSSLocation(int d, Location loc) {
        try {
            PreparedStatement statement = getConnection()
                    .prepareStatement("UPDATE SSLocation SET LocX = ?, LocY = ?, LocZ = ?, world = ? WHERE Id = ?");

            statement.setDouble(1, loc.getX());
            statement.setDouble(2, loc.getY());
            statement.setDouble(3, loc.getZ());
            statement.setString(4, loc.getWorld().getName());
            statement.setInt(5, d);

            statement.executeUpdate();

            statement.close();
        } catch (SQLException e) {
            System.out.println(prefix+"Unable to update player database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteSSLocation(int d) {
        try {
            PreparedStatement statement = getConnection()
                    .prepareStatement("DELETE FROM SSLocation WHERE Id = ?");
            statement.setInt(1, d);

            statement.executeUpdate();

            statement.close();
        }catch (SQLException e) {
            System.out.println(prefix+"Unable to delete player database: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            getConnection().close();
        }catch (SQLException e) {
            System.out.println(prefix+"Unable to close connection: " + e.getMessage());
        }
    }
}
