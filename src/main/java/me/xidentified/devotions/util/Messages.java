package me.xidentified.devotions.util;

import de.cubbossa.translations.Message;
import de.cubbossa.translations.MessageBuilder;
import de.cubbossa.translations.MessageCore;

public class Messages {

  public static final Message GENERAL_CMD_PLAYER_ONLY = new MessageBuilder("general.must_be_player")
      .withDefault("<red>Only players can use this command.")
      .build();
  public static final Message GENERAL_NO_PERM = new MessageBuilder("general.no_perm")
      .withDefault("<red>You don't have sufficient permission")
      .build();
  public static final Message GENERAL_CMD_NO_PERM = new MessageBuilder("general.cmd.no_perm")
      .withDefault("<red>You don't have permission to use this command.")
      .build();
  public static final Message GENERAL_PLAYER_NOT_FOUND = new MessageBuilder("general.player_not_found")
      .withDefault("<red>Player not found: <name>")
      .build();

  public static final Message DEITY_BLESSED = new MessageBuilder("deity.blessed")
      .withDefault("<green><deity> has blessed you with <blessing>!")
      .withPlaceholders("deity", "blessing")
      .build();
  public static final Message DEITY_CURSED = new MessageBuilder("deity.cursed")
      .withDefault("<green><deity> has cursed you with <curse>!")
      .withPlaceholders("deity", "curse")
      .build();
  public static final Message DEITY_CMD_USAGE = new MessageBuilder("deity.command.usage")
      .withDefault("<yellow>Usage: /deity <select|info> [DeityName]")
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
      .withDefault("<green>Meditation complete! You can now move.")
      .build();
  public static final Message MEDIDATION_CANCELLED = new MessageBuilder("meditation.cancelled")
      .withDefault("<red>You moved during meditation! Restarting timer...")
      .build();

  public static final Message SHRINE_NO_PERM_LIST = new MessageBuilder("shrines.list.no_perm")
      .withDefault("<red>You don't have permission to list shrines.")
      .build();
  public static final Message SHRINE_NO_DESIGNATED_SHRINE = new MessageBuilder("shrines.list.no_shrines")
      .withDefault("<red>There are no designated shrines.")
      .build();
  public static final Message SHRINE_SHRINE = new MessageBuilder("shrines.list.shrine")
      .withDefault("<hover:show_text:Click to teleport><click:run_cmd:/teleport @p <x> <y> <z>><deity> at <x>, <y>, <z>")
      .withPlaceholders("deity", "x", "y", "z")
      .build();
  public static final Message SHRINE_NO_PERM_REMOVE = new MessageBuilder("shrines.remove.no_perm")
      .withDefault("<red>You don't have permission to remove shrines.")
      .build();
  public static final Message SHRINE_NO_PERM_SET = new MessageBuilder("shrines.set.no_perm")
      .withDefault("<red>You don't have permission to set a shrine.")
      .build();
  public static final Message SHRINE_RC_TO_REMOVE = new MessageBuilder("shrines.right_click_to_remove")
      .withDefault("<yellow>Right-click on a shrine to remove it.")
      .build();
  public static final Message SHRINE_LIMIT_REACHED = new MessageBuilder("shrines.limit_reached")
      .withDefault("<red>You have reached the maximum number of shrines (<limit>).")
      .withPlaceholder("limit")
      .build();
  public static final Message SHRINE_FOLLOW_DEITY_TO_DESIGNATE = new MessageBuilder("shrines.follow_deity_to_designate")
      .withDefault("<red>You need to follow a deity to designate a shrine!")
      .build();
  public static final Message SHRINE_CLICK_BLOCK_TO_DESIGNATE = new MessageBuilder("shrines.click_block_to_create")
      .withDefault("<yellow>Right-click on a block to designate it as a shrine for <deity>")
      .withPlaceholder("deity")
      .build();
  public static final Message SHRINE_SUCCESS = new MessageBuilder("shrines.shrine_created")
      .withDefault("<green>Successfully designated a shrine for <deity>!")
      .withPlaceholder("deity")
      .build();
  public static final Message SHRINE_DEITY_NOT_FOUND = new MessageBuilder("shrines.deity_not_found")
      .withDefault("<red>Could not determine your deity, please inform an admin.")
      .build();
  public static final Message SHRINE_REMOVED = new MessageBuilder("shrines.remove.success")
      .withDefault("<green>Shrine removed successfully!")
      .build();
  public static final Message SHRINE_REMOVE_FAIL = new MessageBuilder("shrines.remove.not_found")
      .withDefault("<red>Failed to remove shrine. You might not own it.")
      .build();
  public static final Message SHRINE_REMOVE_NOT_FOUND = new MessageBuilder("shrines.remove.not_found")
      .withDefault("<red>No shrine found at this location.")
      .build();
}
