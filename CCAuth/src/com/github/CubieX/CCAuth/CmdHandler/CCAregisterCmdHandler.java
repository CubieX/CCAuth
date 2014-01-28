package com.github.CubieX.CCAuth.CmdHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.github.CubieX.CCAuth.CCAConfigHandler;
import com.github.CubieX.CCAuth.CCAHTTPHandler;
import com.github.CubieX.CCAuth.CCAuth;

public class CCAregisterCmdHandler implements CommandExecutor
{
   private CCAuth plugin = null;
   private CCAHTTPHandler httpHandler = null;
   private CCAConfigHandler cHandler = null;
   private String wrongParamCountMsg = "§eFalsche Parameteranzahl.\n" +
         "Bitte in der Form:\n" +
         "§f'/register Freischaltcode Foren-Name Foren-Passwort Foren-Passwort eMail\n" +
         "§eeingeben!";

   public CCAregisterCmdHandler(CCAuth plugin, CCAHTTPHandler httpHandler, CCAConfigHandler cHandler) 
   {
      this.plugin = plugin;
      this.httpHandler = httpHandler;
      this.cHandler = cHandler;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("register"))
      {
         if(args.length == 4) // forumUserName, forumPass, forumPass, eMail (format checks already done in CommandPreProccessEvent)
         {
            if(null != player)
            {
               // REGISTER player in forum (creates new forum account) ======================================================
               if(player.isOp() || player.hasPermission("ccauth.use"))
               {
                  if(Bukkit.getOnlineMode())
                  {
                     if(!cHandler.getPlayerListFile().isSet("uuids." + plugin.getUUIDbyBukkit(player.getName()) + ".forumUserName"))
                     {
                        if(playerHasNeededForumRegisterPayItems(player.getInventory()))
                        {
                           httpHandler.httpRegisterUserAsync(player, args[0], args[1], args[3]);
                        }
                        else
                        {
                           player.sendMessage("§eDu benoetigst§f\n" + getForumRegisterPayItemsListForChat() + 
                                 "§e um einen Forenaccount zu erstellen.");
                        }
                     }
                     else
                     {
                        player.sendMessage("§eDu bist bereits mit dem Namen §f" +
                              cHandler.getPlayerListFile().getString("uuids." + plugin.getUUIDbyBukkit(player.getName()) +
                                    ".forumUserName") + "§e im Forum registriert!");
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
   
   private String getForumRegisterPayItemsListForChat()
   {
      String list = "nichts";
      
      if(!CCAuth.forumRegisterPayItems.isEmpty())
      {
         list = "";
         
         for(String item : CCAuth.forumRegisterPayItems.keySet())
         {
            list += CCAuth.forumRegisterPayItems.get(item) + "x " + item + "\n";
         }
      }
      
      return (list);
   }
   
   private boolean playerHasNeededForumRegisterPayItems(Inventory inv)
   {
      boolean res = true;
      
      if(!CCAuth.forumRegisterPayItems.isEmpty())
      {  
         for(String item : CCAuth.forumRegisterPayItems.keySet())
         {
            if(!inv.containsAtLeast(new ItemStack(Material.getMaterial(item)), CCAuth.forumRegisterPayItems.get(item)))
            {
               res = false;
               break;
            }
         }
      }
      
      return (res);
   }
}
