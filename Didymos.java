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
    public static class Status extends Point2D.Double implements Serializable {
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
     * A history of the status of a robot. 
     */
    public static class History {
        private ArrayDeque<Status> m_payload;

        /**
         * Creates a new history.
         */
        public History() {
            m_payload = new ArrayDeque<>();
        }

        /**
         * Adds a status, if possible.
         * @param status The status which is to be added.
         * @return true if the insertion was sucessfull.
         */
        public boolean addStatus(Status status) {
            // Filter out already known status
            if (this.m_payload.size() > 0 && this.getLastStatus().getTurn() >= status.getTurn()) {
                return false;
            }

            // Discard old entries
            if (this.m_payload.size() > 3) {
                this.m_payload.pollLast();
            }

            this.m_payload.push(status);
            return true;
        }

        /**
         * Returns the most current status.
         * @return the most current status or NULL else.
         */
        public Status getLastStatus() {
            return this.m_payload.peekFirst();
        }
    }

    /**
     * A report which may have different types and is to be sended between the robots.
     */
    public static class Report<E extends Serializable> implements Serializable {
        /**
         * The type of the report.
         */
        public enum ReportType {
            CurrentPosition, GoalPosition, Enemy;
        }

        private final E m_payload;
        private final ReportType m_type;

        /**
         * Creates a new report.
         * @param type The type of report.
         * @param status The actual payload.
         */
        public Report(ReportType type, E payload) {
            this.m_payload = payload;
            this.m_type = type;
        }

        /**
         * Return the payload.
         * @return the payload.
         */
        public E getPayload() {
            return m_payload;
        }

        /**
         * Returns the type of the report.
         * @return the type of report.
         */
        public ReportType GetType() {
            return m_type;
        }
    }

    /**
     * The power which is used to fire bullets.
     */
    public static final double FIRE_POWER = 3.0;

    private History m_friend, m_enemy;
    private Point2D.Double m_goal;

    @Override
    public void run() {
        this.setAdjustRadarForRobotTurn(true);
        this.setAdjustGunForRobotTurn(true);

        this.m_friend = new History();
        this.m_enemy = new History();

        while (true) {
            this.turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Save status
        if (this.isTeammate(e.getName())) {
            this.m_friend.addStatus(new Status(this, e));
            return;
        } else {
            this.m_enemy.addStatus(new Status(this, e));
        }

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
        switch (((Report) event.getMessage()).GetType()) {
        case CurrentPosition:
            this.m_friend.addStatus(((Report<Status>) event.getMessage()).getPayload());
            break;
        case GoalPosition:
            this.m_goal = ((Report<Point2D.Double>) event.getMessage()).getPayload();
            break;
        case Enemy:
            this.m_enemy.addStatus(((Report<Status>) event.getMessage()).getPayload());
            break;
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        if (this.m_enemy.getLastStatus() != null) {
            g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
            g.fillRect((int) this.m_enemy.getLastStatus().getX() - 20, (int) this.m_enemy.getLastStatus().getY() - 20,
                    40, 40);
        }

        if (this.m_friend.getLastStatus() != null) {
            g.setColor(new Color(0x00, 0xff, 0x26, 0x80));
            g.fillRect((int) this.m_friend.getLastStatus().getX() - 20, (int) this.m_friend.getLastStatus().getY() - 20,
                    40, 40);
        }

        if (this.m_goal != null) {
            g.setColor(new Color(0xff, 0x00, 0x00));
            g.fillOval((int) this.m_goal.getX() - 4, (int) this.m_goal.getY(), 8, 8);
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
            setFire(FIRE_POWER);
        }
    }

    /**
     * Controls the movement after an enemy was scanned.
     * @param e The ScannedRobotEvent event.
     */
    private void handleMovement(ScannedRobotEvent e) {
        // Get the next waypoint.
        final Point2D.Double nextGoal = this.getNextTarget();

        try {
            // Send the current status to the other robot.
            this.broadcastMessage(new Report<>(Report.ReportType.CurrentPosition, new Status(this)));
            this.broadcastMessage(new Report<>(Report.ReportType.Enemy, this.m_enemy.getLastStatus()));

            // Try to avoid collision beforehand by stopping
            final Status friend = this.m_friend.getLastStatus();
            if (this.isAssistant() && friend != null && friend.distance(getX(), getY()) <= getWidth() * 2.5) {
                this.stop();
                return;
            } else {
                this.broadcastMessage(new Report<>(Report.ReportType.GoalPosition, nextGoal));
            }
        } catch (Exception ex) {
            this.out.println("[ERROR] Error during result transmission");
        }

        // Move to the nex waypoint.
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
        final Status enemy = this.m_enemy.getLastStatus();

        // If the robot is not the leader ...
        if (this.isAssistant()) {
            // Generate the two right angle position on both side of the enemy...
            final double xFactor = Math.tan(45) * -(this.m_goal.getY() - enemy.getY()),
                    yFactor = Math.tan(45) * (this.m_goal.getX() - enemy.getX());

            final Point2D.Double p1 = this
                    .getPointInBattlefield(new Point2D.Double(enemy.getX() + xFactor, enemy.getY() + yFactor));
            final Point2D.Double p2 = this
                    .getPointInBattlefield(new Point2D.Double(enemy.getX() - xFactor, enemy.getY() - yFactor));

            // ... and choose the one which is nearby.
            return (p1.distance(this.getX(), this.getY()) <= p2.distance(this.getX(), this.getY())) ? p1 : p2;
        } else {
            // Move to the enemy while having a secure distance.
            final double enemyDistance = enemy.distance(this.getX(), this.getY());
            final double safeDistanceFraction = (enemyDistance - this.getWidth() * 2) / enemyDistance;

            return new Point2D.Double((1 - safeDistanceFraction) * this.getX() + safeDistanceFraction * enemy.getX(),
                    (1 - safeDistanceFraction) * this.getY() + safeDistanceFraction * enemy.getY());
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
        final Status friend = this.m_friend.getLastStatus();
        return (friend != null && this.m_goal != null && this.getEnergy() < friend.getEnergy()
                && this.getTime() - friend.getTurn() < 10);
    }
}
