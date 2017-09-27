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
        private final double m_energy;
        private final long m_turn;

        public Status(Robot robot) {
            super(robot.getX(), robot.getY());
            this.m_energy = robot.getEnergy();
            this.m_turn = robot.getTime();
        }

        public Status(Robot robot, ScannedRobotEvent enemy) {
            super(robot.getX()
                    + Math.sin(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance(),
                    robot.getY() + Math.cos(Math.toRadians((robot.getHeading() + enemy.getBearing()) % 360))
                            * enemy.getDistance());

            this.m_energy = enemy.getEnergy();
            this.m_turn = robot.getTime();
        }

        public double getEnergy() {
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
    }

    public static class Report implements Serializable {
        public enum ReportType {
            Team, Enemy;
        }

        private final Status m_status;
        private final ReportType m_type;

        public Report(ReportType type, Status status) {
            this.m_status = status;
            this.m_type = type;
        }

        public Status getStatus() {
            return m_status;
        }

        public ReportType GetType() {
            return m_type;
        }
    }

    private History m_friend, m_enemy;

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
        }

        this.m_enemy.addStatus(new Status(this, e));

        try {
            this.broadcastMessage(new Report(Report.ReportType.Team, new Status(this)));
            this.broadcastMessage(new Report(Report.ReportType.Enemy, this.m_enemy.getLastStatus()));
        } catch (Exception ex) {
            this.out.println("[ERROR] Error during result transmission");
        }

        final double radarTurn = this.getHeadingRadians() + e.getBearingRadians() - this.getRadarHeadingRadians();
        this.setTurnRadarRightRadians(1.95 * Utils.normalRelativeAngle(radarTurn));

        final double gunTurn = this.getHeadingRadians() + e.getBearingRadians() - this.getGunHeadingRadians();
        this.setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));

        this.execute();
    }

    @Override
    public void onSkippedTurn(SkippedTurnEvent event) {
        this.out.println("[ERROR] Skipped turn!");
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Report report = (Report) event.getMessage();
        switch (report.GetType()) {
        case Team:
            this.m_friend.addStatus(report.getStatus());
            break;
        case Enemy:
            this.m_enemy.addStatus(report.getStatus());
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
    }
}
