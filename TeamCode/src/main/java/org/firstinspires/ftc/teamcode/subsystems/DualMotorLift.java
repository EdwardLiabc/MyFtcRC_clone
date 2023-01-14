package org.firstinspires.ftc.teamcode.subsystems;

import android.util.Log;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.control.PIDCoefficients;
import com.acmerobotics.roadrunner.control.PIDFController;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.robot.Subsystem;

@Config
//synchronizes lifts, provides commands to move to a position
public class DualMotorLift implements Subsystem {
    //Hardware: 2 lift motors
    private DcMotorEx slideMotorL;
    private DcMotorEx slideMotorR;
    private static final double TICKS_PER_REV = 537.7; //fix this
    private static final double PULLEY_DIAMETER = 50.4 / 25.4; //update diameter of the pulley
    private final int HEIGHT_DIFF_TOLERANCE = inchToTicks(1.5); //(int) (0.3*TICKS_PER_REV / (PULLEY_DIAMETER * Math.PI));
    private Telemetry telemetry;
    private boolean targetReached = true;
    private final double FAST_POWER = 0.6;
    private final double SLOW_POWER = 0.3;
    public enum Mode {
        BOTH_MOTORS_PID,
        RIGHT_FOLLOW_LEFT
    };
    public Mode mode;
    // Public just to allow tuning through Dashboard
    public static double  UP_VELOCITY = 500;
    public static double[] LEVEL_HT = {0, 7, 18.0, 32.0, 2}; // in inches, please fine-tune
    //4 levels: 0 ground, 1 low, 2 middle, 3 high, 4 (minimum height for free chain bar movement)
                            //0:5.0

    private PIDFController pidfController;
    public static double kP = 0.2;
    public static double kI = 0.0000000001;
    public static double kD = 0.0;
    public static double kA = 0.0;
    public static double kV = 0.0;
    public static double kS = 0.0;


//TODO: fine-tune LEVEL-HT values.
    public DualMotorLift (Robot robot, Telemetry telemetry, Mode mode){
        this.mode = mode;
        this.telemetry = telemetry;
        slideMotorL = robot.getMotor("slideMotorL");
        slideMotorR = robot.getMotor("slideMotorR");
        slideMotorL.setTargetPositionTolerance(inchToTicks(0.3)); //HEIGHT_DIFF_TOLERANCE);
        slideMotorL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slideMotorR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slideMotorR.setDirection(DcMotorSimple.Direction.REVERSE);
        if (mode== Mode.RIGHT_FOLLOW_LEFT) {
            slideMotorL.setTargetPosition(0);
            slideMotorL.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            //slideMotorL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

            slideMotorR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        else{
            slideMotorL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            slideMotorR.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            PIDCoefficients coefficients = new PIDCoefficients();
            coefficients.kP = kP;
            coefficients.kI = kI;
            coefficients.kD = kD;
            pidfController = new PIDFController(coefficients, kV, kA, kS);
            pidfController.setOutputBounds(-1.0, 1.0);
        }
        Log.v("PIDLift: status: ", "init");
    }

    public void goToLevel(int level){
        //4 levels: 0 ground, 1 low, 2 middle, 3 high, 4 (minimum height for free chain bar movement)
        int targetPosition = inchToTicks(LEVEL_HT[level]);
        goToHt(targetPosition);
        Log.v("ChainBar", "going to level" + level);

    }
    //for going to non-junction heights
    public void goToHt(int ticks) {
        targetReached=false;
        if(mode==Mode.RIGHT_FOLLOW_LEFT) {
            slideMotorL.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            slideMotorL.setTargetPosition(ticks);
            slideMotorL.setVelocity(UP_VELOCITY);
            //fine tune velocity?
            //In case if i should set right motor right when i set left motor (prob not useful)
            slideMotorR.setVelocity(UP_VELOCITY);
        }
        else {
            pidfController.reset();
            pidfController.setTargetPosition(ticksToInches(ticks));
        }
        Log.v("Lift: status", String.format("Lift moving to height %f", ticksToInches(slideMotorL.getTargetPosition())));
    }

    public void adjustLift(int direction, boolean slow){
        targetReached=true;
        double power = SLOW_POWER;
        if(!slow){
            power = FAST_POWER;
        }
        if(mode == Mode.RIGHT_FOLLOW_LEFT) {
            slideMotorL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            slideMotorL.setPower(power*direction);
        }
        else{
            slideMotorL.setPower(power*direction);
            slideMotorR.setPower(power*direction);
        }
        Log.v("PIDLift: status: ", "adjust lift");
        //for using run-using-encoder mode
        //int current = slideMotorL.getCurrentPosition();
        // int targetPosition = current + inchToTicks(1)*direction;
        //slideMotorL.setTargetPosition(targetPosition);
    }

    public void stopMotor(){
        if(mode==Mode.RIGHT_FOLLOW_LEFT) {
            slideMotorL.setVelocity(0.0);
            slideMotorR.setVelocity(0.0);
        }
        slideMotorL.setPower(0);
        slideMotorR.setPower(0);
    }

    private void updateTargetReached() {
        //if it is already true, don't change it. only change when slide is set to a level
        double motorLVel = Math.abs(slideMotorL.getVelocity());
        double targetPos, currPos;

        motorLVel = Math.abs(slideMotorL.getVelocity());
        currPos = slideMotorL.getCurrentPosition();



        if (mode == Mode.RIGHT_FOLLOW_LEFT) {
            targetPos = slideMotorL.getTargetPosition();

            /*
            this.targetReached = (this.targetReached ||
                    (Math.abs(slideMotorL.getVelocity()) <= 20
                            && (Math.abs(slideMotorL.getTargetPosition() - slideMotorL.getCurrentPosition()) <= HEIGHT_DIFF_TOLERANCE)));

            */
        } else {
            targetPos = inchToTicks(pidfController.getTargetPosition());
            /*
            this.targetReached = (this.targetReached ||
                    (Math.abs(slideMotorL.getVelocity()) <= 20
                            && (Math.abs(inchToTicks(pidfController.getTargetPosition()) - slideMotorL.getCurrentPosition()) <= HEIGHT_DIFF_TOLERANCE)));
            */
        }
        this.targetReached = (this.targetReached || (motorLVel <= 20 && Math.abs(targetPos - currPos) <= HEIGHT_DIFF_TOLERANCE));
        //telemetry.addData("slideMotorL.getVelocity() ", Math.abs(slideMotorL.getVelocity()));
        //telemetry.addData("lastError ", ticksToInches((int)Math.abs(targetPos - currPos)));
        //telemetry.addData("targetReached ", this.targetReached);
        //telemetry.update();
    }

    public boolean isLevelReached(){
        return this.targetReached;
    }

    public int inchToTicks(double inches) {
        return (int) (inches * TICKS_PER_REV / (PULLEY_DIAMETER * Math.PI));
    }
    private double ticksToInches(int ticks){
        return ((double) ticks) / (TICKS_PER_REV / (PULLEY_DIAMETER * Math.PI));
    }

    public double getPosition(){
        return slideMotorL.getCurrentPosition() / (TICKS_PER_REV/(PULLEY_DIAMETER * Math.PI));
    }
    public boolean chainBarCanSwing(){
        Log.v("ChainBar: can swing?", ticksToInches(slideMotorL.getCurrentPosition())+"");
        return slideMotorL.getCurrentPosition()>=inchToTicks(LEVEL_HT[4]);
    }

    @Override
    public void update(TelemetryPacket packet) {

        updateTargetReached();
        if(mode==Mode.RIGHT_FOLLOW_LEFT) {
            double velocity = slideMotorL.getVelocity();
            slideMotorR.setVelocity(velocity);
        }//if target is reached and not in manual mode, set velocity of right motor to 0
        else{
            if (!isLevelReached()) {
                double measuredPosition = (double) ticksToInches(slideMotorL.getCurrentPosition());
                double powerFromPIDF = pidfController.update(measuredPosition);
                Log.v("PIDLift: Debug: ", String.format("Target pos: %4.2f, current pos: %4.2f, last error: %4.2f, velocity: %4.2f, set power to: %4.2f",
                        pidfController.getTargetPosition(), measuredPosition, pidfController.getLastError(), slideMotorL.getVelocity(), powerFromPIDF));
                slideMotorL.setPower(powerFromPIDF);
                slideMotorR.setPower(powerFromPIDF);
            }
        }
        //telemetry.addLine("Slide motor set to " + ticksToInches(slideMotorL.getTargetPosition()));
        //telemetry.addLine("current slide velocity: " + slideMotorL.getVelocity());
        //telemetry.addLine("current slide position: " + ticksToInches(slideMotorL.getCurrentPosition()));
        packet.put("target pos (inches)", ticksToInches(slideMotorL.getTargetPosition()));
        if (mode == Mode.BOTH_MOTORS_PID) {
            packet.put("PID target pos", pidfController.getTargetPosition());
        }
        packet.put("left velocity", slideMotorL.getVelocity());
        packet.put("right velocity", slideMotorR.getVelocity());
        packet.put("L pos (inches)", ticksToInches(slideMotorL.getCurrentPosition()));
        packet.put("R pos (inches)", ticksToInches(slideMotorR.getCurrentPosition()));
        //Log.v("PIDLift: power", String.format("Left power: %f, Right power: %f",slideMotorL.getPower(), slideMotorR.getPower()));
        //Log.v("PIDLift: modes", String.format("left mode: %s, right mode: %s", slideMotorL.getMode().toString(), slideMotorR.getMode().toString()));
        //packet.put("motor power", slideMotorL.getPower());
        packet.put("R motor mode", slideMotorR.getMode());
        //FtcDashboard.getInstance().sendTelemetryPacket(packet);
        telemetry.update();
    }
}
