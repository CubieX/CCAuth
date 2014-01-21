package com.edawg878.identifier;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileCriteria;

public class IdentifierAPI
{
   private static final HttpProfileRepository repository = new HttpProfileRepository();
   private static final String AGENT = "minecraft";
   
   public String getPlayerUUID(String playerName) // call this asynchronous, because it may take a while to get the info from Mojang server!
   {
      String playerUUID = "";
      Profile[] profiles = repository.findProfilesByCriteria(new ProfileCriteria(playerName, AGENT));

      if (profiles.length == 1)
      {
         playerUUID = profiles[0].getId();
      }
      else
      {
         playerUUID = "";
      }

      return playerUUID;
   }
}