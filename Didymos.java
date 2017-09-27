package didymos;

import robocode.*;
import robocode.util.Utils;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayDeque;

public class Didymos extends TeamRobot {

    public static class Status extends Point2D.Double implements Serializable {
        private final double m_energy, m_heading;
        private final long m_turn;

        public Status(Robot robot) {
            super(robot.getX(), robot.getY());
            this.m_energy = robot.getEnergy();
            this.m_turn = robot.getTime();
            this.m_heading = robot.getHeading();
        }

        public Status(Robot robot, ScannedRobotEvent enemy) {
            super(robot.getX()
                    + Math.sin(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance(),
                    robot.getY() + Math.cos(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360))
                            * enemy.getDistance());

            this.m_energy = enemy.getEnergy();
            this.m_turn = robot.getTime();
            this.m_heading = enemy.getHeading();
        }

        public double getEnergy() {
            return m_energy;
        }

        public double getHeading() {
            return m_energy;
        }

        public long getTurn() {
            return m_turn;
        }
    }

    public static class History {
        private ArrayDeque<Status> m_status;

        public History() {
            m_status = new ArrayDeque<>();
        }

        public boolean addStatus(Status status) {
            if (this.m_status.size() > 0 && this.getLastStatus().getTurn() >= status.getTurn()) {
                return false;
            }

            if (this.m_status.size() > 10) {
                this.m_status.pollLast();
            }

            this.m_status.push(status);
            return true;
        }

        public Status getLastStatus() {
            return this.m_status.peekFirst();
        }

        public Point2D.Double predictPosition(final long turn) {
            if (this.m_status.size() < 2) {
                return this.getLastStatus();
            }

            // This algorithm is highly inspired by IBM (https://www.ibm.com/developerworks/library/j-circular/) 
            final Status last = this.getLastStatus(), secondLast = (Status) (this.m_status.toArray()[1]);
            final double headingChanged = last.getHeading() - secondLast.getHeading();
            final double diff = turn - last.getTurn();
            final double speed = last.distanceSq(secondLast) / (last.getTurn() - secondLast.getTurn());

            double newX, newY;
            if (Math.abs(headingChanged) > 0.00001) {
                // Choose circular targetting...
                double radius = speed / headingChanged;
                double tothead = diff * headingChanged;
                newY = last.getY() + (Math.sin(last.getHeading() + tothead) * radius)
                        - (Math.sin(last.getHeading()) * radius);
                newX = last.getX() + (Math.cos(last.getHeading()) * radius)
                        - (Math.cos(last.getHeading() + tothead) * radius);
            } else {
                // ... or the linear one.
                newY = last.getY() + Math.cos(last.getHeading()) * speed * diff;
                newX = last.getX() + Math.sin(last.getHeading()) * speed * diff;
            }

            return new Point2D.Double(newX, newY);
        }
    }

    public static class Report<E extends Serializable> implements Serializable {
        public enum ReportType {
            CurrentPosition, GoalPosition, Enemy;
        }

        private final E m_status;
        private final ReportType m_type;

        public Report(ReportType type, E status) {
            this.m_status = status;
            this.m_type = type;
        }

        public E getPayload() {
            return m_status;
        }

        public ReportType GetType() {
            return m_type;
        }
    }

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
        if (this.isTeammate(e.getName())) {
            this.m_friend.addStatus(new Status(this, e));
            return;
        } else {
            this.m_enemy.addStatus(new Status(this, e));
        }

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

    private void handleRadar(ScannedRobotEvent e) {
        final double radarTurn = this.getHeadingRadians() + e.getBearingRadians() - this.getRadarHeadingRadians();
        this.setTurnRadarRightRadians(1.95 * Utils.normalRelativeAngle(radarTurn));
    }

    private void handleGun(ScannedRobotEvent e) {
        final double gunTurn = this.getHeadingRadians() + e.getBearingRadians() - this.getGunHeadingRadians();
        this.setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));

        if (e.getDistance() < 150 && this.getGunHeat() == 0 && Math.abs(e.getDistance()
                + (this.m_friend.getLastStatus().distance(this.getX(), this.getY()) - e.getDistance()) / 2) > this
                        .getWidth() * 2) {
            this.setFire(3);
        }
    }

    private void handleMovement(ScannedRobotEvent e) {
        final Point2D.Double nextGoal = this.getNextTarget();
        try {
            this.broadcastMessage(new Report<>(Report.ReportType.CurrentPosition, new Status(this)));
            this.broadcastMessage(new Report<>(Report.ReportType.Enemy, this.m_enemy.getLastStatus()));
            this.broadcastMessage(new Report<>(Report.ReportType.GoalPosition, nextGoal));
        } catch (Exception ex) {
            this.out.println("[ERROR] Error during result transmission");
        }

        this.goTo((int) nextGoal.getX(), (int) nextGoal.getY());
    }

    private Point2D.Double getNextTarget() {
        final Status friend = this.m_friend.getLastStatus(), enemy = this.m_enemy.getLastStatus();

        // TODO: CHECK FRIEND IS LIVING
        if (friend != null && this.getEnergy() < friend.getEnergy()) {
            final double xFactor = Math.tan(45) * -(this.m_goal.getY() - enemy.getY()),
                    yFactor = Math.tan(45) * (this.m_goal.getX() - enemy.getX());

            final Point2D.Double p1 = this
                    .getPointInBattlefield(new Point2D.Double(enemy.getX() + xFactor, enemy.getY() + yFactor));

            final Point2D.Double p2 = this
                    .getPointInBattlefield(new Point2D.Double(enemy.getX() - xFactor, enemy.getY() - yFactor));

            return (p1.distance(this.getX(), this.getY()) <= p2.distance(this.getX(), this.getY())) ? p1 : p2;
        } else {
            final double enemyDistance = enemy.distance(this.getX(), this.getY());
            final double safeDistanceFraction = (enemyDistance - this.getWidth() * 2) / enemyDistance;

            return new Point2D.Double((1 - safeDistanceFraction) * this.getX() + safeDistanceFraction * enemy.getX(),
                    (1 - safeDistanceFraction) * this.getY() + safeDistanceFraction * enemy.getY());
        }
    }

    private void goTo(int x, int y) {
        double a;
        this.setTurnRightRadians(
                Math.tan(a = Math.atan2(x -= (int) this.getX(), y -= (int) this.getY()) - this.getHeadingRadians()));
        this.setAhead(Math.hypot(x, y) * Math.cos(a));
    }

    private Point2D.Double getPointInBattlefield(Point2D.Double target) {
        final double x = Math.min(Math.max(this.getWidth(), target.getX()),
                this.getBattleFieldWidth() - this.getWidth());
        final double y = Math.min(Math.max(this.getHeight(), target.getY()),
                this.getBattleFieldHeight() - this.getHeight());
        return new Point2D.Double(x, y);
    }
}
