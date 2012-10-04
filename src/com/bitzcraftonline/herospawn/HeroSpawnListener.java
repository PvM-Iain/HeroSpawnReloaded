package com.bitzcraftonline.herospawn;


import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;


public class HeroSpawnListener
  implements Listener
{
  HeroSpawn plugin;

  public HeroSpawnListener()
  {
    this.plugin = HeroSpawn.instance;
    Bukkit.getServer().getPluginManager().registerEvents(this, this.plugin);
  }

  @EventHandler(priority=EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    boolean hasJoined = false;
    hasJoined = HeroSpawn.instance.hasJoined(player);
    if (!hasJoined)
    {
      this.plugin.addPlayer(player);
      Location herospawn = this.plugin.getHeroSpawn(player.getWorld()).getLocation();
      player.teleport(herospawn);
      this.plugin.log(Level.INFO, event.getPlayer().getName() + " has joined the server for the first time!");
    }
  }

  @EventHandler(priority=EventPriority.HIGH)
  public void onWorldChange(PlayerChangedWorldEvent event)
  {
    Player player = event.getPlayer();
    boolean hasJoined = false;
    hasJoined = HeroSpawn.instance.hasJoined(player);
    if (!hasJoined)
    {
      this.plugin.addPlayer(player);
      Location herospawn = this.plugin.getHeroSpawn(player.getWorld()).getLocation();
      player.teleport(herospawn);
      this.plugin.log(Level.INFO, event.getPlayer().getName() + " has joined the server for the first time!");
    }
  }
}