# Badger 

Badger queries the [Stack Exchange API](http://api.stackexchange.com/), specifically for [badges](https://api.stackexchange.com/docs/badges). 

The main idea of Badger was to return the new list of Tumbleweed posts on Stack Overflow, every 10 minutes. The motivation for this bot was the [Weed Eater](http://winterbash2015.stackexchange.com/weed-eater) hat. Badger now track other badges also. 

Badger queries the API every 10 mins. If there are new badges, Then it'll print the number of new posts with a link to that particular badge page. If there are no new ones, It waits for another 10 minutes. 

[Badger](http://stackoverflow.com/users/7240793/badger) now runs in the [SOBotics room](http://chat.stackoverflow.com/rooms/111347/sobotics) along with its other bot friends. Do visit them and say "Hi". 

The command list is as follows 

    alive    - Test to check if the bot is alive or not.
    help     - Returns description of the bot
    commands - Returns this list of commands
    track    - Tracks a given badge. Syntax: track badgeId badgeName

