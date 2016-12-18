# How to run and test Badger

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

----

<sub>Back to [Home](/Badger)</sub>