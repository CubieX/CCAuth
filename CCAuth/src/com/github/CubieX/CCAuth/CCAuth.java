/*
 * CCAuth - A CraftBukkit plugin that can query a remote DB via HTTP request by calling a PHP script via AES encrypted protocol
 * to check players against the forum profile.
 * Can also get the players Mojang UUID.
 * Copyright (C) 2014  CubieX
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not,
 * see <http://www.gnu.org/licenses/>.
 */
package com.github.CubieX.CCAuth;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.edawg878.identifier.IdentifierAPI;
import com.github.CubieX.CCAuth.CCAuth;
import com.github.CubieX.CCAuth.CmdHandler.CCAccaCmdHandler;
import com.github.CubieX.CCAuth.CmdHandler.CCAactivationCmdHandler;

public class CCAuth extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   public static final String logPrefix = "[CCAuth] "; // Prefix to go in front of all log entries
   public static final int MAX_RETRIEVAL_TIME = 2000;  // max time in ms to wait for an async PlayerUUID request to deliver its result
   // This prevents async task jam in case HTTP is unreachable or connection is very slow   
   private CCAccaCmdHandler ccaComHandler = null;
   private CCAactivationCmdHandler registerComHandler = null;
   private CCAConfigHandler cHandler = null;
   private CCAHTTPHandler httpHandler = null;
   private CCAEntityListener eListener = null;
   private CCASchedulerHandler schedHandler = null;
   private IdentifierAPI identAPI = null;
   private final int contentLinesPerPage = 10;

   // config values
   public static boolean debug = false;
   public static String forumURL = "";

   // HTTP GET request
   static String scriptURL = " ";           // main URL of HTTP gateway to send the request to
   // AES config
   static String secretKey = "01234567890ABCDE"; // must have exactly 16 HEX chars (AES 128) or optional 32 HEX chars (AES 256)

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {            
      cHandler = new CCAConfigHandler(this);

      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      readConfigValues();

      identAPI = new IdentifierAPI(); // FIXME ins Plugin includen oder so machen, dass Bukkit sie findet beim start?? (lib-Verzeichnis z.B.)???
      schedHandler = new CCASchedulerHandler(this, cHandler, identAPI);
      httpHandler = new CCAHTTPHandler(this, schedHandler, cHandler, identAPI);
      eListener = new CCAEntityListener(this);            
      ccaComHandler = new CCAccaCmdHandler(this, cHandler, httpHandler);
      registerComHandler = new CCAactivationCmdHandler(this, httpHandler);      
      getCommand("cca").setExecutor(ccaComHandler);
      getCommand("activate").setExecutor(registerComHandler);

      log.info(logPrefix + " version " + getDescription().getVersion() + " is enabled!");
   }   

   private boolean checkConfigFileVersion()
   {      
      boolean configOK = false;     

      if(cHandler.getConfig().isSet("config_version"))
      {
         String configVersion = getConfig().getString("config_version");

         if(configVersion.equals(usedConfigVersion))
         {
            configOK = true;
         }
      }

      return (configOK);
   }  

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      if(getConfig().isSet("debug")){debug = getConfig().getBoolean("debug");}else{invalid = true;}
      if(getConfig().isSet("forumURL")){forumURL = getConfig().getString("forumURL");}else{invalid = true;}

      // HTTP request
      if(getConfig().isSet("scriptURL")){scriptURL = getConfig().getString("scriptURL");}else{invalid = true;}
      if(getConfig().isSet("secretKey")){secretKey = getConfig().getString("secretKey");}else{invalid = true;}

      if(exceed)
      {
         log.warning(logPrefix + "One or more config values are exceeding their allowed range. Please check your config file!");
      }

      if(invalid)
      {
         log.warning(logPrefix + "One or more config values are invalid. Please check your config file!");
      }
   }

   @Override
   public void onDisable()
   {
      this.getServer().getScheduler().cancelTasks(this);
      identAPI = null;
      cHandler = null;
      eListener = null;
      ccaComHandler = null;
      registerComHandler = null;
      httpHandler = null;
      schedHandler = null;
      log.info(logPrefix + "version " + getDescription().getVersion() + " is disabled!");
   }

   // #########################################################

   /**
    * Gets player UUID form Mojang servers and checks if this player is registered in the configured Forums DB.
    * CAUTION: This may deliver no UUID if more than one player with the same name are registered at Mojang!
    * 
    * @param sender The querying player or console
    * @param checkedPlayersName The player name to get he UUID for
    *     
    * */
   public void isPlayerRegisteredInForum(CommandSender sender, String checkedPlayersName)
   {
      schedHandler.isPlayerRegisteredInForum(sender, checkedPlayersName);
   }

   /**
    * <b>Send message to player synchronously</b><br>   
    * Use this for sending messages to a player from out an async task
    * 
    * @param player The player to send the message to
    * */
   public void sendSyncChatMessage(Player player, String message)
   {
      schedHandler.sendSyncChatMessageToPlayer(player, message);
   }

   /**
    * <b>Utility method to get Player by his Mojang UUID</b><br>   
    * Use this whenever you need to retrieve a player from a saved UUID<br>
    * CAUTION: Works only if server is in online-mode and player is online!
    * 
    * @param player The player to get the Mojang UUID from
    * @return p The player if a matching UUID was found
    * */
   public Player getPlayerByUUID(UUID uuid)
   {
      Player p = null;

      for(Player player : Bukkit.getServer().getOnlinePlayers())
      {
         if(player.getUniqueId().equals(uuid))
         {
            p = player;
            break;   
         }
      }

      return p;
   }

   /**
    * Sends the HELP as a paginated list of strings in chat to a player
    * 
    * @param sender The sender to send the list to
    * @param list The list to paginate
    * @param page The page number to display.
    * @param countAll The count of all available entries   
    */
   public void paginateHelpList(CommandSender sender, ArrayList<String> list, int page, int countAll)
   {
      int totalPageCount = 1;

      if((list.size() % contentLinesPerPage) == 0)
      {
         if(list.size() > 0)
         {
            totalPageCount = list.size() / contentLinesPerPage;
         }      
      }
      else
      {
         totalPageCount = (list.size() / contentLinesPerPage) + 1;
      }

      if(page <= totalPageCount)
      {
         sender.sendMessage(ChatColor.WHITE + "----------------------------------------");
         sender.sendMessage(ChatColor.GREEN + getDescription().getName() + " Hilfe - Seite (" + String.valueOf(page) + " von " + totalPageCount + ")");      
         sender.sendMessage(ChatColor.WHITE + "----------------------------------------");

         if(list.isEmpty())
         {
            sender.sendMessage(ChatColor.WHITE + "Keine Eintraege.");
         }
         else
         {
            int i = 0, k = 0;
            page--;

            for (String entry : list)
            {
               k++;
               if ((((page * contentLinesPerPage) + i + 1) == k) && (k != ((page * contentLinesPerPage) + contentLinesPerPage + 1)))
               {
                  i++;
                  sender.sendMessage(entry);
               }
            }
         }

         sender.sendMessage(ChatColor.WHITE + "----------------------------------------");
      }
      else
      {
         sender.sendMessage(ChatColor.YELLOW + "Die Hilfe hat nur " + ChatColor.WHITE + totalPageCount + ChatColor.YELLOW + " Seiten!");
      }
   }

   /** Encrypt text with AES.
    * 
    * @param text The text to encrypt.
    * @return text The encrypted text as HEX string.
    * */
   public String encrypt(String text)
   {
      return httpHandler.encrypt(text);
   }

   /** Escape text for safe HTML
    * Used for parameters of PHP scripts for example.
    * 
    * @param text The text to escape.
    * @return text The escaped text
    * */
   public String escapeHTML(String text)
   {
      return httpHandler.escapeHTML(text);     
   }
   
   /** Get players UUID from Bukkit (use only if player is online)
    * Used for parameters of PHP scripts for example.
    * 
    * @param text The text to escape.
    * @return text The escaped text
    * */
   public String getUUIDbyBukkit(String playerName)
   {
      return Bukkit.getServer().getPlayer(playerName).getUniqueId().toString().toLowerCase().replace("-", "");     
   }
}


