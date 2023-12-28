package me.xidentified.devotions.util;

import de.cubbossa.translations.Message;
import de.cubbossa.translations.MessageBuilder;

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
	public static final Message DEVOTION_RELOAD_SUCCESS = new MessageBuilder("devotion.reload_success")
			.withDefault("<positive>Devotions successfully reloaded!")
			.build();
	public static final Message DEVOTION_SET = new MessageBuilder("devotion.set")
			.withDefault("<positive>You are now devoted to <name>. Your favor is <favor>.")
			.withPlaceholder("name", "favor")
			.build();
	public static final Message DEITY_NOT_FOUND = new MessageBuilder("deity.not_found")
			.withDefault("<negative>Unknown deity. Please choose a valid deity name.")
			.build();
	public static final Message DEITY_NO_DEITY_FOUND = new MessageBuilder("deity.no_deity_found")
			.withDefault("<negative>No deities found.")
			.build();
	public static final Message DEITY_BLESSED = new MessageBuilder("deity.blessed")
			.withDefault("<positive><deity> has blessed you with <blessing>!")
			.withPlaceholders("deity", "blessing")
			.build();
	public static final Message DEITY_CURSED = new MessageBuilder("deity.cursed")
			.withDefault("<positive><deity> has cursed you with <curse>!")
			.withPlaceholders("deity", "curse")
			.build();
	public static final Message DEITY_CMD_USAGE = new MessageBuilder("deity.cmd.usage")
			.withDefault("<yellow>Usage: /deity <select|info> [DeityName]")
			.build();
	public static final Message DEITY_CMD_SPECIFY_DEITY = new MessageBuilder("deity.cmd.specify_deity")
			.withDefault("<yellow>Please specify the deity you wish to worship.")
			.build();
	public static final Message DEITY_SPECIFY_PLAYER = new MessageBuilder("deity.cmd.specify_player")
			.withDefault("<warning>Please specify the deity whose information you wish to view.")
			.build();
	public static final Message DEITY_LIST_HEADER = new MessageBuilder("deity.list.header")
			.withDefault("<gold>Available Deities:")
			.build();
	public static final Message DEITY_LIST_ENTRY = new MessageBuilder("deity.list.entry")
			.withDefault("<gray>- <name></gray>")
			.withPlaceholder("name")
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
	public static final Message SHRINE_LIST = new MessageBuilder("shrines.list.header")
			.withDefault("<yellow>Shrines:")
			.build();
	public static final Message SHRINE_INFO = new MessageBuilder("shrines.list.shrine")
			.withDefault("<hover:show_text:Click to teleport><click:run_command:'/teleport @p <x> <y> <z>'><deity> at <x:#.#>, <y:#.#>, <z:#.#>")
			.withPlaceholders("deity", "x", "y", "z")
			.build();
	public static final Message SHRINE_NO_PERM_REMOVE = new MessageBuilder("shrines.remove.no_perm")
			.withDefault("<negative>You don't have permission to remove shrines.")
			.build();
	public static final Message SHRINE_NO_PERM_SET = new MessageBuilder("shrines.set.no_perm")
			.withDefault("<negative>You don't have permission to set a shrine.")
			.build();
	public static final Message SHRINE_RC_TO_REMOVE = new MessageBuilder("shrines.right_click_to_remove")
			.withDefault("<warning>Right-click on a shrine to remove it.")
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
	public static final Message SHRINE_COOLDOWN = new MessageBuilder("shrines.cooldown")
			.withDefault("<negative>You must wait <cooldown:'m'>m <cooldown:'s'>s before performing another ritual.")
			.build();
	public static final Message SHRINE_ALREADY_EXISTS = new MessageBuilder("shrines.cooldown")
			.withDefault("<negative>A shrine already exists at this location!")
			.build();
	public static final Message SHRINE_CLICK_BLOCK_TO_DESIGNATE = new MessageBuilder("shrines.click_block_to_create")
			.withDefault("<warning>Right-click on a block to designate it as a shrine for <deity>")
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
			.withDefault("<negative>You cannot destroy shrines! Remove with <warning>/shrine remove</warning>.")
			.build();
	public static final Message SHRINE_OFFERING_DECLINED = new MessageBuilder("shrines.offering_declined")
			.withDefault("<negative>Your offering was not accepted by <subject>.")
			.withPlaceholder("subject")
			.build();

	public static final Message FAVOR_CMD_USAGE = new MessageBuilder("favor.cmd.usage")
			.withDefault("<warning>Usage: /favor <set|give|take> <playername> <amount>")
			.build();
	public static final Message FAVOR_CMD_PLAYER_DOESNT_WORSHIP = new MessageBuilder("favor.cmd.player_does_not_worship")
			.withDefault("<negative><player> doesn't worship any deity.")
			.withPlaceholder("player")
			.build();
	public static final Message FAVOR_CMD_INVALID_ACTION = new MessageBuilder("favor.cmd.invalid_action")
			.withDefault("<negative>Invalid action. Use set, give, or take.")
			.build();
	public static final Message FAVOR_CMD_NUMBER_FORMAT = new MessageBuilder("favor.cmd.invalid_number_format")
			.withDefault("<negative>Invalid amount. Please enter a number.")
			.build();
	public static final Message FAVOR_CURRENT = new MessageBuilder("favor.amount.current_favor")
			.withDefault("<yellow>Your current favor with <deity> is <color><favor>")
			.withPlaceholder("deity")
			.withPlaceholder("favor")
			.build();
	public static final Message FAVOR_NO_DEVOTION_SET = new MessageBuilder("favor.cmd.no_devotion_set")
			.withDefault("<negative>You don't have any devotion set.")
			.build();
	public static final Message FAVOR_SET_TO = new MessageBuilder("favor.amount.set_to")
			.withDefault("<yellow>Your favor has been set to <color><favor>")
			.build();
	public static final Message FAVOR_INCREASED = new MessageBuilder("favor.amount.favor_increased")
			.withDefault("<positive>Your favor with <deity> has increased to <color><favor>")
			.build();
	public static final Message FAVOR_DECREASED = new MessageBuilder("favor.amount.favor_decreased")
			.withDefault("<negative>Your favor with <deity> has decreased to <color><favor>")
			.build();
	public static final Message RITUAL_CMD_USAGE = new MessageBuilder("ritual.cmd.usage")
			.withDefault("<warning>Usage: /ritual <info> [RitualName]")
			.build();
	public static final Message RITUAL_CMD_SPECIFY = new MessageBuilder("ritual.cmd.specify_ritual")
			.withDefault("<yellow>Please specify the ritual you'd like to lookup information for.")
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
	public static final Message RITUAL_START = new MessageBuilder("ritual.start")
			.withDefault("<light_purple>The <ritual> has begun...")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_FAILURE = new MessageBuilder("ritual.failure")
			.withDefault("<positive><ritual> was a success! Blessings upon ye!")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_SUCCESS = new MessageBuilder("ritual.success")
			.withDefault("<negative><ritual> has failed - the conditions were not met!")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_RETURN_TO_RESUME = new MessageBuilder("ritual.return_to_resume")
			.withDefault("<light_purple>Return to the shrine to complete the ritual.")
			.build();

	public static final Message MIRACLE_CMD_USAGE = new MessageBuilder("miracle.cmd.usage")
			.withDefault("<warning>Usage: /testmiracle <number>")
			.build();
	public static final Message MIRACLE_CMD_NO_MIRACLES = new MessageBuilder("miracle.cmd.no_miracles")
			.withDefault("<negative>No miracles are loaded.")
			.build();
	public static final Message MIRACLE_CMD_AVAILABLE = new MessageBuilder("miracles.cmd.list_available")
			.withDefault("<positive>Available miracles: <yellow><miracles>")
			.withPlaceholder("miracles")
			.build();
	public static final Message MIRACLE_CMD_UNKNOWN_MIRACLE = new MessageBuilder("miracle.cmd.unknown_miracle")
			.withDefault("<negative>Unknown miracle")
			.withPlaceholder("miracle")
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
