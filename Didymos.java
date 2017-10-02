package didymos;

import robocode.*;
import robocode.util.Utils;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayDeque;

/**
 * A twin robot optimized for fighting a 2v1 battle.
 */
public class Didymos extends TeamRobot {

    /**
     * The status of a specific robot in a specific turn.
     */
    public static abstract class Status extends Point2D.Double implements Serializable {
        private final double m_energy;
        private final long m_turn;

        /**
         * Creates a status from the current state of a robot.
         * @param robot The robot delivering the current parameters.
         */
        public Status(Robot robot) {
            super(robot.getX(), robot.getY());
            this.m_energy = robot.getEnergy();
            this.m_turn = robot.getTime();
        }

        /**
         * Creates a status from a ScannedRobotEvent.
         * @param robot The robot which recieved the event.
         * @param enemy The actual event.
         */
        public Status(Robot robot, ScannedRobotEvent enemy) {
            super(robot.getX()
                    + Math.sin(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance(),
                    robot.getY() + Math.cos(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360))
                            * enemy.getDistance());

            this.m_energy = enemy.getEnergy();
            this.m_turn = robot.getTime();
        }

        /**
         * Returns the energy of the robot at this point in time.
         * @return the energy of the robot at this point in time.
         */
        public double getEnergy() {
            return m_energy;
        }

        /**
         * Returns the turn the status was genereted.
         * @return the turn the status was geenrated.
         */
        public long getTurn() {
            return m_turn;
        }
    }

    /**
     * The status of the enemy in a specific turn.
     */
    public static class Enemy extends Status {
        /**
         * Creates a status from a ScannedRobotEvent.
         * @param robot The robot which recieved the event.
         * @param enemy The actual event.
         */
        public Enemy(final Robot robot, final ScannedRobotEvent enemy) {
            super(robot, enemy);
        }
    }

    /**
     * The status of one of the twins in a specific turn.
     */
    public static class TeamMember extends Status {
        private final Point2D.Double m_goal;

        /**
         * Creates the status from the current state of a twin.
         * @param robot The robot delivering the current parameters.
         * @param goal The goal of the robot.
         */
        public TeamMember(final Robot robot, final Point2D.Double goal) {
            super(robot);
            this.m_goal = goal;
        }

        /**
        * Creates a status from a ScannedRobotEvent.
        * @param robot The robot which recieved the event.
        * @param enemy The actual event.
        * @param goal The known goal of the robot or 'null' to set a default.
        */
        public TeamMember(final Robot robot, final ScannedRobotEvent event, Point2D.Double goal) {
            super(robot, event);
            if (goal == null) {
                goal = this;
            }
            this.m_goal = goal;
        }

        /**
         * Returns the goal of the teammember.
         * @return the goal.
         */
        public Point2D.Double getGoal() {
            return this.m_goal;
        }
    }

    /**
     * A message which is transmitted between the twins.
     */
    public static class Message implements Serializable {
        private final Status m_status;
        private final boolean m_isEnemy;

        /**
         * Creates a new message.
         * @param status The status which should be sended.
         */
        public Message(Status status) {
            this.m_status = status;
            this.m_isEnemy = (status instanceof Enemy);
        }

        /**
         * Checks, if the message contains data of the enemy.
         * @return true, if the message contains data of the enemy.
         */
        public boolean isEnemyData() {
            return this.m_isEnemy;
        }

        /**
         * Returns the underlying status.
         * @return the underlying status.
         */
        public Status getStatus() {
            return this.m_status;
        }

        /**
         * Sends a message to the other twin.
         * @param robot The sender of the event.
         * @param status The status which should be transmitted.
         */
        public static void send(TeamRobot sender, Status status) {
            try {
                sender.broadcastMessage(new Message(status));
            } catch (Exception ex) {
                sender.out.println("[ERROR] Error during result transmission");
            }
        }
    }

    /**
     * The power which is used to fire bullets.
     */
    public static final double FIRE_POWER = 3.0;

    private TeamMember m_friend;
    private Enemy m_enemy;

    @Override
    public void run() {
        this.setAdjustRadarForRobotTurn(true);
        this.setAdjustGunForRobotTurn(true);

        while (true) {
            this.turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Save status
        if (this.isTeammate(e.getName())) {
            this.m_friend = new TeamMember(this, e, (this.m_friend != null ? this.m_friend.getGoal() : null));
            return;
        } else {
            this.m_enemy = new Enemy(this, e);
        }

        // Send the current status to the other robot.
        Message.send(this, this.m_enemy);

        // Handle the different robot parts in parallel.
        this.handleRadar(e);
        this.handleGun(e);
        this.handleMovement(e);

        this.execute();
    }

    @Override
    public void onSkippedTurn(SkippedTurnEvent event) {
        this.out.println("[ERROR] Skipped turn!");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessageReceived(MessageEvent event) {
        Message message = (Message) event.getMessage();
        if (message.isEnemyData()) {
            this.m_enemy = (Enemy) message.getStatus();
        } else {
            this.m_friend = (TeamMember) message.getStatus();
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        if (this.m_enemy != null) {
            g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
            g.fillRect((int) this.m_enemy.getX() - 20, (int) this.m_enemy.getY() - 20, 40, 40);
        }

        if (this.m_friend != null) {
            g.setColor(new Color(0x00, 0xff, 0x26, 0x80));
            g.fillRect((int) this.m_friend.getX() - 20, (int) this.m_friend.getY() - 20, 40, 40);

            g.setColor(new Color(0xff, 0x00, 0x00));
            g.fillOval((int) this.m_friend.getGoal().getX() - 4, (int) this.m_friend.getGoal().getY(), 8, 8);
        }
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        if (!this.isTeammate(event.getName()) || this.isAssistant()) {
            if (event.getBearing() > -90 && event.getBearing() <= 90) {
                this.back(50);
            } else {
                this.ahead(50);
            }
        }
    }

    /**
     * Controls the radar after an enemy is scanned.
     * @param e The ScannedRobotEvent event.
     */
    private void handleRadar(ScannedRobotEvent e) {
        // Focus on the enemy
        final double radarTurn = this.getHeadingRadians() + e.getBearingRadians() - this.getRadarHeadingRadians();
        this.setTurnRadarRightRadians(1.95 * Utils.normalRelativeAngle(radarTurn));
    }

    /**
     * Controls the gun after an enemy was scanned.
     * @param e The ScannedRobotEvent event.
     */
    private void handleGun(ScannedRobotEvent e) {
        // Predict future position and aim. Inspired by http://robowiki.net/wiki/Linear_Targeting
        final double headOnBearing = getHeadingRadians() + e.getBearingRadians();
        final double linearBearing = headOnBearing + Math.asin(
                e.getVelocity() / Rules.getBulletSpeed(FIRE_POWER) * Math.sin(e.getHeadingRadians() - headOnBearing));
        this.setTurnGunRightRadians(Utils.normalRelativeAngle(linearBearing - this.getGunHeadingRadians()));

        // Fire, if suitable.
        if (e.getDistance() < 150 && this.getGunHeat() == 0) {
            this.setFire(FIRE_POWER);
        }
    }

    /**
     * Controls the movement after an enemy was scanned.
     * @param e The ScannedRobotEvent event.
     */
    private void handleMovement(ScannedRobotEvent e) {
        // Try to avoid collision beforehand by stopping
        if (this.isAssistant() && this.m_friend != null
                && this.m_friend.distance(this.getX(), this.getY()) <= this.getWidth() * 2.5) {
            this.stop();
            Message.send(this, new TeamMember(this, new Point2D.Double(this.getX(), this.getY())));
            return;
        }

        // Get the next waypoint.
        final Point2D.Double nextGoal = this.getNextTarget();
        Message.send(this, new TeamMember(this, nextGoal));

        // Move to the next waypoint.
        double angle;
        this.setTurnRightRadians(Math.tan(
                angle = Math.atan2(nextGoal.x -= this.getX(), nextGoal.y -= this.getY()) - this.getHeadingRadians()));
        this.setAhead(Math.hypot(nextGoal.getX(), nextGoal.getY()) * Math.cos(angle));
    }

    /**
     * Generates the next waypoint for the robot.
     * @return the position of the next waypoint.
     */
    private Point2D.Double getNextTarget() {
        // If the robot is not the leader ...
        if (this.isAssistant()) {
            // Generate the two right angle position on both side of the enemy...
            final double xFactor = Math.tan(45) * -(this.m_friend.getGoal().getY() - this.m_enemy.getY()),
                    yFactor = Math.tan(45) * (this.m_friend.getGoal().getX() - this.m_enemy.getX());

            final Point2D.Double p1 = this.getPointInBattlefield(
                    new Point2D.Double(this.m_enemy.getX() + xFactor, this.m_enemy.getY() + yFactor));
            final Point2D.Double p2 = this.getPointInBattlefield(
                    new Point2D.Double(this.m_enemy.getX() - xFactor, this.m_enemy.getY() - yFactor));

            // ... and choose the one which is nearby.
            return (p1.distance(this.getX(), this.getY()) <= p2.distance(this.getX(), this.getY())) ? p1 : p2;
        } else {
            // Move to the enemy while having a secure distance.
            final double enemyDistance = this.m_enemy.distance(this.getX(), this.getY());
            final double safeDistanceFraction = (enemyDistance - this.getWidth() * 2) / enemyDistance;

            return new Point2D.Double(
                    (1 - safeDistanceFraction) * this.getX() + safeDistanceFraction * this.m_enemy.getX(),
                    (1 - safeDistanceFraction) * this.getY() + safeDistanceFraction * this.m_enemy.getY());
        }
    }

    /**
     * Normalized a point that it is safely reachable.
     * @param target The point which is to be normalized.
     * @return the normalized point.
     */
    private Point2D.Double getPointInBattlefield(Point2D.Double target) {
        final double x = Math.min(Math.max(this.getWidth() * 2, target.getX()),
                this.getBattleFieldWidth() - this.getWidth() * 2);
        final double y = Math.min(Math.max(this.getHeight() * 2, target.getY()),
                this.getBattleFieldHeight() - this.getHeight() * 2);
        return new Point2D.Double(x, y);
    }

    /**
     * Returns the current role of the robot.
     * @return true, if the robot is not the leader.
     */
    private boolean isAssistant() {
        return (this.m_friend != null && this.m_friend.getGoal() != null && this.getEnergy() < this.m_friend.getEnergy()
                && this.getTime() - this.m_friend.getTurn() < 10);
    }
}
