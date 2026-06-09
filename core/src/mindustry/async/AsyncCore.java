package mindustry.async;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;

public class AsyncCore{
    //all processes to be executed each frame
    public final Seq<AsyncProcess> processes = Seq.with(
        new PhysicsProcess(),
        avoidance = new AvoidanceProcess()
    );

    //futures to be awaited
    private final Seq<Future<?>> futures = new Seq<>();

    private ExecutorService executor;

    public AsyncCore(){
        Events.on(WorldLoadEvent.class, e -> {
            complete();
            for(AsyncProcess p : processes){
                p.init();
            }
        });

        Events.on(ResetEvent.class, e -> {
            complete();
            for(AsyncProcess p : processes){
                p.reset();
            }
        });
    }

    private void innerSyncBegin() {
        // for(AsyncProcess p : processes){
        //     p.begin();
        // }
    }
    private void innerSubmit() {
        //for(AsyncProcess p : processes){
    }
    public void begin(){
        if(state.isPlaying()){
            //sync begin
            // innerSyncBegin();
            // for(AsyncProcess p : processes){
            //     p.begin();
            // }
            for (int i = 0; i < processes.size; i++){
                processes.items[i].begin();
            }

            futures.clear();

            //init executor with size of potentially-modified process list
            if(executor == null){
                executor = Executors.newFixedThreadPool(processes.size, r -> {
                    Thread thread = new Thread(r, "AsyncLogic-Thread");
                    thread.setDaemon(true);
                    thread.setUncaughtExceptionHandler((t, e) -> Threads.throwAppException(e));
                    return thread;
                });
            }

            //submit all tasks
            // for(AsyncProcess p : processes){
            //     if(p.shouldProcess()){
            //         futures.add(executor.submit(p::process));
            //     }
            // }
            for(int i = 0; i < processes.size; i++){
                var p = processes.items[i];
                if(p.shouldProcess()){
                    futures.add(executor.submit(p::process));
                }
            }
            // innerSubmit();
        }
    }

    public void end(){
        if(state.isPlaying()){
            complete();

            //sync end (flush data)
            for(AsyncProcess p : processes){
                p.end();
            }
        }
    }

    private void complete(){
        //wait for all threads to stop processing
        for(var future : futures){
            try{
                future.get();
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }

        //clear processed futures
        futures.clear();
    }
}
