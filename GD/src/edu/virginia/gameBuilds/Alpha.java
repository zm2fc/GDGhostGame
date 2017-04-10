package edu.virginia.gameBuilds;

import edu.virginia.engine.display.Game;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.virginia.engine.display.DisplayObjectContainer;
import edu.virginia.engine.display.Sprite;
import edu.virginia.engine.display.Tween;
import edu.virginia.engine.display.TweenEvent;
import edu.virginia.engine.display.TweenJuggler;
import edu.virginia.engine.display.TweenParam;
import edu.virginia.engine.display.TweenTransition;
import edu.virginia.engine.display.VertWallSprite;
import edu.virginia.engine.display.WallSprite;
import edu.virginia.engine.display.coinSprite;
import edu.virginia.engine.display.enemySprite;
import edu.virginia.engine.display.ghostSprite;
import edu.virginia.engine.display.marioSprite;
import edu.virginia.engine.events.Event;
import edu.virginia.engine.util.GameClock;
import edu.virginia.lab1test.AStar;
import edu.virginia.lab1test.AStar.Cell;
import edu.virginia.lab1test.QuestManager;
import edu.virginia.lab1test.TweenableParam;

public class Alpha extends Game {

	coinSprite coin1 = new coinSprite("Coin One");
	WallSprite wall = new WallSprite("testWall");
	WallSprite wall2 = new WallSprite("testWall2");
	WallSprite wall3 = new WallSprite("testWall3");
	WallSprite wall4 = new WallSprite("testWall4");
	
	ghostSprite mario1 = new ghostSprite("ghost");
	Sprite questConfirm = new Sprite("Quest completed", "questComplete.png");
	VertWallSprite vwall = new VertWallSprite("vertWallOne");
	VertWallSprite vwall2 = new VertWallSprite("vertWallTwo");
	
	enemySprite enemy = new enemySprite("EnemyOne");
	
	Tween marioTween = new Tween(mario1, new TweenTransition() );
	Tween coinTween = new Tween(coin1, new TweenTransition() );
	
	TweenJuggler juggler = new TweenJuggler();
	
	private GameClock clock;

	Rectangle marioBounds = new Rectangle();
	Rectangle coinBounds = new Rectangle();
	Rectangle wallBounds = new Rectangle();
	Rectangle VertwallBounds = new Rectangle();

	QuestManager myQuestManager = new QuestManager();

	private int dx =7;
	private int dy =7;
	
	
	private boolean ghostAbilities = false;
	//for collision detection
	ArrayList<Sprite> collDects = new ArrayList<Sprite>(Arrays.asList(wall,wall2,vwall,vwall2,wall3,wall4));
	private boolean collisionOccured = false;
	private boolean stopR = false;
	private boolean stopL = false;
	private boolean stopU = false;
	private boolean stopD = false;
	private boolean zPress = false;
	
	
	public int room1x = 400;
	public int room1y = 375;
	public ArrayList<Cell> path;
	public ArrayList<Cell> fPath = new ArrayList<Cell>();
	public int enemyMoveCounter = 1;
	private boolean initializeRoute=true;
	public Rectangle room1 = new Rectangle();

	public Alpha() {
		
		super("Alpha Build", 1200, 800);
		
		clock = new GameClock();
		
		this.getScenePanel().setBackground(Color.gray);
		
		mario1.setTrans(0.0f);
		mario1.setXPos(3);
		mario1.setYPos(800-mario1.getUnscaledHeight()-25);
		marioTween.doTween(true);
		marioTween.animate(TweenableParam.FADE_IN, 0.0f, 1.0f, 6000);	
		
		coin1.setXPos(400);
		coin1.setYPos(400);
		coin1.addEventListener(myQuestManager, null);
		
		wall2.setXPos(300);
		wall2.setYPos(500-vwall.getScaledHeight()-wall.getScaledHeight());
		wall2.addEventListener(myQuestManager, null);
		
		wall4.setXPos(300+wall2.getScaledWidth());
		wall4.setYPos(500-vwall.getScaledHeight()-wall2.getScaledHeight());
		wall4.addEventListener(myQuestManager, null);
		
		vwall.setXPos(300);
		vwall.setYPos(500-vwall.getScaledHeight());
		vwall.addEventListener(myQuestManager, null);
		
		vwall2.setXPos(300+(2*wall.getScaledWidth())-vwall.getScaledWidth());
		vwall2.setYPos(500-vwall.getScaledHeight());
		vwall2.addEventListener(myQuestManager, null);
		
		
		coinTween.addEventListener(myQuestManager, null);
		coinTween.animate(TweenableParam.POP_TO_CENTER, coin1.getYPos(), this.getScenePanel().getHeight()/2-(coin1.getScaledHeight()/2)-50, 6000);
		coinTween.animate(TweenableParam.SWELL, coin1.getXScale(), .05, 6000);
		coinTween.animate(TweenableParam.FADE_OUT, 1.0f, 0.0f, 6000);
		
		juggler.add(marioTween);
		juggler.add(coinTween);
		
		questConfirm.setXScale(.5);
		questConfirm.setYScale(.5);
		questConfirm.setVisible(false);
		
		//enemy code
		enemy.setTrans(1.0f);
		enemy.setXPos(800);
		enemy.setYPos(100);
		
		//room rectangle
		room1.setBounds(300+vwall.getScaledHeight(),500-vwall.getScaledHeight(),(2*wall2.getScaledWidth()),vwall.getScaledHeight());
		
		//setting the list of blocked pixels the AI can't walk over, and figuring out a specific path
		
		ArrayList<int[]> blockedList = new ArrayList<int[]>(); //{{300,95},{300,96},{300,97},{300,98},{300,99},{300,100},{300,101},{300,102},{300,103},{300,104},{300,105}};
		
		//set the top wall of box to be blocked
		int wstart = 500-vwall.getScaledHeight()-wall.getScaledHeight();
		for(int c = wstart-enemy.getScaledHeight(); c<=wstart+wall2.getScaledHeight();c++){	
			for(int x = 300-enemy.getScaledWidth();x<=300+(wall2.getScaledWidth()*2);x++){
				int[] e = new int[]{x,c};
				blockedList.add(e);
			}
		}
		//set the left vertical wall of the box to be blocked
		for(int vy =500-vwall.getScaledHeight(); vy<=500; vy++){
			for(int vx=300-enemy.getScaledWidth(); vx<= 300+vwall.getScaledWidth(); vx++){
				int[] e = new int[]{vx,vy};
				blockedList.add(e);
			}
		}
		
		//set the Right vertical wall of the box to be blocked
				for(int vy2 =500-vwall.getScaledHeight(); vy2<=500; vy2++){
					for(int vx2=300-enemy.getScaledWidth()+(2*wall.getScaledWidth())-vwall.getScaledWidth(); vx2<= 300+(2*wall.getScaledWidth()); vx2++){
						int[] e = new int[]{vx2,vy2};
						blockedList.add(e);		
					}
				}
		
		path = AStar.test(1, this.getScenePanel().getWidth(), this.getScenePanel().getHeight(), (int)enemy.getXPos(), (int)enemy.getYPos(), room1x, room1y, blockedList);
		int pLen = path.size();
		int q=pLen-1;
		while(q>=0){
			Cell temp = new Cell(path.get(q).i,path.get(q).j);
			fPath.add(temp);
			q-=1;
		}
		room1.setBounds(300+vwall.getScaledWidth(),500-vwall.getScaledHeight(),(2*wall2.getScaledWidth())-(2*vwall.getScaledWidth()),vwall.getScaledHeight());
		initializeRoute = false;
	}

public void update(ArrayList<String> pressedKeys) {
		
		super.update(pressedKeys);
		
		if (mario1 != null) {
			if (coin1 != null) {
				
				juggler.nextFrame();
				
				if(initializeRoute==false && enemyMoveCounter<fPath.size()){
					Cell moveTo = fPath.get(enemyMoveCounter);

					int xm = moveTo.i;
					int ym = moveTo.j;

					enemy.setXPos(xm);
					enemy.setYPos(ym);
					enemyMoveCounter+=2;
				}
				
				for(Sprite wall : collDects){
					
					if(mario1.collidesWith(wall) && ghostAbilities==false){
						collisionOccured = true;
					double pcenterx = mario1.getHitBox().getCenterX();
					double pcentery = mario1.getHitBox().getCenterY();
					
					double wcenterx = wall.getHitBox().getCenterX();
					double wcentery = wall.getHitBox().getCenterY();
				    
					
					double marioT = mario1.getYPos();
					double marioB = (mario1.getYPos()+ mario1.getUnscaledHeight());
					double wallT = wall.getYPos();
					double wallB = (wall.getYPos()+ wall.getUnscaledHeight());
					
					double marioR = mario1.getXPos() + mario1.getUnscaledWidth();
					double marioL = mario1.getXPos();
					double wallR = wall.getXPos() + wall.getUnscaledWidth();
					double wallL = wall.getXPos();
	
					
					
					
						
					if(marioR - wallL >=0 && marioL<wcenterx){
							stopR = true;
							
						}
					if(wallR-marioL>=0 && marioL>wcenterx){
							stopL=true;
							
						}
					
					if(marioB - wallT>=0 && marioB<wallB){
						stopD = true;
						
						
					}
					if(wallB-marioT>=0 && marioT>wallT){
						stopU = true;
						
					}
	
					break;
					}
					
					
				}
				
				//enemy in the same room as player detection
				if(enemy.getHitBox().intersects(room1) && mario1.getHitBox().intersects(room1)&&ghostAbilities==false){
					System.out.println("ENEMY FOUND YOU! GAME OVER");
				}
				
				
				
				
				if(collisionOccured == false){
					stopL =false;
					stopU =false;
					stopR =false;
					stopD =false;	
				}
				
				if (collisionOccured == true){
					collisionOccured = false;
				}
				
				if (ghostAbilities==true){
					stopL =false;
					stopU =false;
					stopR =false;
					stopD =false;
				}
				
				
				if (myQuestManager.questCompleted) {
					coin1.setXPos(this.getScenePanel().getWidth()/2-coin1.getScaledWidth()/2);
					coinTween.doTween(myQuestManager.tweenComplete);
					questConfirm.setVisible(true);		
				}

				if (pressedKeys.contains(KeyEvent.getKeyText(38)) ) {
					if ( !(mario1.getYPos() - dy < -20) && stopU==false)
						mario1.setYPos(mario1.getYPos()-dy);
				}

				if (pressedKeys.contains(KeyEvent.getKeyText(40))) {
					if ( !(mario1.getYPos() + dy > this.getScenePanel().getHeight() - 20) && stopD==false)
						mario1.setYPos(mario1.getYPos() + dy);
				}

				if (pressedKeys.contains(KeyEvent.getKeyText(39))) {
					if ( !(mario1.getXPos() + dx > this.getScenePanel().getWidth() - 60) && stopR==false)
						mario1.setXPos(mario1.getXPos() + dx);
				}

				if (pressedKeys.contains(KeyEvent.getKeyText(37))) {
					if ( !(mario1.getXPos() - dx < -20) && stopL==false)
						mario1.setXPos(mario1.getXPos() - dx);
				}
				if (pressedKeys.contains(KeyEvent.getKeyText(71))) {
					if(zPress == false){
						zPress = true;
					
					if(ghostAbilities==false ){
							float newAlpha = (float) (0.2);
							mario1.setTrans(newAlpha);
					}
					
					if (ghostAbilities==true ) {
							float newAlpha = (float) (0.9f);
							mario1.setTrans(newAlpha);
					
					}
					
					ghostAbilities = !ghostAbilities;
					
					
					}
				}
				
				if (pressedKeys.contains(KeyEvent.getKeyText(71))==false){
					zPress = false;
				}	
				

				if (mario1.collidesWith(coin1)&& ghostAbilities==false) {
					coin1.dispatchEvent(new Event(Event.COIN_PICKED_UP, coin1));
					coinTween.dispatchEvent(new TweenEvent(TweenEvent.TWEEN_EVENT_COMPLETE, coinTween));
				}
				

			}
		
		
		}
	}

	/**
	 * Engine automatically invokes draw() every frame as well. If we want to make sure mario1 gets drawn to
	 * the screen, we need to make sure to override this method and call mario1's draw method.
	 * */
	@Override
	public void draw(Graphics g){
		
		super.draw(g);
		
		g.drawRect (100, 500, 1000, 200);//entryway
		g.drawRect (100, 200, 500, 300); //leftroom
		g.drawRect (600, 200, 500, 300);
		if (coin1 != null) {
			coin1.draw(g);
			wall.draw(g);
			wall2.draw(g);
			wall3.draw(g);
			wall4.draw(g);
			vwall.draw(g);
			vwall2.draw(g);
		}
		
		if (mario1 != null) {
			mario1.draw(g);
			enemy.draw(g);
		}
		if (wall != null) {
		}
		
		if (questConfirm != null) {
			questConfirm.draw(g);
		}
	}

	public static void main(String[] args) {

		Alpha game = new Alpha();
		game.start();
	}
}


