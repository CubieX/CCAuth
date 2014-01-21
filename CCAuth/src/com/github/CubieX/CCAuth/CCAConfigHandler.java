package com.github.CubieX.CCAuth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.github.CubieX.CCAuth.CCAuth;

public class CCAConfigHandler 
{   
   private final CCAuth plugin;
   private FileConfiguration config;
   private FileConfiguration playerListCfg = null;
   private File playerListFile = null;
   private final String playerListFileName = "playerList.yml";

   public CCAConfigHandler(CCAuth plugin) 
   {
      this.plugin = plugin;      

      initConfig();
   }

   private void initConfig()
   {
      plugin.saveDefaultConfig(); //creates a copy of the provided config.yml in the plugins data folder, if it does not exist
      config = plugin.getConfig(); //re-reads config out of memory. (Reads the config from file only, when invoked the first time!)

      // player list
      reloadPlayerListFile(); // load file from disk and create objects
      saveFireListDefaultFile(); // creates a copy of the provided fireList.yml
      reloadPlayerListFile(); // load file again after it is physically present now
   }

   public FileConfiguration getConfig()
   {
      return (config);
   }

   public synchronized void saveConfig() //saves the standard config to disc (needed when entries have been altered via the plugin in-game)
   {
      // get and set values here!
      plugin.saveConfig();
      plugin.reloadConfig();
   }

   //reloads all config files from disc (used if user made manual changes to the config.yml file)
   public void reloadConfig(CommandSender sender)
   {
      plugin.reloadConfig();
      plugin.readConfigValues();
      config = plugin.getConfig(); // new assignment necessary when returned value is assigned to a variable or static field(!)

      reloadPlayerListFile();
      sender.sendMessage(CCAuth.logPrefix + " " + plugin.getDescription().getVersion() + " reloaded!");      
   }

   // =========================
   // fireList file handling
   // =========================

   // reload from disk
   public void reloadPlayerListFile()
   {
      if (playerListFile == null)
      {
         playerListFile = new File(plugin.getDataFolder(), playerListFileName);
      }
      playerListCfg = YamlConfiguration.loadConfiguration(playerListFile);

      // Look for defaults in the jar
      InputStream defConfigStream = plugin.getResource(playerListFileName);
      if (defConfigStream != null)
      {
         YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
         playerListCfg.setDefaults(defConfig);
      }
   }

   // reload config and return it
   public FileConfiguration getPlayerListFile()
   {
      if (playerListCfg == null)
      {
         this.reloadPlayerListFile();
      }
      
      return playerListCfg;
   }

   //save config
   public synchronized void savePlayerListFile()
   {
      if (playerListCfg == null || playerListFile == null)
      {
         return;
      }
      try
      {
         playerListCfg.save(playerListFile);
         reloadPlayerListFile();
      }
      catch (IOException ex)
      {
         CCAuth.log.severe("Could not save data to " + playerListFile.getName());
         CCAuth.log.severe(ex.getMessage());
      }
   }

   // safe a default config if there is no file present
   public void saveFireListDefaultFile()
   {
      if (!playerListFile.exists())
      {            
         plugin.saveResource(playerListFileName, false);
      }
   }
}
