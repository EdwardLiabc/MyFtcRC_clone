package org.firstinspires.ftc.teamcode.subsystems;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.robot.Robot;

public class CrabRobot extends Robot {
    public final SimpleMecanumDrive mecanumDrive;
    public final ScoringSystem scoringSystem;
    public final RobotColorSensor robotcolorsensor;
    public final RobotDistanceSensor robotdistancesensor;
    public  SmartGamepad smartGamepad1;
    public  SmartGamepad smartGamepad2;

    public CrabRobot(LinearOpMode opMode, boolean autoMode) {
        super(opMode);
        mecanumDrive = new SimpleMecanumDrive(this);
        Log.v("update", "registering mecanumDrive: " + mecanumDrive.getClass().getSimpleName());
        registerSubsystem(mecanumDrive);
        scoringSystem = new ScoringSystem(this, autoMode, opMode.telemetry);
        Log.v("update", "registering scoringSystem: " + scoringSystem.getClass().getSimpleName());
        registerSubsystem(scoringSystem);
        robotcolorsensor = new RobotColorSensor(this, opMode.telemetry);
        Log.v("update", "registering robotcolorsensor: " + robotcolorsensor.getClass().getSimpleName());
        registerSubsystem(robotcolorsensor);
        robotdistancesensor = new RobotDistanceSensor(this, opMode.telemetry);
        registerSubsystem(robotdistancesensor);
    }

    public void addGamepads(Gamepad g1, Gamepad g2){
        smartGamepad1 = new SmartGamepad(g1);
        if (smartGamepad1 != null ) {
            Log.v("update", "registering smartGamepad1: " + smartGamepad1.getClass().getSimpleName());
            registerSubsystem(smartGamepad1);
        }
        smartGamepad2 = new SmartGamepad(g2);
        if (smartGamepad2 != null) {
            Log.v("update", "registering smartGamepad2: " + smartGamepad2.getClass().getSimpleName());
            registerSubsystem(smartGamepad2);
        }
    }
}
