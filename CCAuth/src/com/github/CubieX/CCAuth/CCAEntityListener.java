package com.github.CubieX.CCAuth;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class CCAEntityListener implements Listener
{
   private CCAuth plugin = null;
   private CCAConfigHandler cHandler = null;
   private CCASchedulerHandler schedHandler = null;

   public final Pattern VALID_EMAIL_ADDRESS_REGEX = 
         Pattern.compile("^[A-Z0-9üäö._%+-]+@[A-Z0-9üöä.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

   public CCAEntityListener(CCAuth plugin, CCAConfigHandler cHandler, CCASchedulerHandler schedHandler)
   {        
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.schedHandler = schedHandler;

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

            if ((null != encryptedForumUserName) && (encryptedForumUserName.length() > 0)
                  && (null != encryptedForumPW) && (encryptedForumPW.length() > 0))
            {
               e.setMessage(cmdTokens[0] + " " + encryptedForumUserName + " " + encryptedForumPW);
               if(CCAuth.debug){e.getPlayer().sendMessage("§aLogin-Daten wurden verschluesselt");}
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

      if(e.getMessage().startsWith("/register"))
      {
         String[] cmdTokens = e.getMessage().split(" ");

         if(cmdTokens.length == 5) // maybe activationCode is deactivated in config
         {
            if(cmdTokens[2].equals(cmdTokens[3])) // are both entered passwords identical?
            {
               if(validEmailFormat(cmdTokens[4])) // valid eMail address format?
               {
                  // Encrypt all entries to avoid logging or showing these data tokens in plain text
                  String encryptedForumUserName = plugin.encrypt(cmdTokens[1]); // no HTML escaping for name and PW because forum takes care oon account creation
                  String encryptedForumPW = plugin.encrypt(cmdTokens[2]);
                  String encryptedEmail = plugin.encrypt(cmdTokens[4]);

                  if ((null != encryptedForumUserName) && (encryptedForumUserName.length() > 0)
                        && (null != encryptedForumPW) && (encryptedForumPW.length() > 0)
                        && (null != encryptedEmail) && (encryptedEmail.length() > 0))
                  {
                     e.setMessage(cmdTokens[0] + " " + encryptedForumUserName + " " + encryptedForumPW +
                           " " + encryptedForumPW + " " + encryptedEmail);
                     if(CCAuth.debug){e.getPlayer().sendMessage("§aRegistrier-Daten wurden verschluesselt");}
                  }
                  else
                  {
                     e.setCancelled(true);
                     e.getPlayer().sendMessage("§cKommando blockiert. Fehler beim Verschluesseln der Registrier-Daten.\n" +
                           "Bitte melde das einem Admin!");
                  }
               }
               else
               {
                  e.setCancelled(true);
                  e.getPlayer().sendMessage("§Fehler: Ungueltiges eMail-Format.");
               }
            }
            else
            {
               e.setCancelled(true);
               e.getPlayer().sendMessage("§eFehler: Die beiden Passwoerter stimmen nicht ueberein.");
            }
         }
         else
         {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cKommando blockiert. Bitte in der Form:\n" +
                  "§f'/register Freischaltcode Foren-Name Foren-Passwort Foren-Passwort eMail" +
                  "§ceingeben!");
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
               "Bitte achte darauf das Kommando mit '/' zu verwenden! (/activate ...)");
      }

      if(e.getMessage().startsWith("register"))
      {
         // player accidently forgot the "/" command prefix, so prevent the message to be sent
         // to avoid sending his plan forum password to all players
         e.setCancelled(true);
         e.getPlayer().sendMessage(CCAuth.logPrefix + "§cChat-Ausgabe wurde unterdrueckt!\n" +
               "Bitte achte darauf das Kommando mit '/' zu verwenden! (/register ...)");
      }
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerJoinEvent(PlayerJoinEvent e)
   {
      if(!cHandler.getPlayerListFile().isSet("uuids." + plugin.getUUIDbyBukkit(e.getPlayer().getName()) + ".forumUserName"))
      {
         String message = "§eVerwende §f/register FORENNAME PASSWORT PASSWORT EMAIL\n" +
               "§eum einen Foren-Account zu erstellen. Du benoetigst ausserdem\n" +
               "§f 1x " + CCAuth.forumRegisterPayItem + "§e.";
         schedHandler.sendMessageDelayed(e.getPlayer(), message, 5L);
      }
   }

   public boolean validEmailFormat(String emailStr)
   {
      Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
      return matcher.find();
   }
}
