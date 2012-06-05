// ScalaDemo.scala
package tar
 
import robocode._
import robocode.util.Utils
 
/*
  A small demo to test robot coding in Scala.
*/
class ScalaDemo extends AdvancedRobot {
 
  // Main thread of robot
  override def run {
    setAdjustGunForRobotTurn(true)
    while (true) {
      // If we're not going anywhere, move randomly around battlefield
      if (Math.abs(getDistanceRemaining) < Rules.MAX_VELOCITY
          && Math.abs(getTurnRemaining) < Rules.MAX_TURN_RATE)
      {
        setAhead((Math.random*2-1)*100)
        setTurnRight(Math.random*90-45)
      }
      // Tell RADAR to spin
      if (getRadarTurnRemaining == 0)
        setTurnRadarRightRadians(4)
      // Carry out pending actions
      execute
    }
  }
 
  // Picked up a robot on RADAR
  override def onScannedRobot(e : ScannedRobotEvent) {
    // Absolute bearing to detected robot
    val absBearing = e.getBearingRadians + getHeadingRadians
 
    // Tell RADAR to paint detected robot
    setTurnRadarRightRadians(3*Utils.normalRelativeAngle(
      absBearing - getRadarHeadingRadians ))
 
    // Tell gun to point at detected robot
    setTurnGunRightRadians(Utils.normalRelativeAngle(
      absBearing - getGunHeadingRadians ))
 
    // Tell robot to shoot with power 2
    setFire(2)
  }
 
  // Robot ran into a wall
  override def onHitWall(e : HitWallEvent) {
    // Turn in opposite direction to wall
    setTurnRightRadians(Utils.normalRelativeAngle(
      e.getBearingRadians + Math.Pi))
  }
}
