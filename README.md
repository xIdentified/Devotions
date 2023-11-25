# Devotions - Deities and Blessings

## Overview
Devotions is a Minecraft plugin that adds a highly customizeable deity worship system to your server. Players can select deities to worship, perform rituals, make offerings, and gain blessings, curses, or miracles based on their favor amount.

## Features
- **Deity Selection**: Players can choose a deity to worship from a list of available deities, each with unique lore and details.
- **Rituals and Offerings**: Engage in rituals and make offerings to gain favor or avoid the wrath of your chosen deity.
- **Shrines / Altars:** Dedicate a shrine to your favored deity. Set a limit for the amount of shrines players can create.
- **Blessings and Curses**: Based on the level of devotion, players can receive blessings or suffer curses.
- **Favor System**: Players earn favor with their deity, which can decay over time if not maintained.
- **PlaceholderAPI Support**: Integration with PAPI for placeholders like `%devotions_deity%`, `%devotions_favor%`, and `%devotions_favor_top%`.
- **Configurable**: Fully configurable settings to tailor the experience to your server’s needs including deities, rituals, and sounds.

## Installation
1. Download the latest jar from [Spigot]([link-to-spigot-page](https://www.spigotmc.org/resources/devotions-deities-and-blessings-⛧†.113549/)).
2. Place the downloaded `.jar` file into your server's `plugins` directory.
3. Restart your Minecraft server.
4. Customize the plugin using `config.yml`, `deities.yml`, `rituals.yml`, and `sounds.yml`.

## Commands and Permissions
- `/deity list` - List all available deities.
- `/deity select <DeityName>` - Select a deity to worship.
- `/deity info <DeityName>` - Get information about a specific deity.
- `/shrine` - Devote a shrine to a deity.
- `/shrine list` - List and teleport to all shrines.
- `/shrine remove` - Remove an existing shrine.
- `/favor <set/give/take>` - Set, give, or take favor from another player. For admin use.
- `/ritual info <ritualName>` - View details for all configured rituals.
- `/testmiracle <miracleName>` - Test miracles to ensure they're working as intended - admin use.
- `/devotions reload` - Reloads the plugin. Not recommended, you should restart your server.

## Configuration
Devotions allows extensive customization through its configuration file. Adjust favor settings, deity attributes, rituals, and more to fit your server's theme and balance.

## Compatibility
Developed and tested using Paper 1.20

## Support
For support, questions, or more information, please visit my [Discord server]((https://discord.com/invite/yRrbBjfbXp)).

## Contributing
Contributions are welcome! If you would like to contribute to the development of Devotions, please submit a pull request or open an issue on this repository.

Made by xIdentified
