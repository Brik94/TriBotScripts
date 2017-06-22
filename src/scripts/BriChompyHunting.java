package scripts;

import org.tribot.api.DynamicClicking;
import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.util.abc.ABCUtil;
import org.tribot.api2007.Player;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Objects;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSObject;

import java.awt.*;
import java.util.Random;

import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.MessageListening07;
import org.tribot.script.interfaces.Painting;

import static org.tribot.api2007.Walking.clickTileMS;

/**
 * Fix 1: Need to add total number of bellows in players inventory at the beginning.
 * Fix getState 2nd else if statement to include this.
 * Fix 2: Check for correct chompy hunting gear at beginning
 * Fix 3: Placing new toad where bloated toad already exists
 * https://tribot.org/forums/topic/30185-trilezs-scripting-tutorial/
 * https://pastebin.com/v0Y6vAdy     //ABC2 implement
 */


@ScriptManifest(authors = {"Brik94"}, category = "Quests", name = "BriChompyHunting",
        description = "Chompy hunting using an efficient method.")
public class BriChompyHunting extends Script implements Painting, MessageListening07{

    private final int EMPTY_BELLOWS_ID = 2871, TOAD_ID = 1473, BLOATED_TOAD_ID= 1474, INV_TOAD_ID = 2875,
                      ALIVE_CHOMY_ID = 1475, SWAMP_ID = 684;
                        //OGRE_ARROW_ID = 2866, FULL_BELLOWS_ID = 2872,COMP_OGREBOW_ID = 4827, DEAD_CHOMPY_ID = 1476
    private final int[] USUSABLE_BELLOW_ID = {2872, 2873, 2874};
    private RSTile[] tileList = {new RSTile(2334, 3055), new RSTile(2337, 3062), new RSTile(2335, 3058), new RSTile(2338, 3060), new RSTile(2331, 3059)};
    private int killedChompies;
    private State SCRIPT_STATE = getState();
    private Random rand;
    private ABCUtil abc = new ABCUtil(); //AntiBanCompliance

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("Bri's Chompy Killer", 10, 70);
        g.drawString("Total chompies killed: " + killedChompies, 10, 90);
        g.drawString("State: " + SCRIPT_STATE, 10, 110);
    }

    private enum State {
        KILLING_CHOMPY, FILLING_BELLOWS, CATCHING_TOAD, PLACING_TOAD, LOOK_FOR_CHOMPY, PLUCKING_CHOMPY
    }

    private State getState(){
        if (checkForChompy()){ //chompy in sight
            if(numEmptyBellows() == 24){
                println("Less than 1 bellows");
                return State.FILLING_BELLOWS;
            }else {
                return State.KILLING_CHOMPY;
            }
        }else if(numEmptyBellows() != 24) {//there is atleast 1 useable bellow
            if (Inventory.getCount(INV_TOAD_ID) < 1) { //we have no toads
                return State.CATCHING_TOAD;
            } else { //we do have toads
                return State.PLACING_TOAD;
            }
        }else{ //no chompy + no full bellows
            return State.FILLING_BELLOWS;
        }
    }

    @Override
    public void run() {
        killedChompies = 0;
        rand = new Random();
        //noinspection InfiniteLoopStatement
        while(true) {
            sleep(50);
            SCRIPT_STATE = getState();

            switch(SCRIPT_STATE){

                case KILLING_CHOMPY:
                    killChompy();
                    break;
                case FILLING_BELLOWS:
                    fillBellows();
                    break;
                case CATCHING_TOAD:
                    catchToad();
                    break;
                case PLACING_TOAD:
                    placeToad();
                    break;
            }
            //sleep(40, 80);
        }
    }

    private int numEmptyBellows(){
        return Inventory.getCount(EMPTY_BELLOWS_ID);
    }

    private void fillBellows(){

        //if all bellows are empty, fill them up.
        while (numEmptyBellows() != 0){ // returns false
            if (Player.getAnimation() == -1) {
                println("Player standing");
                RSItem[] ebellows = Inventory.find(EMPTY_BELLOWS_ID);
                RSObject[] bubbles = Objects.findNearest(10, SWAMP_ID);
                ebellows[0].click("Use");
                DynamicClicking.clickRSObject(bubbles[0], 1);
                waitUntilIdle();
            }
        }
    }

    private void catchToad(){
            RSItem[] goodBellows = Inventory.find(USUSABLE_BELLOW_ID);
            RSNPC[] toad = NPCs.findNearest(10, TOAD_ID);

            goodBellows[0].click("Use");
            DynamicClicking.clickRSNPC(toad[0], 1);
            waitUntilIdle();
    }

    private void placeToad(){
        RSItem[] bloatedToad = Inventory.find(INV_TOAD_ID);
        bloatedToad[0].click("Drop");
    }

    private boolean checkForChompy(){
        RSNPC[] chompy = NPCs.findNearest(ALIVE_CHOMY_ID);
        if(chompy.length > 0 && chompy[0].getID()!= 1476){
            println("Chompy in sight yes");
            return true;
        }
        return false;
    }

    private void waitUntilIdle(){
        long t = System.currentTimeMillis();

        while(Timing.timeFromMark(t) < General.random(400, 800)){ //400, 800
            sleep(400, 800);

            if(Player.isMoving() || Player.getAnimation() != -1){
                t = System.currentTimeMillis();
                continue;
            }

            sleep(40, 80);

            if(Player.getAnimation() == -1)
                break;
        }
    }

    //deprecated
    private boolean checkGroundForToad() {
        RSObject[] groundToad = Objects.findNearest(5, BLOATED_TOAD_ID);
        if (groundToad.length > 0){
            if (Player.getPosition() == groundToad[0].getPosition()) {
                println("Standing on Toad");
                return true;
            }
        }
        println("Not standing on toad");
        return false;
    }

    private void killChompy(){
        RSNPC[] first_chompy = NPCs.findNearest(50, ALIVE_CHOMY_ID);
        if(checkForChompy()){ //if chompy is here
            //Attack it once. Then check for new chompy. If no new chompy, proceed to attack current chompy.
            println("going for kill");
            first_chompy[0].click("Attack");
            waitUntilIdle();
            killChompy(); //Recursion

        }
    }


    //Methods that have to be generated from MessageListener07------------------------------------------------------
    @Override
    //Increments killed chompies based on server message received.
    //Also checks if standing on toad based on server message received.
    public void serverMessageReceived(String s) {
        sleep(300, 1000);
        if (s.contains("You scratch a notch on your bow for the chompy bird kill.")) {
            println("Server message worked. Chompy killed.");
            killedChompies++;
        }
        if(s.contains("There is a bloated toad already placed at this location.")){
            //Move to another tile.
            RSTile randomTile = tileList[rand.nextInt(tileList.length)];
            clickTileMS(randomTile, 1);
        }
    }

    @Override
    public void clanMessageReceived(String s, String s1) {

    }

    @Override
    public void playerMessageReceived(String s, String s1) {

    }

    @Override
    public void tradeRequestReceived(String s) {

    }

    @Override
    public void duelRequestReceived(String s, String s1) {

    }

    @Override
    public void personalMessageReceived(String s, String s1) {

    }
    //--------------------------------------------------------------------------------------------------------------

}