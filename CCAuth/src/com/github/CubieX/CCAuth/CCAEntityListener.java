package com.github.CubieX.CCAuth;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CCAEntityListener implements Listener
{
   private CCAuth plugin = null;

   UUID leashedEntity;

   public CCAEntityListener(CCAuth plugin)
   {        
      this.plugin = plugin;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
   public void onPlayerCommand(PlayerCommandPreprocessEvent e)
   {
      if(e.getMessage().startsWith("/activate"))
      {
         String[] cmdTokens = e.getMessage().split(" ");

         if(cmdTokens.length == 3)
         {
            // Encrypt forumUserName that the player entered, in case he puts the password as first parameter to avoid it to be logged in plain text
            String encryptedForumUserName = plugin.encrypt(plugin.escapeHTML(cmdTokens[1]));
            // player is sending his forum password to activate his account
            // CRITICAL: Encrypt the password at this point (AES-128 as HEX string) to not let it be logged in plain text to server log
            String encryptedForumPW = plugin.encrypt(plugin.escapeHTML(cmdTokens[2]));

            if ((null != encryptedForumUserName) && (encryptedForumUserName.length() > 0) && (null != encryptedForumPW) && (encryptedForumPW.length() > 0))
            {
               e.setMessage(cmdTokens[1] + " " + encryptedForumUserName);
               if(CCAuth.debug){e.getPlayer().sendMessage("§aForen-Name wurde verschluesselt: §f\n" + encryptedForumUserName + "\n");}

               e.setMessage(cmdTokens[2] + " " + encryptedForumPW);
               if(CCAuth.debug){e.getPlayer().sendMessage("§aPasswort wurde verschluesselt: §f\n" + encryptedForumPW);}
            }
            else
            {
               e.setCancelled(true);
               e.getPlayer().sendMessage("§cKommando blockiert. Fehler beim Verschluesseln der Login-Daten.\n" +
                     "Bitte melde das einem Admin!");
            }
         }
         else
         {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cKommando blockiert. Bitte in der Form: §f'/activate Foren-Name Foren-Passwort §ceingeben!");
         }
      }
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onPlayerCommand(AsyncPlayerChatEvent e)
   {
      if(e.getMessage().startsWith("activate"))
      {
         // player accidently forgot the "/" command prefix, so prevent the message to be sent
         // to avoid sending his plan forum password to all players
         e.setCancelled(true);
         e.getPlayer().sendMessage(CCAuth.logPrefix + "§cChat-Ausgabe wurde unterdrueckt!\n" +
               "Bitte achte darauf das Kommando mit '/' zu verwenden! (/activate <Foren-Passwort>)");
      }
   }
}
