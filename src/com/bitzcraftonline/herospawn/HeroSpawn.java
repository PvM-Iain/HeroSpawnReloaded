package com.bitzcraftonline.herospawn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HeroSpawn extends JavaPlugin {
  public static boolean firstRun = false;
  public static HeroSpawn instance;
  public static Logger logger = Logger.getLogger("Minecraft");
  public static String PREFIX;
private HashMap<World, SpawnPoint> spawnPoints = new HashMap<World, SpawnPoint>();

  public HeroSpawn() {
    instance = this;
  }

  public void onEnable()
  {
    firstRun = hasRun();
    new HeroSpawnListener();
    PREFIX = "[HeroSpawn v" + getDescription().getVersion() + "] ";
    checkWorlds();
    initSpawnPoints();
    startMetrics();
    super.onEnable();
  }

  public void startMetrics() {
    try {
      new Metrics();
    }
    catch (IOException localIOException)
    {
    }
  }

  public void onDisable() {
    super.onDisable();
  }

  public void log(Level level, String msg) {
    logger.log(level, "[HeroSpawn v" + getDescription().getVersion() + "] " + 
      msg);
  }

  public boolean hasRun()
  {
    if (getDataFolder().exists()) {
      return false;
    }

    log(Level.INFO, "First run, Generating player files... ");
    getDataFolder().mkdir();
    addPlayers();
    log(Level.INFO, "Completed First Time Setup, Enjoy :)");
    return true;
  }

  private void checkWorlds()
  {
    BufferedWriter out = null;
    for (int i = 0; i < getServer().getWorlds().size(); i++) {
      String worldName = ((World)getServer().getWorlds().get(i)).getName();
      try
      {
        File dir = new File("plugins/HeroSpawn/" + worldName);
        if (!dir.exists()) {
          log(Level.INFO, "Generating players.txt file for : " + worldName);
          dir.mkdir();

          File playersFile = new File(dir.getPath() + "/players.txt");
          playersFile.createNewFile();

          out = new BufferedWriter(new FileWriter("plugins/HeroSpawn/" + worldName + "/players.txt", true));

          File playersDir = new File(worldName + "/players");

          for (int j = 0; j < playersDir.listFiles().length; j++) {
            String fileName = playersDir.listFiles()[j].getName();

            if (fileName.endsWith(".dat")) {
              String playerName = playersDir.listFiles()[j].getName().substring(0, playersDir.listFiles()[j].getName().lastIndexOf("."));
              out.write(playerName);
              out.newLine();
            }
          }

          out.close();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void addPlayers()
  {
    BufferedWriter out = null;

    for (int i = 0; i < getServer().getWorlds().size(); i++) {
      String worldName = ((World)getServer().getWorlds().get(i)).getName();
      log(Level.INFO, "Generating players.txt file for : " + worldName);
      try
      {
        File dir = new File("plugins/HeroSpawn/" + worldName);
        dir.mkdir();

        File playersFile = new File(dir.getPath() + "/players.txt");
        playersFile.createNewFile();

        out = new BufferedWriter(new FileWriter("plugins/HeroSpawn/" + worldName + "/players.txt", true));

        File playersDir = new File(worldName + "/players");

        for (int j = 0; j < playersDir.listFiles().length; j++) {
          String fileName = playersDir.listFiles()[j].getName();

          if (fileName.endsWith(".dat")) {
            String playerName = playersDir.listFiles()[j].getName().substring(0, playersDir.listFiles()[j].getName().lastIndexOf("."));
            out.write(playerName);
            out.newLine();
          }
        }

        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void addPlayer(Player player)
  {
    String playerName = player.getName();
    String worldName = player.getWorld().getName();
    try {
      FileWriter fstream = new FileWriter("plugins/HeroSpawn/" + worldName + "/players.txt", true);
      BufferedWriter fbw = new BufferedWriter(fstream);
      fbw.write(playerName);
      fbw.newLine();
      fbw.close();
    } catch (IOException e) {
      checkWorlds();
      addPlayer(player);
    }
  }

  public boolean hasJoined(Player player) {
    String playerName = player.getName();
    String worldName = player.getWorld().getName();
    try
    {
      FileInputStream fstream = new FileInputStream("plugins/HeroSpawn/" + worldName + "/players.txt");
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strln;
      while ((strln = br.readLine()) != null)
      {
        if (strln.equalsIgnoreCase(playerName))
          return true;
      }
    }
    catch (IOException e)
    {
      checkWorlds();
    }
    return false;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (!(sender instanceof Player)) {
      sender.sendMessage("§cYou can only use this command in-game!");
      return false;
    }
    Player player = (Player)sender;
    if ((!player.hasPermission("herospawn.set")) || (!player.hasPermission("herospawn.tp"))) {
      player.sendMessage("§cYou don't have permission to do that!");
      return false;
    }
    if ((args.length == 0) && (
      (player.hasPermission("herspawn.set")) || (player.hasPermission("herospawn.tp")))) {
      sender.sendMessage("§e------------------- §fHeroSpawn Help§e -------------------");
      if (player.hasPermission("herospawn.set")) {
        sender.sendMessage("§e-- §9/herospawn set §e-§f Sets the HeroSpawn to your location  §e--");
      }
      if (player.hasPermission("herspawn.tp")) {
        sender.sendMessage("§e-- §9/herospawn tp [world] §e-§f Teleport to a herospawn         §e--");
      }
      sender.sendMessage("§e--    Rewritten by PvM_Iain for Bitzcraft    §e--");
      sender.sendMessage("§e-----------------------------------------------------");
      return false;
    }

    if ((args[0].equalsIgnoreCase("set")) && 
      (player.hasPermission("herospawn.set"))) {
      saveData(player);
      sender.sendMessage("§9[§eHeroSpawn§9] §eThe HeroSpawn of '" + player.getWorld().getName() + "' has been set");
      return true;
    }

    if ((args[0].equalsIgnoreCase("tp")) && 
      (player.hasPermission("herospawn.tp"))) {
      if (args.length == 1) {
        player.teleport(((SpawnPoint)this.spawnPoints.get(player.getWorld())).getLocation());
        player.sendMessage("§9[§eHeroSpawn§9] §eTeleported to the HeroSpawn of '" + player.getWorld().getName() + "'");
      } else {
        World world = getServer().getWorld(args[1]);
        if (world != null) {
          player.teleport(((SpawnPoint)this.spawnPoints.get(world)).getLocation());
          player.sendMessage("§9[§eHeroSpawn§9] §eTeleported to the HeroSpawn of '" + player.getWorld().getName() + "'");
        } else {
          player.sendMessage("§f'" + args[1] + "'§c is not a valid world");
        }
      }
    }

    return true;
  }

  public void saveData(Player player)
  {
    double x = player.getLocation().getX();
    double y = player.getLocation().getY();
    double z = player.getLocation().getZ();
    double yaw = player.getLocation().getYaw();
    double pitch = player.getLocation().getPitch();

    File data = new File(getDataFolder() + "/" + player.getWorld().getName(), "data.yml");
    if (!data.exists()) {
      try {
        data.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    FileConfiguration dataC = new YamlConfiguration();
    try
    {
      dataC.load(data);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
    String path = "Location.";

    dataC.set(path + "x", Double.valueOf(x));
    dataC.set(path + "y", Double.valueOf(y));
    dataC.set(path + "z", Double.valueOf(z));
    dataC.set(path + "yaw", Double.valueOf(yaw));
    dataC.set(path + "pitch", Double.valueOf(pitch));
    try
    {
      dataC.save(data);
    } catch (IOException e) {
      e.printStackTrace();
    }

    ((SpawnPoint)this.spawnPoints.get(player.getWorld())).load();
  }

  public SpawnPoint getHeroSpawn(World world)
  {
    SpawnPoint spawn = (SpawnPoint)this.spawnPoints.get(world);
    return spawn;
  }

  private void initSpawnPoints() {
    for (int i = 0; i < getServer().getWorlds().size(); i++) {
      World w = (World)getServer().getWorlds().get(i);
      SpawnPoint s = new SpawnPoint(w);
      s.load();
      this.spawnPoints.put(w, s);
    }
  }
}