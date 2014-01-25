package com.github.CubieX.CCAuth.CmdHandler;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.github.CubieX.CCAuth.CCAConfigHandler;
import com.github.CubieX.CCAuth.CCAHTTPHandler;
import com.github.CubieX.CCAuth.CCAuth;

public class CCAccaCmdHandler implements CommandExecutor
{
   private CCAuth plugin = null;
   private CCAConfigHandler cHandler = null;
   private CCAHTTPHandler httpHandler = null;
   private ArrayList<String> helpList = new ArrayList<String>();

   public CCAccaCmdHandler(CCAuth plugin, CCAConfigHandler cHandler, CCAHTTPHandler httpHandler) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.httpHandler = httpHandler;

      loadHelpList();
   }

   private void loadHelpList()
   {
      // add all available commands here      
      helpList.add("§a" + "Farben: §aJeder §f- §cAdmins");
      helpList.add("§f" + "=============== Befehle ===============");
      helpList.add("§a" + "/register <Freischaltcode> <Foren-Name> <Foren-Passwort> <Foren-Passwort> <eMail> - Spieler registrieren");
      helpList.add("§a" + "/activate <Foren-Name> <Foren-Passwort> - Spieler freischalten");
      helpList.add("§a" + "/cca help - Dieses Hilfemenue");
      helpList.add("§a" + "/cca verifiy <Spielername> - Pruefen welcher Foren-User zum Spieler gehoert");
      helpList.add("§a" + "/cca version - Version des Plugins ausgeben");
      helpList.add("§c" + "/cca reload - Plugin und Daten neu laden");
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("cca"))
      {
         if (args.length == 0)
         { //no arguments, so help will be displayed
            return false;
         }
         else if (args.length == 1)
         {
            if (args[0].equalsIgnoreCase("version"))
            {
               sender.sendMessage(CCAuth.logPrefix + ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());
               return true;
            }

            if (args[0].equalsIgnoreCase("reload"))
            {
               if(sender.isOp() || sender.hasPermission("ccauth.admin"))
               {                        
                  cHandler.reloadConfig(sender);
                  return true;
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");
               }
            }

            // HELP will be displayed (Page 1) ========================================
            if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe"))
            {
               if(sender.hasPermission("ccauth.use"))
               {
                  // send list paginated
                  plugin.paginateHelpList(sender, helpList, 1, helpList.size());
               }

               return true;
            }
         }
         else if (args.length == 2)
         {
            // UUID of Mojang account for given player name will be displayed =================================
            if (args[0].equalsIgnoreCase("verify"))
            {
               if(sender.hasPermission("ccauth.use"))
               {
                  if(Bukkit.getOnlineMode())
                  {          
                     OfflinePlayer checkedPlayer = Bukkit.getServer().getOfflinePlayer(args[1]);
                     String playerUUID = "";

                     if(checkedPlayer.isOnline())
                     {
                        if(CCAuth.debug){sender.sendMessage("Retrieving UUID from Bukkit...");}
                        playerUUID = plugin.getUUIDbyBukkit(args[1]);

                        if(cHandler.getPlayerListFile().isSet("uuids." + playerUUID + ".forumUserName"))
                        {
                           String playerName = cHandler.getPlayerListFile().getString("uuids." + playerUUID + ".forumUserName");
                           sender.sendMessage("§f" + checkedPlayer.getName() + "§a ist im Forum unter dem Namen §f" + playerName + "§a registriert.\n§a" +
                                 "Mojang UUID: §f" + playerUUID);
                        }
                        else
                        {
                           sender.sendMessage("§f" + checkedPlayer.getName() + "§e ist nicht im Forum registriert.\n" +
                                 "Mojang UUID: §f" + playerUUID);
                        }
                     }
                     else
                     {
                        if(CCAuth.debug){sender.sendMessage("Retrieving UUID from Mojang server and checking player registration in configured forum DB...");}
                        plugin.isPlayerRegisteredInForum(sender, checkedPlayer.getName()); // will send message to querying player                     
                     }
                  }
                  else
                  {
                     player.sendMessage(CCAuth.logPrefix + "§4Der Server laeuft momentan im Offline-Mode.\n" +
                           "Die UUID kann nur im Online-Mode angefordert werden!");
                  }
               }

               return true;
            }
         }
         else
         {
            sender.sendMessage("§eFalsche Parameteranzahl.");
         }
      }         
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
