package com.bitzcraftonline.herospawn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class SpawnPoint
{
  private World world;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;
  public SpawnPoint(World world)
  {
    this.world = world;
    this.x = world.getSpawnLocation().getX();
    this.y = world.getSpawnLocation().getY();
    this.z = world.getSpawnLocation().getZ();
    this.yaw = world.getSpawnLocation().getYaw();
    this.pitch = world.getSpawnLocation().getPitch();
  }

  public void load()
  {
    File data = new File(HeroSpawn.instance.getDataFolder() + "/" + this.world.getName(), "data.yml");
    if (!data.exists()) {
      return;
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

    this.x = dataC.getDouble(path + "x");
    this.y = dataC.getDouble(path + "y");
    this.z = dataC.getDouble(path + "z");
    this.yaw = dataC.getInt(path + "yaw");
    this.pitch = dataC.getInt(path + "pitch");
  }

  public Location getLocation()
  {
    return new Location(this.world, this.x, this.y, this.z, this.yaw, this.pitch);
  }
}