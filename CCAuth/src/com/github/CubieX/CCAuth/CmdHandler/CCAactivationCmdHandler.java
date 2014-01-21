package com.github.CubieX.CCAuth.CmdHandler;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.github.CubieX.CCAuth.CCAHTTPHandler;
import com.github.CubieX.CCAuth.CCAuth;

public class CCAactivationCmdHandler implements CommandExecutor
{
   private CCAuth plugin = null;
   private CCAHTTPHandler httpHandler = null;

   public CCAactivationCmdHandler(CCAuth plugin, CCAHTTPHandler httpHandler) 
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

      if (cmd.getName().equalsIgnoreCase("activate"))
      {
         if(args.length == 2)
         {
            if(null != player)
            {
               // ACTIVATE using forum password ======================================================
               if(sender.isOp() || sender.hasPermission("ccauth.use"))
               {
                  if(Bukkit.getOnlineMode())
                  {
                     httpHandler.httpRegisterUserAsync(player, args[0], args[1]);
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
         else
         {
            sender.sendMessage("§eFalsche Parameteranzahl.\n" +
                  "Bitte in der Form: §f'/activate Foren-Name Foren-Passwort §eeingeben!");
            return false;
         }                

      }         
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
