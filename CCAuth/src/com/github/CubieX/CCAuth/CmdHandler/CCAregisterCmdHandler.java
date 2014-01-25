package com.github.CubieX.CCAuth.CmdHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.github.CubieX.CCAuth.CCAHTTPHandler;
import com.github.CubieX.CCAuth.CCAuth;

public class CCAregisterCmdHandler implements CommandExecutor
{
   private CCAuth plugin = null;
   private CCAHTTPHandler httpHandler = null;
   private String wrongParamCountMsg = "§eFalsche Parameteranzahl.\n" +
         "Bitte in der Form:\n" +
         "§f'/register Freischaltcode Foren-Name Foren-Passwort Foren-Passwort eMail\n" +
         "§eeingeben!";

   public CCAregisterCmdHandler(CCAuth plugin, CCAHTTPHandler httpHandler) 
   {
      this.plugin = plugin;
      this.httpHandler = httpHandler;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      // TODO implement ingame-forum registration. See: webserver\www\board\admin\sources\base\ipsMember.php
      // and http://www.invisionpower.com/support/guides/_/advanced-and-developers/api-methods/ipsmember-r200
      // and http://stackoverflow.com/questions/5389619/calling-php-file-fron-another-php-file-while-passing-arguments
      // players should do the parcour and then when the retrieved the secret code, enter:
      // ./activate CODE DESIRED_FORUMNAME FORUM_PASS FORUM_PASS EMAIL EMAIL
      if (cmd.getName().equalsIgnoreCase("register"))
      {
         if(args.length == 5) // activationCode, forumUserName, forumPass, forumPass, eMail (format checks already done in CommandPreProccessEvent)
         {
            if(null != player)
            {
               // REGISTER player in forum (creates new forum account) ======================================================
               if(sender.isOp() || sender.hasPermission("ccauth.use"))
               {
                  if(Bukkit.getOnlineMode())
                  {
                     if(args[0].equals(CCAuth.activationCode))
                     {
                        httpHandler.httpRegisterUserAsync(player, args[1], args[2], args[4]);
                     }
                     else
                     {
                        player.sendMessage("§eDer eingegebene Freischalt-Code ist nicht korrekt!");
                     }
                  }
                  else
                  {
                     player.sendMessage(CCAuth.logPrefix + "§4Der Server laeuft momentan im Offline-Mode.\n" +
                           "Aktivierung ist nur im Online-Mode moeglich!");
                  }
               }
            }
            else
            {
               sender.sendMessage(CCAuth.logPrefix + "This command is only usable ingame!");
            }

            return true;
         }
         else if(args.length == 4) // forumUserName, forumPass, forumPass, eMail (format checks already done in CommandPreProccessEvent)
         {
            if(null != player)
            {
               // REGISTER player in forum (creates new forum account) ======================================================
               if(sender.isOp() || sender.hasPermission("ccauth.use"))
               {
                  if(Bukkit.getOnlineMode())
                  {
                     if(CCAuth.activationCode.equals("")) // activationCode deactivated in config?
                     {
                        httpHandler.httpRegisterUserAsync(player, args[0], args[1], args[3]);
                     }
                     else
                     {
                        player.sendMessage(wrongParamCountMsg);
                     }
                  }
                  else
                  {
                     player.sendMessage(CCAuth.logPrefix + "§4Der Server laeuft momentan im Offline-Mode.\n" +
                           "Registrierung ist nur im Online-Mode moeglich!");
                  }
               }
            }
            else
            {
               sender.sendMessage(CCAuth.logPrefix + "This command is only usable ingame!");
            }

            return true;
         }
         else
         {
            sender.sendMessage(wrongParamCountMsg);
            return false;
         }                

      }
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
