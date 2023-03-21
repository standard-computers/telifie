package com.telifie.Models.Actions;

import java.util.List;

public class Queue {

    private List<String> queue;
    private boolean started = false;

    public Queue(List<String> queue) {
        this.queue = queue;
    }

    public void queue(String uri){
        this.queue.add(uri);
    }

    public void start(){
        this.started = true;
        if(queue.isEmpty()){
            Out.console("queue is empty");
        }else{

        }
    }

    public void stop(){
        this.started = false;
    }

    public void parse(int queueIndex){

    }

}
