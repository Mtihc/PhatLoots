package com.codisimus.plugins.phatloots;

import java.util.Calendar;
import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Dispenser;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

/**
 * A PhatLoots is a reward made up of money and items
 * 
 * @author Codisimus
 */
public class PhatLoots {
    public String name; //A unique name for the Warp
    public int numberCollectiveLoots = PhatLootsMain.defaultNumberOfLoots; //Amount of loots received from each collective loot
    
    public int rangeLow; //Range of money that may be given
    public int rangeHigh;
    
    public LinkedList<Loot>[] loots = (LinkedList<Loot>[])new LinkedList[6]; //List of items that may be given

    public int days = PhatLootsMain.defaultDays; //Reset time (will never reset if any are negative) 
    public int hours = PhatLootsMain.defaultHours;
    public int minutes = PhatLootsMain.defaultMinutes;
    public int seconds = PhatLootsMain.defaultSeconds;

    public boolean global = false; //Reset Type
    
    public LinkedList<PhatLootsChest> chests = new LinkedList<PhatLootsChest>(); //List of PhatLootsChest that activate the Warp

    /**
     * Constructs a new PhatLoots
     * 
     * @param name The name of the PhatLoots which will be created
     */
    public PhatLoots (String name) {
        this.name = name;
        
        //Initialize lits of loots
        for (int i = 0; i < 6; i++)
            loots[i] = new LinkedList<Loot>();
    }

    /**
     * Activates the PhatLoots by checking for remaining time and receiving loots
     * 
     * @param player The Player who is looting
     * @param block The Block being looted
     */
    public void getLoot(Player player, Block block) {
        //Retrieve the PhatLootsChest associated with the given Block
        PhatLootsChest chest = findChest(block);
        
        //Get the user to be looked up for last time of use
        String user = player.getName();
        if (global)
            user = "global";
        
        //Find out how much time remains
        String timeRemaining = getTimeRemaining(chest.getTime(user));
        
        //User can never loot the Chest again if timeRemaining == -1
        if (timeRemaining.equals("-1")) {
            if (PhatLootsMain.displayTimeRemaining)
                player.sendMessage(PhatLootsMain.canOnlyLootOnceMsg);
            
            return;
        }
        
        //Display remaining time if it is not 0
        if (!timeRemaining.equals("0")) {
            if (PhatLootsMain.displayTimeRemaining)
                player.sendMessage(PhatLootsMain.timeRemainingMsg.replaceAll("<time>", timeRemaining));
            
            return;
        }
        
        //Roll for money amount if the range is above 0
        if (rangeHigh > 0) {
            int amount = PhatLootsMain.random.nextInt((rangeHigh + 1) - rangeLow);
            amount = amount + rangeLow;
            
            //Give money to the Player if there is money to give
            if (amount > 0) {
                String money = Register.reward(player.getName(), amount);
                player.sendMessage(money+" added to your account!");
            }
        }
        
        //Give individual loots
        lootIndividual(player, block);
        
        //Give collective loots
        lootCollective(player, block);
        
        //Set the new time for the User and return true
        chest.setTime(user);
        return;
    }

    /**
     * Returns the remaining time until the PhatLootsChest resets
     * Returns -1 if the PhatLootsChest never resets
     * 
     * @param time The given time
     * @return the remaining time until the PhatLootsChest resets
     */
    public String getTimeRemaining(int[] time) {
        //Return 0 if a time was not given
        if (time == null)
            return "0";
        
        //Return -1 if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0)
            return "-1";
        
        //Calculate the time that the Warp will reset
        int resetDay = time[0] + days;
        int resetHour = time[1] + hours;
        int resetMinute = time[2] + minutes;
        int resetSecond = time[3] + seconds;
        
        //Update time values into the correct format
        while (resetSecond >= 60) {
            resetMinute++;
            resetSecond = resetSecond - 60;
        }
        while (resetMinute >= 60) {
            resetHour++;
            resetMinute = resetMinute - 60;
        }
        while (resetHour >= 24) {
            resetDay++;
            resetHour = resetHour - 24;
        }
        
        Calendar calendar = Calendar.getInstance();
        
        //Return 0 if the current time is later than the reset time
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        if (day > resetDay)
            return "0";
        
        if (day < resetDay)
            //Display remaining days
            return (resetDay - day)+" days";
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour > resetHour)
            return "0";
        
        if (hour < resetHour)
            //Display remaining hours
            return (resetHour - hour)+" hours";
        
        int minute = calendar.get(Calendar.MINUTE);
        if (minute > resetMinute)
            return "0";
        
        if (minute < resetMinute)
            //Display remaining minutes
            return (resetMinute - minute)+" minutes";
        
        int second = calendar.get(Calendar.SECOND);
        if (second >= resetSecond)
            return "0";
        else
            //Display remaining seconds
            return (resetSecond - second)+" seconds";
    }
    
    /**
     * Fills the Chest (Block) with loot
     * Each item is rolled for to determine if it will by added to the Chest
     * Money is rolled for to determine how much will be given within the range
     * 
     * @param player The Player looting
     * @param block The Block being looted
     */
    public void lootIndividual(Player player, Block block) {
        for (Loot loot: loots[0])
            //Roll for item
            if (PhatLootsMain.random.nextInt(100) < loot.probability)
                lootItem(loot.item, player, block);
    }

    /**
     * Fills the Chest (Block) with loot
     * Items are rolled for in order until the maximum number is added to the Chest
     * 
     * @param collectiveLoots The String that contains the items and percentages
     * @param block The Block being looted
     */
    public void lootCollective(Player player, Block block) {
        //Loot from each of the 5 collective loots
        for (int i = 1 ; i <= 5; i++) {
            int numberLooted = 0;

            //Make sure there are items that will be looted before entering loop
            if (!loots[i].isEmpty() && getPercentRemaining(i) < 100)
                while (numberLooted < numberCollectiveLoots)
                    for (Loot loot: loots[i])
                        //Roll for item
                        if (PhatLootsMain.random.nextInt(100) < loot.probability) {
                            lootItem(loot.item, player, block);

                            //Break if the Player looted enough items
                            if (numberLooted++ >= numberCollectiveLoots)
                                break;
                        }
        }
    }

    /**
     * Fills the Chest (Block) with loot
     * Items are rolled for in order until the maximum number is added to the Chest
     * 
     * @param collectiveLoots The String that contains the items and percentages
     * @param block The Block being looted
     */
    public void lootItem(ItemStack item, Player player, Block block) {
        PlayerInventory sack = player.getInventory();

        if (block.getTypeId() == 23) {
            //Add the item to the Dispenser inventory
            Dispenser dispenser = (Dispenser)block.getState();
            Inventory inventory = dispenser.getInventory();
            inventory.clear();
            inventory.addItem(item);

            //Dispense until the Dispenser is empty
            while (inventory.firstEmpty() > 0)
                dispenser.dispense();
        }
        else if (PhatLootsMain.autoLoot && sack.firstEmpty() != -1) {
            //Add the Loot to the Player's Inventory
            player.sendMessage(PhatLootsMain.autoLootMsg.replaceAll("<item>", item.getType().name()));
            sack.addItem(item);
        }
        else {
            //Add the Loot to the Chest's Inventory
            Chest chest = (Chest)block.getState();
            chest.getInventory().clear(PhatLootsMain.random.nextInt(15)+10);
            chest.getInventory().addItem(item);
        }
    }
    
    /**
     * Returns the Remaining Percent of the given collective Loots
     * 
     * @param id The id of the collective Loots
     * @return Total probability of all Loots in the collective Loots subtracted from 100
     */
    public int getPercentRemaining(int id) {
        //Subtract the probabilty of each loot from 100
        int total = 100;
        for (Loot loot: loots[id])
            total = total - loot.probability;
        
        return total;
    }

    /**
     * Loads data from the save file
     * 
     * @param id The id of the Loots (0 for individual loots)
     * @param string The data of the Loots
     */
    public void setLoots(int id, String data) {
        //Cancel if no data was provided
        if (data.isEmpty())
            return;
        
        //Load data for each loot
        for (String loot: data.split(", ")) {
            try {
                //Construct a new loot with the item data and probability
                String[] lootData = loot.split("'");
                loots[id].add(new Loot(Integer.parseInt(lootData[0]), Short.parseShort(lootData[1]),
                        Integer.parseInt(lootData[2]), Integer.parseInt(lootData[3])));
            }
            catch (Exception invalidLoot) {
                System.out.println("[PhatLoots] Error occured while loading, '"+loot+"' is not a valid Loot");
                SaveSystem.save = false;
                System.out.println("[PhatLoots] Saving turned off to prevent loss of data");
                invalidLoot.printStackTrace();
            }
        }
    }
    
    /**
     * Loads data from the save file
     * 
     * @param string The data of the Chests
     */
    public void setChests(String data) {
        //Cancel if no data was provided
        if (data.isEmpty())
            return;
        
        int index;
        
        //Load data for each PhatLootsChest
        for (String chest: data.split("; ")) {
            try {
                String[] chestData = chest.split("\\{", 2);

                //Load the Block Location data of the Chest
                String[] blockData = chestData[0].split("'");
                
                //Construct a a new PhatLootsChest with the Location data
                PhatLootsChest phatLootsChest = new PhatLootsChest(blockData[0], Integer.parseInt(blockData[1]),
                        Integer.parseInt(blockData[2]), Integer.parseInt(blockData[3]));

                //Load the HashMap of Users of the Chest
                for (String user: chestData[1].substring(0, chestData[1].length() - 1).split(", "))
                    //Don't load if the data is corrupt or empty
                    if ((index = user.indexOf('@')) != -1) {
                        String[] timeData = user.substring(index + 1).split("'");
                        int[] time = new int[4];

                        for (int j = 0; j < 4; j++)
                            time[j] = Integer.parseInt(timeData[j]);

                        phatLootsChest.users.put(user.substring(0, index), time);
                    }
                
                chests.add(phatLootsChest);
            }
            catch (Exception invalidChest) {
                System.out.println("[PhatLoots] Error occured while loading, "+'"'+chest+'"'+" is not a valid PhatLootsChest");
                SaveSystem.save = false;
                System.out.println("[PhatLoots] Saving turned off to prevent loss of data");
                invalidChest.printStackTrace();
            }
        }
    }
    
    /**
     * Loads data from the outdated save file
     * 
     * @param id The id of the Loots (0 for individual loots)
     * @param string The data of the Loots
     */
    public void setOldLoots(int id, String data) {
        //Cancel if no data was provided
        if (data.isEmpty())
            return;
        
        //Load data for each loot
        for (String loot: data.split("~")) {
            try {
                String[] lootData = loot.split(",");
                
                if (lootData[0].equals("money")) {
                    //Set the money range
                    rangeLow = Integer.parseInt(lootData[1]);
                    rangeHigh = Integer.parseInt(lootData[2]);
                }
                else {
                    int itemID;
                    Short durability = -1;
                    if (lootData[0].contains(".")) {
                        String[] itemData = lootData[0].split(".");
                        itemID = Integer.parseInt(itemData[0]);
                        durability = Short.parseShort(itemData[1]);
                    }
                    else
                        itemID = Integer.parseInt(lootData[0]);

                    Loot lootObject = new Loot(itemID, durability, Integer.parseInt(lootData[1]),Integer.parseInt(lootData[2]));
                    loots[id].add(lootObject);
                }
            }
            catch (Exception invalidLoot) {
                System.out.println("[PhatLoots] Error occured while loading, '"+loot+"' is not a valid Loot");
                SaveSystem.save = false;
                System.out.println("[PhatLoots] Saving turned off to prevent loss of data");
                invalidLoot.printStackTrace();
            }
        }
    }
    
    /**
     * Loads data from the outdated save file
     * 
     * @param string The data of the Chests
     */
    public void setOldChests(String data) {
        //Cancel if no data was provided
        if (data.isEmpty())
            return;
        
        //Load data for each PhatLootsChest
        for (String chest: data.split("~")) {
            try {
                String[] chestData = chest.split(",");
                
                //Construct a a new PhatLootsChest with the Location data
                PhatLootsChest phatLootsChest = new PhatLootsChest(chestData[0], Integer.parseInt(chestData[1]),
                        Integer.parseInt(chestData[2]), Integer.parseInt(chestData[3]));
                
                //Load the HashMap of Users of the Chest
                for (int i = 4; i < chestData.length; i = i + 2) {
                    String[] timeData = chestData[i+1].split("'");
                    int[] time = new int[4];

                    for (int j = 0; j < 4; j++)
                        time[j] = Integer.parseInt(timeData[j]);

                    phatLootsChest.users.put(chestData[i], time);
                }
                
                chests.add(phatLootsChest);
            }
            catch (Exception invalidChest) {
                System.out.println("[PhatLoots] Error occured while loading, "+chest+" is not a valid PhatLootsChest");
                SaveSystem.save = false;
                System.out.println("[PhatLoots] Saving turned off to prevent loss of data");
                invalidChest.printStackTrace();
            }
        }
    }
    
    /**
     * Returns the List of Loots as a String
     * 
     * @param id The id of the Loots
     * @return The List of Loots as a String
     */
    public String getLoots(int id) {
        Short durability;
        String list = "";
        
        //Concat each Loot onto the list
        for (Loot loot: loots[id]) {
            //Add the Loot info
            list = list.concat(", "+loot.item.getAmount()+" of "+loot.item.getType().name()+" @ "+loot.probability+"%");
            
            //Add the durability if it is not negative
            durability = loot.item.getDurability();
            if (durability >= 0)
                list.replace("@", "with durability "+durability+" @");
        }
        
        if (!list.isEmpty())
            list = list.substring(2);
        
        return list;
    }
    
    /**
     * Returns the Button that is associated with the given Block
     * 
     * @param block The given Block
     * @return The Button that is associated with the given Block
     */
    public PhatLootsChest findChest(Block block) {
        //Iterate through chests to find the PhatLootsChest of the given Block
        for (PhatLootsChest chest: chests)
            if (chest.isBlock(block))
                return chest;
        
        //Return null because the Button does not exist
        return null;
    }

    /**
     * Resets the user times for all PhatLootsChests of this PhatLoots
     * If a Block is given then only reset that PhatLootsChest
     * 
     * @param block The given Block
     */
    public void reset(Block block) {
        if (block == null)
            //Reset all PhatLootsChests
            for (PhatLootsChest phatLootsChest: chests)
                phatLootsChest.users.clear();
        else
            //Find the PhatLootsChest of the given Block and reset it
            for (PhatLootsChest phatLootsChest: chests)
                if (phatLootsChest.isBlock(block))
                    phatLootsChest.users.clear();
    }
}