package me.xidentified.devotions.util;

import de.cubbossa.tinytranslations.Message;
import de.cubbossa.tinytranslations.MessageBuilder;

public class Messages {

	public static final Message GENERAL_PLAYER_NOT_FOUND = new MessageBuilder("general.player_not_found")
			.withDefault("<prefix_negative>Player not found: {name}")
			.build();
	public static final Message DEVOTION_RELOAD_SUCCESS = new MessageBuilder("devotion.reload_success")
			.withDefault("<prefix>Devotions successfully reloaded!")
			.build();
	public static final Message DEVOTION_SET = new MessageBuilder("devotion.set")
			.withDefault("<prefix>You are now devoted to {name}. Your favor is {favor}.")
			.withPlaceholder("name", "favor")
			.build();
	public static final Message DEITY_NOT_FOUND = new MessageBuilder("deity.not_found")
			.withDefault("<prefix_negative>Unknown deity. Please choose a valid deity name.")
			.build();
	public static final Message DEITY_NO_DEITY_FOUND = new MessageBuilder("deity.no_deity_found")
			.withDefault("<prefix_negative>No deities found.")
			.build();
	public static final Message DEITY_BLESSED = new MessageBuilder("deity.blessed")
			.withDefault("<prefix>{deity} has blessed you with {blessing}!")
			.withPlaceholders("deity", "blessing")
			.build();
	public static final Message DEITY_CURSED = new MessageBuilder("deity.cursed")
			.withDefault("<prefix>{deity} has cursed you with {curse}!")
			.withPlaceholders("deity", "curse")
			.build();
	public static final Message DEITY_CMD_USAGE = new MessageBuilder("deity.cmd.usage")
			.withDefault("<offset>Usage: <cmd_syntax>/deity <arg>select|info</arg> <arg_opt>DeityName</arg_opt></cmd_syntax>")
			.build();
	public static final Message DEITY_CMD_SPECIFY_DEITY = new MessageBuilder("deity.cmd.specify_deity")
			.withDefault("<prefix_warning>Please specify the deity you wish to worship.")
			.build();
	public static final Message DEITY_SPECIFY_PLAYER = new MessageBuilder("deity.cmd.specify_player")
			.withDefault("<prefix_warning>Please specify the deity whose information you wish to view.")
			.build();
	public static final Message DEITY_LIST_HEADER = new MessageBuilder("deity.list.header")
			.withDefault("<primary>Available Deities:")
			.build();
	public static final Message DEITY_LIST_ENTRY = new MessageBuilder("deity.list.entry")
			.withDefault("<text>- {name}</text>")
			.withPlaceholder("name")
			.build();
	public static final Message DEITY_INFO = new MessageBuilder("deity.info")
			.withDefault("""
					<primary>Details of {name}
					<offset>Lore: <text>{lore}
					<offset>Domain: <text>{domain}
					<offset>Alignment: <text>{alignment}
					<offset>Favored Rituals: <text>{rituals}
					<offset>Favored Offerings: <text>{offerings}""")
			.withPlaceholders("name", "lore", "domain", "alignment", "rituals", "offerings")
			.build();

	public static final Message MEDITATION_COMPLETE = new MessageBuilder("meditation.complete")
			.withDefault("<prefix>Meditation complete! You can now move.")
			.build();
	public static final Message MEDIDATION_CANCELLED = new MessageBuilder("meditation.cancelled")
			.withDefault("<prefix_negative>You moved during meditation! Restarting timer...")
			.build();

	public static final Message SHRINE_NO_PERM_LIST = new MessageBuilder("shrines.list.no_perm")
			.withDefault("<prefix_negative>You don't have permission to list shrines.")
			.build();
	public static final Message SHRINE_NO_DESIGNATED_SHRINE = new MessageBuilder("shrines.list.no_shrines")
			.withDefault("<prefix_negative>There are no designated shrines.")
			.build();
	public static final Message SHRINE_LIST = new MessageBuilder("shrines.list.header")
			.withDefault("<offset>Shrines:")
			.build();
	public static final Message SHRINE_INFO = new MessageBuilder("shrines.list.shrine")
			.withDefault("<click:run_command:\"/teleport @p {x} {y} {z}\"><hover:show_text:Click to teleport>{deity} at {x:#.#}, {y:#.#}, {z:#.#}</hover></click>")
			.withPlaceholders("deity", "x", "y", "z")
			.build();
	public static final Message SHRINE_NO_PERM_REMOVE = new MessageBuilder("shrines.remove.no_perm")
			.withDefault("<prefix_negative>You don't have permission to remove shrines.")
			.build();
	public static final Message SHRINE_NO_PERM_SET = new MessageBuilder("shrines.set.no_perm")
			.withDefault("<prefix_negative>You don't have permission to set a shrine.")
			.build();
	public static final Message SHRINE_RC_TO_REMOVE = new MessageBuilder("shrines.right_click_to_remove")
			.withDefault("<prefix_warning>Right-click on a shrine to remove it.")
			.build();
	public static final Message SHRINE_LIMIT_REACHED = new MessageBuilder("shrines.limit_reached")
			.withDefault("<prefix_negative>You have reached the maximum number of shrines ({limit}).")
			.withPlaceholder("limit")
			.build();
	public static final Message SHRINE_FOLLOW_DEITY_TO_DESIGNATE = new MessageBuilder("shrines.follow_deity_to_designate")
			.withDefault("<prefix_negative>You need to follow a deity to designate a shrine!")
			.build();
	public static final Message SHRINE_NOT_FOLLOWING_DEITY = new MessageBuilder("shrines.not_following_deity")
			.withDefault("<prefix_negative>Only followers of {deity} may use this shrine.")
			.withPlaceholder("deity")
			.build();
	public static final Message SHRINE_COOLDOWN = new MessageBuilder("shrines.cooldown")
			.withDefault("<prefix_negative>You must wait {cooldown:'m'}m {cooldown:'s'}s before performing another ritual.")
			.build();
	public static final Message SHRINE_ALREADY_EXISTS = new MessageBuilder("shrines.cooldown")
			.withDefault("<prefix_negative>A shrine already exists at this location!")
			.build();
	public static final Message SHRINE_CLICK_BLOCK_TO_DESIGNATE = new MessageBuilder("shrines.click_block_to_create")
			.withDefault("<prefix_warning>Right-click on a block to designate it as a shrine for {deity}")
			.withPlaceholder("deity")
			.build();
	public static final Message SHRINE_SUCCESS = new MessageBuilder("shrines.shrine_created")
			.withDefault("<prefix>Successfully designated a shrine for {deity}!")
			.withPlaceholder("deity")
			.build();
	public static final Message SHRINE_DEITY_NOT_FOUND = new MessageBuilder("shrines.deity_not_found")
			.withDefault("<prefix_negative>Could not determine your deity, please inform an admin.")
			.build();
	public static final Message SHRINE_REMOVED = new MessageBuilder("shrines.remove.success")
			.withDefault("<prefix>Shrine removed successfully!")
			.build();
	public static final Message SHRINE_REMOVE_FAIL = new MessageBuilder("shrines.remove.not_found")
			.withDefault("<prefix_negative>Failed to remove shrine. You might not own it.")
			.build();
	public static final Message SHRINE_REMOVE_NOT_FOUND = new MessageBuilder("shrines.remove.not_found")
			.withDefault("<prefix_negative>No shrine found at this location.")
			.build();
	public static final Message SHRINE_OFFERING_ACCEPTED = new MessageBuilder("shrines.offering_accepted")
			.withDefault("<prefix>Your offering has been accepted!")
			.build();
	public static final Message SHRINE_PLACE_ON_TOP = new MessageBuilder("shrines.cannot_place_on_top")
			.withDefault("<prefix_negative>You cannot place blocks on top of a shrine!")
			.build();
	public static final Message SHRINE_CANNOT_BREAK = new MessageBuilder("shrines.cannot_break_shrines")
			.withDefault("<prefix_negative>You cannot destroy shrines! Remove with <cmd_syntax>/shrine remove</cmd_syntax>.")
			.build();
	public static final Message SHRINE_OFFERING_DECLINED = new MessageBuilder("shrines.offering_declined")
			.withDefault("<prefix_negative>Your offering was not accepted by {subject}.")
			.withPlaceholder("subject")
			.build();

	public static final Message FAVOR_CMD_USAGE = new MessageBuilder("favor.cmd.usage")
			.withDefault("<prefix_warning>Usage: <cmd_syntax>/favor <arg>set|give|take</arg> <arg>playername</arg> <arg>amount</arg></cmd_syntax>")
			.build();
	public static final Message FAVOR_CMD_PLAYER_DOESNT_WORSHIP = new MessageBuilder("favor.cmd.player_does_not_worship")
			.withDefault("<prefix_negative>{player} doesn't worship any deity.")
			.withPlaceholder("player")
			.build();
	public static final Message FAVOR_CMD_INVALID_ACTION = new MessageBuilder("favor.cmd.invalid_action")
			.withDefault("<prefix_negative>Invalid action. Use set, give, or take.")
			.build();
	public static final Message FAVOR_CMD_NUMBER_FORMAT = new MessageBuilder("favor.cmd.invalid_number_format")
			.withDefault("<prefix_negative>Invalid amount. Please enter a number.")
			.build();
	public static final Message FAVOR_CURRENT = new MessageBuilder("favor.amount.current_favor")
			.withDefault("<offset>Your current favor with {deity} is <favor_col>{favor}")
			.withPlaceholder("deity")
			.withPlaceholder("favor")
			.withPlaceholder("favor_col")
			.build();
	public static final Message FAVOR_NO_DEVOTION_SET = new MessageBuilder("favor.cmd.no_devotion_set")
			.withDefault("<prefix_negative>You don't have any devotion set.")
			.build();
	public static final Message FAVOR_SET_TO = new MessageBuilder("favor.amount.set_to")
			.withDefault("<offset>Your favor has been set to <favor_col>{favor}")
			.withPlaceholders("favor", "favor_col")
			.build();
	public static final Message FAVOR_INCREASED = new MessageBuilder("favor.amount.favor_increased")
			.withDefault("<prefix>Your favor with {deity} has increased to <favor_col>{favor}")
			.build();
	public static final Message FAVOR_DECREASED = new MessageBuilder("favor.amount.favor_decreased")
			.withDefault("<prefix_negative>Your favor with {deity} has decreased to <favor_col>{favor}")
			.withPlaceholders("favor", "favor_col", "deity")
			.build();
	public static final Message RITUAL_CMD_USAGE = new MessageBuilder("ritual.cmd.usage")
			.withDefault("<prefix_warning>Usage: <cmd_syntax>/ritual <arg>info</arg> <arg_opt>RitualName</arg_opt></cmd_syntax>")
			.build();
	public static final Message RITUAL_CMD_SPECIFY = new MessageBuilder("ritual.cmd.specify_ritual")
			.withDefault("<offset>Please specify the ritual you'd like to lookup information for.")
			.build();
	public static final Message RITUAL_INFO = new MessageBuilder("ritual.info")
			.withDefault("""
					<primary>Details of {display-name}
					<offset>Description: <text>{description}
					<offset>Key Item: <text>{item-name}
					<offset>Favor Rewarded: <text>{favour-amount}""")
			.withPlaceholders("display-name", "description", "item-name", "favour-amount")
			.build();
	public static final Message RITUAL_NOT_FOUND = new MessageBuilder("ritual.not_found")
			.withDefault("<prefix_negative>Unknown ritual. Please choose a valid ritual name.")
			.build();
	public static final Message RITUAL_START = new MessageBuilder("ritual.start")
			.withDefault("<accent>The {ritual} has begun...")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_FAILURE = new MessageBuilder("ritual.failure")
			.withDefault("<prefix_negative>{ritual} has failed - the conditions were not met!")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_SUCCESS = new MessageBuilder("ritual.success")
			.withDefault("<prefix>{ritual} was a success! Blessings upon ye!")
			.withPlaceholder("ritual")
			.build();
	public static final Message RITUAL_RETURN_TO_RESUME = new MessageBuilder("ritual.return_to_resume")
			.withDefault("<accent>Return to the shrine to complete the ritual.")
			.build();

	public static final Message MIRACLE_CMD_USAGE = new MessageBuilder("miracle.cmd.usage")
			.withDefault("<prefix_warning>Usage: <cmd_syntax>/testmiracle <arg>number</arg></cmd_syntax>")
			.build();
	public static final Message MIRACLE_CMD_NO_MIRACLES = new MessageBuilder("miracle.cmd.no_miracles")
			.withDefault("<prefix_negative>No miracles are loaded.")
			.build();
	public static final Message MIRACLE_CMD_AVAILABLE = new MessageBuilder("miracles.cmd.list_available")
			.withDefault("<prefix>Available miracles: <offset>{miracles}")
			.withPlaceholder("miracles")
			.build();
	public static final Message MIRACLE_CMD_UNKNOWN_MIRACLE = new MessageBuilder("miracle.cmd.unknown_miracle")
			.withDefault("<prefix_negative>Unknown miracle")
			.withPlaceholder("miracle")
			.build();
	public static final Message MIRACLE_BESTOWED = new MessageBuilder("miracle.bestowed")
			.withDefault("<prefix>A miracle has been bestowed upon ye!")
			.build();
	public static final Message MIRACLE_SAVED_FROM_DEATH = new MessageBuilder("miracle.saved_from_death")
			.withDefault("<prefix>A miracle has revived you upon death!")
			.build();
	public static final Message MIRACLE_HERO_OF_VILLAGE = new MessageBuilder("miracle.hero_of_the_village")
			.withDefault("<prefix>A miracle has granted you the Hero of the Village effect!")
			.build();
	public static final Message MIRACLE_FIRE_RESISTANCE = new MessageBuilder("miracle.fire_resistance")
			.withDefault("<prefix>A miracle has granted you Fire Resistance!")
			.build();
	public static final Message MIRACLE_REPAIR = new MessageBuilder("miracle.repair")
			.withDefault("<prefix>A miracle has repaired all your items!")
			.build();
	public static final Message MIRACLE_HARVEST = new MessageBuilder("miracle.harvest")
			.withDefault("<prefix>A miracle has blessed you with a bountiful harvest!")
			.build();
	public static final Message MIRACLE_GOLEM = new MessageBuilder("miracle.iron_golem")
			.withDefault("<prefix>A miracle has summoned Iron Golems to aid you!")
			.build();
	public static final Message MIRACLE_WOLVES = new MessageBuilder("miracle.wolves")
			.withDefault("<prefix>A miracle has summoned friendly Wolves to protect you!")
			.build();

	public static final Message VERSION_INFO = new MessageBuilder("devotions.admin")
			.withDefault("<prefix_warning>Server version: <gray>{server-ver}</gray>\nArchGPT version: <gray>{plugin-ver}</gray>\nJava version: <gray>{java-ver}</gray>")
			.withPlaceholder("server-ver")
			.withPlaceholder("plugin-ver")
			.withPlaceholder("java-ver")
			.build();
}
