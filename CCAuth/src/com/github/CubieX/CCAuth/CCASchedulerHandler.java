package com.github.CubieX.CCAuth;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.edawg878.identifier.IdentifierAPI;

public class CCASchedulerHandler
{
   private CCAuth plugin = null;
   private CCAConfigHandler cHandler = null;
   private IdentifierAPI identAPI = null;

   public CCASchedulerHandler(CCAuth plugin, CCAConfigHandler cHandler, IdentifierAPI identAPI)
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.identAPI = identAPI;
   }

   public void sendSyncMessage(final CommandSender sender, final String message)
   {
      plugin.getServer().getScheduler().runTask(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            if(null != sender)
            {
               sender.sendMessage(message);  
            }            
         }
      });
   }

   public void isPlayerRegisteredInForum(final CommandSender sender, final String playerName)
   {
      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            // player is offline, so get UUID from Mojang server
            final String playerUUID = identAPI.getPlayerUUID(playerName);


            plugin.getServer().getScheduler().runTask(plugin, new Runnable()
            {
               @Override
               public void run()
               {
                  if((null != playerUUID) && (!playerUUID.equals("")))
                  {
                     if(cHandler.getPlayerListFile().isSet("uuids." + playerUUID + ".forumUserName"))
                     {
                        String playerName = cHandler.getPlayerListFile().getString("uuids." + playerUUID + ".forumUserName");
                        sender.sendMessage("§f" + playerName + "§a ist im Forum unter dem Namen §f" + playerName + "§a registriert.\n§a" +
                              "Mojang UUID: §f" + playerUUID);
                     }
                     else
                     {
                        sender.sendMessage("§f" + playerName + "§e ist nicht im Forum registriert.\n" +
                              "Mojang UUID: §f" + playerUUID);
                     }
                  }
                  else
                  {                  
                     sender.sendMessage("§f" + playerName + "§e hat keinen Mojang-Account!\n" +
                           "Registrierung im Forum kann nicht geprueft werden.");
                  }                  
               }
            });            
         }
      });
   }
   
   /**
    * <b>Sends a chat message via main server thread</b><br>
    * Used for send messages to players from async tasks
    * */
   public void sendSyncChatMessageToPlayer(final Player player, final String message)
   {
      plugin.getServer().getScheduler().runTask(plugin, new Runnable()
      {
         public void run()
         {
            if(null != player)
            {
               player.sendMessage(message);
            }
         }
      });
   }
}
