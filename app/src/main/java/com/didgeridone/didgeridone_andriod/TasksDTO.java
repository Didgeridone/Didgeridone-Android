package com.didgeridone.didgeridone_andriod;

import java.util.List;
/**
 * Created by IsaacL on 2/16/16.
 */
public class TasksDTO {
    int task_id;
    String name;
    int y_lat;
    int x_long;
    int radius;
    Boolean task_done;
    Boolean enter;

    public int getTask_id() { return task_id;}
    public void setTask_id() {this.task_id = task_id;}
    public String getName() { return name;}
    public void setName () {this.name = name;}
    public int getY_lat () { return y_lat;}
    public void setY_lat () {this.y_lat = y_lat;}
    public int getX_long() { return x_long;}
    public void  setX_long() {this.x_long = x_long;}
    public int getRadius () { return radius;}
    public void setRadius () {this.radius = radius;}
    public Boolean getTask_done() {return task_done;}
    public void setTask_done() {this.task_done = task_done;}
    public Boolean getEnter() {return enter;}
    public void setEnter() {this.enter = enter;}

}
