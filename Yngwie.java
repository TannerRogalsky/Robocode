package tar;   
   
import robocode.*;   
import java.awt.Color;   
import java.util.Vector;   
   
/**  
 *  
 *    This robot Yngwie was designed for Robocode andcreated by Enno Peters  
 *    If you re-use something for your own bot, please give the author  
 *    proper credits in your code.  
 *  
 *    For Questions or donations contact me at e.peters@ai.rug.nl  
 *  
 *  
 */   
   
public class Yngwie extends AdvancedRobot// implements Consts   
{   
   private int StatBulletsHit;   
   private int StatBulletsMissed;   
   private int StatSkippedTurns;   
   private int StatTimeErrors;   
   private static int StatTotalTime;   
   private static int StatTotalTimeErrors;   
   private static int StatTotalSkippedTurns;   
   private static int StatTotalBulletsHit;   
   private static int StatTotalBulletsMissed;   
   private static long StatTotalWallCollisions;   
   private static long StatTotalEnemyCollisions;   
   private static long StatTotalExceptions;   
   
   public static double BattleFieldHeight;   
   public static double BattleFieldWidth;   
   public double X;   
   public double Y;   
   public double LastX;   
   public double LastY;   
   private double LastEnemyX;   
   private double LastEnemyY;   
   
   public static Yngwie instance;   
   
   public static boolean OneOnOne;   
   
   public long LastTime;   
   
   public Control control;   
   public Motor motor;   
   public Gunner gunner;   
   public Scanner scanner;   
   public static EnemyCollection EC;   
   public Predictor predictor;   
   public Vector bullettrackers;   
   public Vector ScanEvents;   
   
   public static long PeaceTime;   
   public static boolean Melee;   
   public boolean won;   
   
   
   public void run()   
   {   
      setColors(Color.green,Color.green,Color.cyan);   
      if (EC == null) // eerste ronde   
      {   
         instance = this;   
         EC = new EnemyCollection(this);   
         StatTotalTime              = 0;   
         StatTotalTimeErrors        = 0;   
         StatTotalSkippedTurns      = 0;   
         StatTotalWallCollisions    = 0;   
         StatTotalBulletsHit        = 0;   
         StatTotalBulletsMissed     = 0;   
         StatTotalEnemyCollisions   = 0;   
         StatTotalExceptions        = 0;   
         BattleFieldHeight = getBattleFieldHeight();   
         BattleFieldWidth = getBattleFieldWidth();   
         PeaceTime = GetCoolingDownTurns();   
         Melee = (getOthers() != 1);   
      }   
      else   
         EC.ResetDeaths();   
   
      ScanEvents = new Vector(getOthers());   
      bullettrackers = new Vector(10,5);   
      control = new Control(this);   
      motor = new Motor(this);   
      gunner = new Gunner(this);   
      scanner = new Scanner(this);   
      predictor = new Predictor(this);   
      out.println();   
   
      while (true)   
      {   
         if (getTime() == 0)   
            getInitialValues();   
         else   
         {   
            if (LastTime != getTime()-1)   
            {   
               StatTimeErrors++;   
               StatTotalTimeErrors++;   
            }   
            LastTime = getTime();   
         }   
   
         X = getX(); // to reduce calls to get-functions   
         Y = getY(); // to reduce calls to get-functions   
         try{   
         OneOnOne = (getOthers() == 1);   
         HandleScanEvents();   
         HandleBulletTrackers();   
         motor.Update();   
         gunner.Update();   
         scanner.Update();   
         control.Update();   
         }   
         catch (Exception ex) {   
            StatTotalExceptions++;   
            out.println(getTime()+" catched it! : "+ex.getMessage());   
         }   
   
         if ((won) && (motor.Threats.size() == 0))   
            VictoryDance();   
         LastX = X;   
         LastY = Y;   
         execute();   
      }   
   }   
   
   public void HandleScanEvents(){   
      for(int i=0; i<ScanEvents.size();i++)   
         EC.StoreScanEvent((ScannedRobotEvent) ScanEvents.elementAt(i));   
      ScanEvents.clear();   
   }   
   
   public void HandleBulletTrackers(){   
      if (bullettrackers.size() == 0)   
         return;   
      BulletTracker bt;   
      Enemy en = null;   
      int idx = 0;   
      int i = 0;   
      while (i < bullettrackers.size())   
      {   
         bt = (BulletTracker) bullettrackers.elementAt(i);   
         if (bt.completed && bt.bulletDead)   
         {   
            bullettrackers.removeElementAt(i);   
            continue;   
         }   
   
         bt.Counter -= 1;   
         if (bt.Counter > 0)   
         {   
            i++;   
            continue;   
         }   
   
         idx = EC.IndexOf(bt.enemy);   
         if (idx == -1)   
            bullettrackers.removeElementAt(i);   
         else   
         {   
            en = (Enemy) EC.Enemies.elementAt(idx);   
   
            if (!bt.completed)   
               bt.completed = en.AddBulletItem(bt,getTime());   
            i++;   
         }   
      }   
   }   
   
   public double getTargetField()   
   {   
      if (OneOnOne)   
         return 50.0;   
      else   
         return 150.0;   
   }   
   
   
   public void onHitRobot(HitRobotEvent event) {   
     motor.Collide = true;   
     StatTotalEnemyCollisions++;   
     scanner.CheckHeading(event.getBearing());   
   }   
   
   public void onHitByBullet(HitByBulletEvent e) {   
      scanner.CheckHeading(My.AddDegrees(getHeading(),e.getBearing()));   
      int Enemyidx = EC.IndexOf(e.getName());   
      if (Enemyidx != -1){   
         Enemy en = (Enemy) EC.Enemies.elementAt(Enemyidx);   
         en.EnergyAdjust += My.getBulletGain(e.getPower());   
         en.LastTimeHitMe = getTime();   
         en.BulletDamage += My.getBulletDamage(e.getPower());   
      }   
   }   
   
   public void onHitWall(HitWallEvent event) {   
      StatTotalWallCollisions++;   
   }   
   
   public void onBulletHitBullet(BulletHitBulletEvent event) {   
      Bullet bullet = event.getBullet();   
      BulletTracker bt;   
      Enemy en;   
      Strategy strat;   
      for (int i = 0; i < bullettrackers.size(); i++){   
         bt = (BulletTracker) bullettrackers.elementAt(i);   
         if (bt.bullet == bullet){   
            int idx = EC.IndexOf(bt.enemy);   
            if (idx > -1) {   
               en = (Enemy) EC.Enemies.elementAt(idx);   
               for (int j = 0; j < en.Strategies.size(); j++){   
                  strat = (Strategy) en.Strategies.elementAt(j);   
                  if (strat.ID == bt.Strategy){   
                     strat.Failed(bullet.getPower());   
                     break;   
                  }   
               }   
            }   
            bt.bulletDead = true;   
            break;   
         }   
      }   
   }   
   
   public void onBulletHit(BulletHitEvent event) {   
      StatBulletsHit++;   
      StatTotalBulletsHit++;   
      Bullet bullet = event.getBullet();   
      BulletTracker bt;   
      Enemy en;   
      Strategy strat;   
      for (int i = 0; i < bullettrackers.size(); i++){   
         bt = (BulletTracker) bullettrackers.elementAt(i);   
         if (bt.bullet == bullet){   
            int idx = EC.IndexOf(bt.enemy);   
            if (idx > -1) {   
               en = (Enemy) EC.Enemies.elementAt(idx);   
               en.EnergyAdjust -= My.getBulletDamage(event.getBullet().getPower());   
               if (en.Name == bullet.getVictim()){   
                  for (int j = 0; j < en.Strategies.size(); j++){   
                     strat = (Strategy) en.Strategies.elementAt(j);   
                     if (strat.ID == bt.Strategy){   
                        strat.Success(bullet.getPower());   
                        break;   
                     }   
                  }   
               }   
            }   
            bt.bulletDead = true;   
            break;   
         }   
      }   
   }   
   
   public void onBulletMissed(BulletMissedEvent event) {   
      StatBulletsMissed++;   
      StatTotalBulletsMissed++;   
      Bullet bullet = event.getBullet();   
      BulletTracker bt;   
      Enemy en;   
      Strategy strat;   
      for (int i = 0; i < bullettrackers.size(); i++){   
         bt = (BulletTracker) bullettrackers.elementAt(i);   
         if (bt.bullet == bullet){   
            int idx = EC.IndexOf(bt.enemy);   
            if (idx > -1) {   
               en = (Enemy) EC.Enemies.elementAt(idx);   
               for (int j = 0; j < en.Strategies.size(); j++){   
                  strat = (Strategy) en.Strategies.elementAt(j);   
                  if (strat.ID == bt.Strategy){   
                     strat.Failed(bullet.getPower());   
                     break;   
                  }   
               }   
            }   
            bt.bulletDead = true;   
            break;   
         }   
      }   
   }   
   
   public double getBulletDamageAimedAt(Enemy en){   
      BulletTracker bt;   
      double result = 0.0;   
      double mydist = Distance(en)+30.0;   
      double bulletdistance;   
      double pow;   
      double damage;   
      for (int i = 0; i < bullettrackers.size(); i++){   
         bt = (BulletTracker) bullettrackers.elementAt(i);   
         if (bt.bulletDead)   
            continue;   
         bulletdistance = My.Distance(bt.bullet.getX(),bt.bullet.getX(),en.X(),en.Y());   
         if ((bt.enemy == en.Name) && (bulletdistance < mydist)){   
            result += My.getBulletDamage(bt.bullet.getPower());   
         }   
      }   
      return result;   
   }   
   
   public void onRobotDeath(RobotDeathEvent e) {   
      int idx = EC.IndexOf(e.getName());   
      if ((won) && (idx != -1) && (((Enemy) EC.Enemies.elementAt(idx)).Time() == getTime()-1))   
      {   
         Enemy en = (Enemy) EC.Enemies.elementAt(idx);   
         LastEnemyX = en.X();   
         LastEnemyY = en.Y();   
      }   
      if ((idx != -1) && (((Enemy) EC.Enemies.elementAt(idx)) == gunner.Target))   
          scanner.ScanRound();   
      EC.EnemyDeath(e.getName());   
   }   
   
   public void onScannedRobot(ScannedRobotEvent e) {   
      ScanEvents.add(e); //verwerk deze later   
   }   
   
   public void onSkippedTurn(SkippedTurnEvent event) {   
      StatSkippedTurns++;   
      StatTotalSkippedTurns++;   
//      out.println(getTime()+" skipped turn");   
   }   
   
   public void onDeath(DeathEvent event) {   
      EndRound();   
   }   
   
   public void onWin(WinEvent event) {   
      won = true;   
      EndRound();   
   }   
   
   private void VictoryDance(){   
      while (Math.abs(getVelocity()) > 0) {   
         setAhead(0.0);   
         execute();   
      }   
      double TargetX;   
      double TargetY;   
      if (LastEnemyX > -1)   
      {   
         TargetX = LastEnemyX;   
         TargetY = LastEnemyY;   
      }   
      else   
      {   
         TargetX = BattleFieldWidth / 2.0;   
         TargetY = BattleFieldHeight / 2.0;   
      }   
   
      double angle = My.AngleFromTo(getX(),getY(),TargetX,TargetY);   
      double antiangle = (angle + 180.0) % 360.0;   
      while ((Math.abs(getHeading() - antiangle) > 0.01) ||   
             (Math.abs(getGunHeading() - antiangle) > 0.01) ||   
             (Math.abs(getRadarHeading() - antiangle) > 0.01))   
      {   
         control.TurnTo(cRobot,3,antiangle,false);   
         control.TurnTo(cTurret,3,antiangle,false);   
         control.TurnTo(cRadar,3,antiangle,false);   
         control.Update();   
         execute();   
      }   
      execute();   
      int i = 1;   
      while (Math.abs(getHeading() - angle) > 0.01)   
      {   
         control.TurnTo(cRobot,3,(antiangle+i*10.0)%360.0,false);   
         control.TurnTo(cTurret,3,(antiangle+i*30.0)%360.0,false);   
         control.TurnTo(cRadar,3,(antiangle+i*71.25)%360.0,false);   
         control.Update();   
         execute();   
         i++;   
      }   
      double dist = My.Distance(getX(),getY(),TargetX,TargetY);   
      while(dist > 2.0)   
      {   
         if (My.isToRightDeg(getHeading(),getRadarHeading()))   
             setTurnRadarLeft(45.0);   
         else   
             setTurnRadarRight(45.0);   
         setAhead(dist);   
         execute();   
         dist = My.Distance(getX(),getY(),TargetX,TargetY);   
      }   
   
      while ((getHeading() > 0.01) ||   
             (getGunHeading() > 0.01) ||   
             (getRadarHeading() > 0.01))   
      {   
         control.TurnTo(cRobot,3,0.0,false);   
         control.TurnTo(cTurret,3,0.0,false);   
         control.TurnTo(cRadar,3,0.0,false);   
         control.Update();   
         execute();   
      }   
      turnLeft(20.0);   
      for (i = 0; i < 30; i++)   
      {   
        turnRight(40.0);   
        turnLeft(40.0);   
      }   
   }   
   
   private void EndRound(){   
      StatTotalTime += getTime();   
//      PrintStats();   
   
/*      if (getRoundNum() == getNumRounds()-1){  
         PrintStats();  
         EC.printStrategies();  
      }*/   
   }   
   
   private void PrintStats(){   
      out.println();   
      if (Melee)   
         out.println("-- Yngwie Melee battle statistics --");   
      else   
         out.println("-- Yngwie 1-v-1 battle statistics --");   
      out.println("Bullets hit           : "+StatBulletsHit);   
      out.println("Bullets hit ratio %   : "+((float) 100.0 * StatBulletsHit / Math.max(1,StatBulletsHit + StatBulletsMissed)));   
      out.println("Current time          : "+getTime());   
      out.println("Skipped turns         : "+StatSkippedTurns);   
      out.println("Time errors           : "+StatTimeErrors);   
      out.println("Total exceptions      : "+StatTotalExceptions);   
      out.println("Total time            : "+StatTotalTime);   
      out.println("Total skipped turns   : "+StatTotalSkippedTurns);   
      out.println("Total time errors     : "+StatTotalTimeErrors);   
      out.println("Total Wall collisions : "+StatTotalWallCollisions);   
      out.println("Total Enemy collisions: "+StatTotalEnemyCollisions);   
      out.println("Total Bullets hit     : "+StatTotalBulletsHit);   
      out.println("Total Bullets hit %   : "+((float) 100.0 * StatTotalBulletsHit / Math.max(1,StatTotalBulletsHit + StatTotalBulletsMissed)));   
   }   
   
   protected void getInitialValues(){   
      StatBulletsHit = 0;   
      StatBulletsMissed = 0;   
      StatSkippedTurns = 0;   
      StatTimeErrors = 0;   
      OneOnOne = false;   
      LastTime = 0;   
      LastEnemyX = -1.0;   
      LastEnemyY = -1.0;   
      LastX = getX();   
      LastY = getY();   
      won = false;   
   }   
   
   protected void printReport(){   
      out.println(getTime()+" REPORT");   
      out.println(getTime()+" BattleFieldHeight : "+BattleFieldHeight);   
      out.println(getTime()+" BattleFieldWidth : "+BattleFieldWidth);   
      out.println(getTime()+" Others : "+getOthers());   
      out.println(getTime()+" GunCoolingRate : "+getGunCoolingRate());   
      out.println(getTime()+" X : "+getX());   
      out.println(getTime()+" Y : "+getY());   
      out.println(getTime()+" Heading : "+getHeading());   
      out.println(getTime()+" Velocity : "+getVelocity());   
      out.println(getTime()+" Energy : "+getEnergy());   
      out.println(getTime()+" RadarHeading : "+getRadarHeading());   
      out.println(getTime()+" GunHeading : "+getGunHeading());   
      out.println(getTime()+" GunHeat : "+getGunHeat());   
      out.println();   
   }   
   
   public double Distance(Enemy en){   
      return My.Distance(getX(),getY(),en.X(),en.Y());   
   }   
   
   public double AngleTo(Enemy en){   
      return My.AngleFromTo(getX(),getY(),en.X(),en.Y());   
   }   
   
   public double TurnsCoolingDown(double firePower){   
      return (1.0 + (firePower / 5)) / getGunCoolingRate();   
   }   
   
   public long GetCoolingDownTurns(){   
      return (long) Math.ceil(getGunHeat() / getGunCoolingRate());   
   }   
   
   // geeft de angle naar een enemy die de volgende beurt waarschijnlijk is   
   // (lineaire extrapolatie van de enemy positie)   
   public double NextTurnAngleTo(Enemy en){   
      if ((en == null) || (en.Death) || (en.RC == 0) || (getTime() - en.Time() > 9))   
         return -1.0;   
      else   
      {   
         double tijdsverschil = getTime() - en.Time() + 1.0;   
         double vX = en.X()+en.Velocity()*tijdsverschil*My.sinDeg(en.Heading());   
         double vY = en.Y()+en.Velocity()*tijdsverschil*My.cosDeg(en.Heading());   
         return My.AngleFromTo(control.NextXPos(),control.NextYPos(),vX,vY);   
      }   
   }   
} 
