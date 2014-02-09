package com.github.CubieX.CCAuth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.springframework.web.util.HtmlUtils;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.edawg878.identifier.IdentifierAPI;

public class CCAHTTPHandler
{
   CCAuth plugin = null;
   CCASchedulerHandler schedHandler = null;
   CCAConfigHandler cHandler = null;
   IdentifierAPI identAPI = null;   
   final private static char[] hexArray = "0123456789ABCDEF".toCharArray(); // possible HEX values   
   String iv = "d0ec238b641fa21d";  // initialization vector (16 bytes) -> should be randomly generated! Only used for CBC or  similar cipher mode.
   Cipher cipher = null;

   public CCAHTTPHandler(CCAuth plugin, CCASchedulerHandler schedHandler, CCAConfigHandler cHandler, IdentifierAPI identAPI)
   {
      this.plugin = plugin;
      this.schedHandler = schedHandler;
      this.cHandler = cHandler;
      this.identAPI = identAPI;

      try
      {
         cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // must match with remote PHP scripts encryption scheme
      }
      catch (NoSuchAlgorithmException e)
      {        
         e.printStackTrace();
      }
      catch (NoSuchPaddingException e)
      {         
         e.printStackTrace();
      }
   }

   // activate minecraft player by bundling his UUID with his forum name
   public void httpActivateUserAsync(final Player player, final String encryptedForumUserName, final String encryptedForumPass)
   {
      if((null == player) || (!player.isOnline()))
      {
         return;
      }

      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            String targetURL = CCAuth.verifyScriptURL;
            String urlParameters = "";            
            URL url;
            HttpURLConnection connection = null;

            //if(CCAuth.debug){CCAuth.log.info("PLUGIN: Decrypted PW: " + decrypt(encryptedForumPass) + " -> Encrypted again (HEX padded): " + encryptedForumPass);}

            if((null != encryptedForumUserName) && (null != encryptedForumPass) && (null != player))
            {
               urlParameters = "?u=" + encryptedForumUserName +
                     "&p=" + encryptedForumPass; // AES-128 encrypted as HEX string. No URLEncoding, because HEX is web-save.

               if(CCAuth.debug){CCAuth.log.info("DEBUG PLUGIN Request: " + CCAuth.verifyScriptURL + urlParameters);}
            }
            else
            {
               player.sendMessage("§cFehler beim Vorbereiten der HTTP Anfrage fuer die Freischaltung!\n" +
                     "Bitte melde das einem Admin!");
               return;
            }

            try
            {
               //Create connection               
               url = new URL(targetURL + urlParameters);
               connection = (HttpURLConnection)url.openConnection(); // will execute the request
               connection.setRequestMethod("GET");

               connection.setUseCaches (false);
               connection.setDoInput(true);
               connection.setDoOutput(true);

               //Get Response 
               InputStream is = connection.getInputStream();
               BufferedReader rd = new BufferedReader(new InputStreamReader(is));
               String line;
               StringBuffer response = new StringBuffer(); 

               while((line = rd.readLine()) != null)
               {
                  response.append(line); // only player name gets sent, so no multi-line response
                  //response.append('\r');
                  //response.append(' ');
               }

               rd.close();

               if(CCAuth.debug){CCAuth.log.info("DEBUG PHP Response: " + response.toString().trim());}

               if(!response.toString().isEmpty())
               {
                  // register player with UUID and forum name in playerFile
                  cHandler.getPlayerListFile().set("uuids." + plugin.getUUIDbyBukkit(player.getName()) + ".forumUserName", response.toString().trim());
                  cHandler.savePlayerListFile();

                  plugin.sendSyncChatMessage(player, "§aForen-Account §f" + response.toString().trim() + " §abestaetigt!\n" +
                        "Du bist jetzt freigeschaltet. Viel Spass auf unserem Server!");
               }
               else
               {
                  plugin.sendSyncChatMessage(player, "§eDu bist nicht mit diesem Passwort in unserem Forum registriert.\n" +
                        "Bitte gib das Forenpasswort an, bzw. erstelle zuerst\n" +
                        "einen Account auf §f" + CCAuth.forumURL + "\n" +
                        "§eDanach kannst du dich hier freischalten.");
               }
            }
            catch (Exception e)
            {
               schedHandler.sendSyncMessage(player, "§cFehler beim Senden der HTTP request fuer die Freischaltung!\n" +
                     "Bitte melde das einem Admin!");
               e.printStackTrace();               
            }
            finally
            {
               if(connection != null)
               {
                  connection.disconnect();
               }
            }
         }
      });
   }

   // create new forum user
   @SuppressWarnings("deprecation")
   public void httpRegisterUserAsync(final Player player, final String encryptedForumUserName, final String encryptedForumPass, final String encryptedEmail)
   {
      if((null == player) || (!player.isOnline()))
      {
         return;
      }

      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            String targetURL = CCAuth.createUserScriptURL;
            String urlParameters = "";            
            URL url;
            HttpURLConnection connection = null;

            if((null != encryptedForumUserName)
                  && (null != encryptedForumPass)
                  && (null != encryptedEmail)
                  && (null != player))
            {
               urlParameters = "?u=" + encryptedForumUserName +
                     "&p=" + encryptedForumPass +
                     "&e=" + encryptedEmail; // AES-128 encrypted as HEX string. No URLEncoding, because HEX is web-save.

               if(CCAuth.debug){CCAuth.log.info("DEBUG PLUGIN Request: " + CCAuth.createUserScriptURL + urlParameters);}
            }
            else
            {
               player.sendMessage("§cFehler beim Vorbereiten der HTTP Anfrage fuer die Registrierung!\n" +
                     "Bitte melde das einem Admin!");
               return;
            }

            try
            {
               //Create connection               
               url = new URL(targetURL + urlParameters);
               connection = (HttpURLConnection)url.openConnection(); // will execute the request
               connection.setRequestMethod("GET");

               connection.setUseCaches (false);
               connection.setDoInput(true);
               connection.setDoOutput(true);

               //Get Response 
               InputStream is = connection.getInputStream();
               BufferedReader rd = new BufferedReader(new InputStreamReader(is));
               String line = "";
               StringBuffer response = new StringBuffer(); 

               while((line = rd.readLine()) != null)
               {
                  response.append(line); // only player name gets sent, so no multi-line response
                  //response.append('\r');
                  //response.append(' ');
               }

               String resp = response.toString().trim();
               rd.close();

               if(CCAuth.debug){CCAuth.log.info("DEBUG PHP Response: " + resp);}

               if((!resp.isEmpty()) && (!resp.contains("Warning")))
               {
                  boolean successRemoving = true;

                  // register player with UUID and forum name in playerFile
                  cHandler.getPlayerListFile().set("uuids." + plugin.getUUIDbyBukkit(player.getName()) + ".forumUserName", response.toString().trim());
                  cHandler.savePlayerListFile();

                  for(String item : CCAuth.forumRegisterPayItems.keySet())
                  {
                     String matDat[] = item.split("@");
                     String mat = matDat[0];
                     int subID = Integer.parseInt(matDat[1].split(":")[0]);
                     HashMap<Integer, ItemStack> couldNotRemove =
                           player.getInventory().removeItem(new ItemStack(Material.getMaterial(mat), CCAuth.forumRegisterPayItems.get(item), (short)subID));

                     if(!couldNotRemove.isEmpty())
                     {
                        successRemoving = false;
                     }
                  }
                  
                  player.updateInventory();

                  if(successRemoving)
                  {
                     plugin.sendSyncChatMessage(player, "§aForen-Account §f" + resp + " §aerstellt!\n" +
                           "Link zum Forum: §f" + CCAuth.forumURL);
                  }
                  else
                  {
                     player.sendMessage("§cFehler beim Entfernen des Bezahl-Items fuer die Forenaccount-Erstellung!\n" +
                           "Bitte melde das einem Admin!");
                  }
               }
               else
               {
                  plugin.sendSyncChatMessage(player, "§eDer gewaehlte Name ist schon vergeben!"); // may also be the eMail address, but this is unlikely
               }
            }
            catch (Exception e)
            {
               schedHandler.sendSyncMessage(player, "§cFehler beim Senden der HTTP request fuer die Freischaltung!\n" +
                     "Bitte melde das einem Admin!");
               e.printStackTrace();               
            }
            finally
            {
               if(connection != null)
               {
                  connection.disconnect();
               }
            }
         }
      });
   }

   public String encrypt(String message)
   {
      byte[] encrypted = null;

      try
      {
         /*byte[] salt = {
            (byte)0xd3, (byte)0xa3, (byte)0x61, (byte)0x8c,
            (byte)0x16, (byte)0xc1, (byte)0xfe, (byte)0x22
        };

      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 16384, 128);
      SecretKey tmp = factory.generateSecret(spec);
      SecretKey keyspec = new SecretKeySpec(tmp.getEncoded(), "AES");*/

         SecretKeySpec keyspec = new SecretKeySpec(CCAuth.secretKey.getBytes("UTF-8"), "AES"); // fixed key alone is not so secure
         //IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes("UTF-8"));      
         //cipher.init(Cipher.ENCRYPT_MODE,keyspec,ivspec); // CBC uses iv
         cipher.init(Cipher.ENCRYPT_MODE, keyspec); // ECB uses no iv
         byte[] plainBytes = message.getBytes("UTF-8");
         encrypted = cipher.doFinal(plainBytes);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }

      return bytesToHexString(encrypted);
   }

   // Input must be HEX string (32 digits (16 HEX values))
   private String decrypt(String encryptedMessage)
   {
      byte[] encMsgAsBytes = null;
      byte[] decrypted = null;

      try
      {
         encMsgAsBytes = hexStringToByteArray(encryptedMessage);
         SecretKeySpec keyspec = new SecretKeySpec(CCAuth.secretKey.getBytes("UTF-8"), "AES");
         //IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes("UTF-8"));         
         //cipher.init(Cipher.ENCRYPT_MODE,keyspec, ivspec); // CBC uses iv
         cipher.init(Cipher.DECRYPT_MODE, keyspec); // ECB uses no iv
         decrypted = cipher.doFinal(encMsgAsBytes);         
      }
      catch (InvalidKeyException e1)
      {         
         e1.printStackTrace();
      }
      /*catch (InvalidAlgorithmParameterException e1)
      {         
         e1.printStackTrace();
      }*/
      catch (UnsupportedEncodingException e)
      {
         e.printStackTrace();
      }
      catch (IllegalBlockSizeException e)
      {        
         e.printStackTrace();
      }
      catch (BadPaddingException e)
      {         
         e.printStackTrace();
      }      

      String plainText = hexStringToCleanUTF8string(bytesToHexString(decrypted));

      return plainText;
   }

   /**
    * Constructs the password hash for IPboard v3
    * $hash = md5( md5( $salt ) . md5( $password ) );
    * 
    * @param salt The salt in database for the user. 5 Bytes. (members_pass_salt)
    * @param pw The plain text password of the user
    * 
    * @return hash The resulting IPboard hash as HEX string (members_pass_hash)
    * */
   private String constructPWhashForIPboard(String salt, String pw)
   {
      //$hash = md5( md5( $salt ) . md5( $cleaned_password ) );
      /* this is how IPboard constructs the hash
         $hash is the value stored in the database column members_pass_hash.
         $salt is the value stored in the database column members_pass_salt.
         $password is the plain text password.  (but some chars are HTML-escaped decimally!!
         see: http://www.invisionpower.com/support/guides/_/advanced-and-developers/integration/login-modules-r42)
       */

      String hash = "";
      String hashedPWhex = null;
      String hashedSaltHex = null;

      try
      {
         hashedPWhex = getMD5(escapeHTML(pw).getBytes("UTF-8")); // escapeHTML converts some special chars into HTML entities (see file: HtmlCharacterEntityReferencesForIPboard.properties)
         if(CCAuth.debug){CCAuth.log.info("PLUGIN Hashed PW: " + hashedPWhex);}
         hashedSaltHex = getMD5(salt.getBytes("UTF-8"));
         if(CCAuth.debug){CCAuth.log.info("PLUGIN Hashed Salt: " + hashedSaltHex);}
         String concatValue = hashedSaltHex.concat(hashedPWhex);         
         hash = getMD5(concatValue.getBytes("UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {       
         e.printStackTrace();
      }

      return hash;
   }

   // returns MD5 as HEX string
   public String getMD5(byte[] value)
   {      
      byte[] theDigest = null;      
      MessageDigest md = null;
      String md5inHex = "";

      try
      {  
         md = MessageDigest.getInstance("MD5");         
         md.update(value);
         theDigest = md.digest();
         md5inHex = bytesToHexString(theDigest);
      }      
      catch (NoSuchAlgorithmException e)
      {         
         e.printStackTrace();
      }

      return md5inHex;
   }

   private String bytesToHexString(byte[] bytes)
   {
      char[] hexChars = new char[bytes.length * 2];

      for ( int j = 0; j < bytes.length; j++ ) {
         int v = bytes[j] & 0xFF;
         hexChars[j * 2] = hexArray[v >>> 4];
         hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }

      return new String(hexChars).toLowerCase();
   }

   private byte[] hexStringToByteArray(String s)
   {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
         data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
               + Character.digit(s.charAt(i+1), 16));
      }
      return data;
   }

   private String hexStringToCleanUTF8string(String hexString)
   {
      ByteBuffer buff = ByteBuffer.allocate(hexString.length()/2);
      for (int i = 0; i < hexString.length(); i+=2) {
         buff.put((byte)Integer.parseInt(hexString.substring(i, i+2), 16));
      }
      buff.rewind();
      Charset cs = Charset.forName("UTF-8");
      CharBuffer cb = cs.decode(buff);

      String cleanedText = cb.toString().replaceAll("\\s+", ""); // delete spaces, newlines, tabs, line feeds

      return cleanedText;
   }

   public String escapeHTML(String s)
   {
      String escaped = HtmlUtils.htmlEscapeDecimal(s); // suitable for IPboard password field
      return (escaped);
   }
}
