package cn.edu.ddlut.arearth;

/**
 * Created by WuJie on 2017/3/8.
 */
public class Info {
    public double  ballId;
    public double yaw;
    public double pitch ;
    public double roll;

    public Info(double ballId, double yaw, double pitch, double roll) {
        this.ballId = ballId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }
}