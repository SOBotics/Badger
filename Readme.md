# Badger

![](https://i.stack.imgur.com/IuV0f.jpg)

[Badger](http://codereview.stackexchange.com/questions/148570/badger-the-tumbleweed-detector)  queries the [Stack Exchange API](http://api.stackexchange.com/), specifically for [badges](https://api.stackexchange.com/docs/badges). 

The main idea of Badger was to return the new list of Tumbleweed posts on Stack Overflow, every 10 minutes. The motivation for this bot was the [Weed Eater](http://winterbash2015.stackexchange.com/weed-eater) hat. Badger now tracks other badges also. 

Badger queries the API every 10 mins. If there are new badges, Then it'll print the number of new posts with a link to that particular badge page. If there are no new ones, It waits for another 10 minutes. 

[Badger](http://stackoverflow.com/users/7240793/badger) now runs in the [SOBotics room](http://chat.stackoverflow.com/rooms/111347/sobotics) along with its other bot friends. Do visit them and say "Hi". 

The command list is as follows 

    alive    - Test to check if the bot is alive or not.
    help     - Returns description of the bot.
    commands - Returns this list of commands.
    track    - Tracks a given badge. Syntax: track badgeId badgeName
    untrack  - UnTracks a given badge.
    tracked  - Returns a list of tracked badges.


To run this bot under your account:

1. Create a new folder named `data` at the same level as the `pom.xml`.
2. Create a new file named `trackedBadges.txt` inside `data`. This contains a list of the badges that are needed to be tracked. Sample file contents is 
        
        63,Tumbleweed
        51,Python

3. Create a new file named `login.properties` inside `data`. This contains 4 properties, `email`, `password`, `username`, and `roomId`. Sample file contents is 
        
        email=mymail@mailprovider.com
        password=changethis
        roomId=111347
        username=nameofthebot

4. Run the main method. 

---------------

See the other [wiki pages](/Badger/wiki).