package me.xidentified.devotions.util;

import de.cubbossa.translations.Message;
import de.cubbossa.translations.MessageBuilder;
import de.cubbossa.translations.MessageCore;

public class Messages {

  public static final Message GENERAL_CMD_PLAYER_ONLY = new MessageBuilder("general.must_be_player")
      .withDefault("<negative>Only players can use this command.")
      .build();
  public static final Message GENERAL_NO_PERM = new MessageBuilder("general.no_perm")
      .withDefault("<negative>You don't have sufficient permission")
      .build();
  public static final Message GENERAL_CMD_NO_PERM = new MessageBuilder("general.cmd.no_perm")
      .withDefault("<negative>You don't have permission to use this command.")
      .build();
  public static final Message GENERAL_PLAYER_NOT_FOUND = new MessageBuilder("general.player_not_found")
      .withDefault("<negative>Player not found: <name>")
      .build();

  public static final Message DEITY_BLESSED = new MessageBuilder("deity.blessed")
      .withDefault("<positive><deity> has blessed you with <blessing>!")
      .withPlaceholders("deity", "blessing")
      .build();
  public static final Message DEITY_CURSED = new MessageBuilder("deity.cursed")
      .withDefault("<positive><deity> has cursed you with <curse>!")
      .withPlaceholders("deity", "curse")
      .build();
  public static final Message DEITY_CMD_USAGE = new MessageBuilder("deity.command.usage")
      .withDefault("<warning>Usage: /deity <select|info> [DeityName]")
      .build();
  public static final Message DEITY_INFO = new MessageBuilder("deity.info")
      .withDefault("""
                  <gold>Details of <name>
                  <yellow>Lore: <gray><lore>
                  <yellow>Domain: <gray><domain>
                  <yellow>Alignment: <gray><alignment>
                  <yellow>Favored Rituals: <gray><rituals>
                  <yellow>Favored Offerings: <gray><offerings>""")
      .withPlaceholders("name", "lore", "domain", "alignment", "rituals", "offerings")
      .build();

  public static final Message MEDITATION_COMPLETE = new MessageBuilder("meditation.complete")
      .withDefault("<positive>Meditation complete! You can now move.")
      .build();
  public static final Message MEDIDATION_CANCELLED = new MessageBuilder("meditation.cancelled")
      .withDefault("<negative>You moved during meditation! Restarting timer...")
      .build();

  public static final Message SHRINE_NO_PERM_LIST = new MessageBuilder("shrines.list.no_perm")
      .withDefault("<negative>You don't have permission to list shrines.")
      .build();
  public static final Message SHRINE_NO_DESIGNATED_SHRINE = new MessageBuilder("shrines.list.no_shrines")
      .withDefault("<negative>There are no designated shrines.")
      .build();
  public static final Message SHRINE_SHRINE = new MessageBuilder("shrines.list.shrine")
      .withDefault("<hover:show_text:Click to teleport><click:run_cmd:/teleport @p <x> <y> <z>><deity> at <x>, <y>, <z>")
      .withPlaceholders("deity", "x", "y", "z")
      .build();
  public static final Message SHRINE_NO_PERM_REMOVE = new MessageBuilder("shrines.remove.no_perm")
      .withDefault("<negative>You don't have permission to remove shrines.")
      .build();
  public static final Message SHRINE_NO_PERM_SET = new MessageBuilder("shrines.set.no_perm")
      .withDefault("<negative>You don't have permission to set a shrine.")
      .build();
  public static final Message SHRINE_RC_TO_REMOVE = new MessageBuilder("shrines.right_click_to_remove")
      .withDefault("<yellow>Right-click on a shrine to remove it.")
      .build();
  public static final Message SHRINE_LIMIT_REACHED = new MessageBuilder("shrines.limit_reached")
      .withDefault("<negative>You have reached the maximum number of shrines (<limit>).")
      .withPlaceholder("limit")
      .build();
  public static final Message SHRINE_FOLLOW_DEITY_TO_DESIGNATE = new MessageBuilder("shrines.follow_deity_to_designate")
      .withDefault("<negative>You need to follow a deity to designate a shrine!")
      .build();
  public static final Message SHRINE_NOT_FOLLOWING_DEITY = new MessageBuilder("shrines.not_following_deity")
      .withDefault("<negative>Only followers of <deity> may use this shrine.")
      .withPlaceholder("deity")
      .build();
  public static final Message SHRINE_CLICK_BLOCK_TO_DESIGNATE = new MessageBuilder("shrines.click_block_to_create")
      .withDefault("<yellow>Right-click on a block to designate it as a shrine for <deity>")
      .withPlaceholder("deity")
      .build();
  public static final Message SHRINE_SUCCESS = new MessageBuilder("shrines.shrine_created")
      .withDefault("<positive>Successfully designated a shrine for <deity>!")
      .withPlaceholder("deity")
      .build();
  public static final Message SHRINE_DEITY_NOT_FOUND = new MessageBuilder("shrines.deity_not_found")
      .withDefault("<negative>Could not determine your deity, please inform an admin.")
      .build();
  public static final Message SHRINE_REMOVED = new MessageBuilder("shrines.remove.success")
      .withDefault("<positive>Shrine removed successfully!")
      .build();
  public static final Message SHRINE_REMOVE_FAIL = new MessageBuilder("shrines.remove.not_found")
      .withDefault("<negative>Failed to remove shrine. You might not own it.")
      .build();
  public static final Message SHRINE_REMOVE_NOT_FOUND = new MessageBuilder("shrines.remove.not_found")
      .withDefault("<negative>No shrine found at this location.")
      .build();
  public static final Message SHRINE_OFFERING_ACCEPTED = new MessageBuilder("shrines.offering_accepted")
      .withDefault("<positive>Your offering has been accepted!")
      .build();
  public static final Message SHRINE_PLACE_ON_TOP = new MessageBuilder("shrines.cannot_place_on_top")
      .withDefault("<negative>You cannot place blocks on top of a shrine!")
      .build();
  public static final Message SHRINE_CANNOT_BREAK = new MessageBuilder("shrines.cannot_break_shrines")
      .withDefault("<negative>You cannot destroy shrines! Remove with <yellow>/shrine remove</yellow>.")
      .build();

  public static final Message FAVOR_CMD_USAGE = new MessageBuilder("favor.cmd.usage")
      .withDefault("<warning>Usage: /favor <set|give|take> <playername> <amount>")
      .build();
  public static final Message FAVOR_CURRENT = new MessageBuilder("favor.current_favor")
      .withDefault("Your current favor is: <favor>")
      .withPlaceholder("favor")
      .build();

  public static final Message RITUAL_CMD_USAGE = new MessageBuilder("ritual.cmd.usage")
      .withDefault("<warning>Usage: /ritual <info> [RitualName]")
      .build();
  public static final Message RITUAL_INFO = new MessageBuilder("ritual.info")
      .withDefault("""
                  <gold>Details of <display-name>
                  <yellow>Description: <gray><description>
                  <yellow>Key Item: <gray><item-name>
                  <yellow>Favor Rewarded: <gray><favour-amount>""")
      .withPlaceholders("display-name", "description", "item-name", "favour-amount")
      .build();
  public static final Message RITUAL_NOT_FOUND = new MessageBuilder("ritual.not_found")
      .withDefault("<negative>Unknown ritual. Please choose a valid ritual name.")
      .build();

  public static final Message MIRACLE_BESTOWED = new MessageBuilder("miracle.bestowed")
      .withDefault("<positive>A miracle has been bestowed upon ye!")
      .build();
  public static final Message MIRACLE_SAVED_FROM_DEATH = new MessageBuilder("miracle.saved_from_death")
      .withDefault("<positive>A miracle has revived you upon death!")
      .build();
  public static final Message MIRACLE_HERO_OF_VILLAGE = new MessageBuilder("miracle.hero_of_the_village")
      .withDefault("<positive>A miracle has granted you the Hero of the Village effect!")
      .build();
  public static final Message MIRACLE_FIRE_RESISTANCE = new MessageBuilder("miracle.fire_resistance")
      .withDefault("<positive>A miracle has granted you Fire Resistance!")
      .build();
  public static final Message MIRACLE_REPAIR = new MessageBuilder("miracle.repair")
      .withDefault("<positive>A miracle has repaired all your items!")
      .build();
  public static final Message MIRACLE_HARVEST = new MessageBuilder("miracle.harvest")
      .withDefault("<positive>A miracle has blessed you with a bountiful harvest!")
      .build();
  public static final Message MIRACLE_GOLEM = new MessageBuilder("miracle.iron_golem")
      .withDefault("<positive>A miracle has summoned Iron Golems to aid you!")
      .build();
  public static final Message MIRACLE_WOLVES = new MessageBuilder("miracle.wolves")
      .withDefault("<positive>A miracle has summoned friendly Wolves to protect you!")
      .build();
}
