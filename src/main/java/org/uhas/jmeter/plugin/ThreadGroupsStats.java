package org.uhas.jmeter.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.threads.JMeterContextService;
import org.uhas.jmeter.util.Stat;

public class ThreadGroupsStats {

  private Map<String, ThreadGroupStats> threadGroups = new HashMap<String, ThreadGroupStats>();

  public ThreadGroupsStats() {
  }

  public void testIterationStart() {
    threadGroups.get( getThreadGroupName() ).testIterationStart();
  }

  public void threadStarted() {
    String tgName = getThreadGroupName();
    ThreadGroupStats tStat = null;

    synchronized( threadGroups ) {
      tStat = threadGroups.get( tgName );
      if( tStat == null ) {
        tStat = new ThreadGroupStats( tgName );
        threadGroups.put( tgName, tStat );
      }
    }

    tStat.threadStarted();
  }

  public void threadFinished() {
    ThreadGroupStats tStat = threadGroups.get( getThreadGroupName() );
    tStat.threadFinished();
  }

  public Collection<ThreadGroupStats> getStats() {
    return threadGroups.values();
  }

  // HELPER METHODS
  private String getThreadGroupName() {
    return JMeterContextService.getContext().getThreadGroup().getName();
  }

  // INNER CLASSES

  public static class ThreadGroupStats {
    ThreadLocal<ThreadData> tlData = new ThreadLocal<ThreadData>();
    Stat duration = new Stat();
    Stat iteration = new Stat();
    String name = "";
    volatile int started = 0;

    ThreadGroupStats( String name ) {
      this.name = name;
    }

    void testIterationStart() {
      tlData.get().iterations++;
    }

    void threadStarted() {
      tlData.set( new ThreadData( System.currentTimeMillis() ) );
      started++;
    }

    void threadFinished() {
      long now = System.currentTimeMillis();
      ThreadData data = tlData.get();

      duration.addValue( now - data.startTime, 1 );
      iteration.addValue( data.iterations, 1 );
      // value no longer needed
      tlData.remove();
    }

    public Stat getDurationStat() {
      return duration;
    }

    public Stat getIterationStat() {
      return iteration;
    }

    public String getName() {
      return name;
    }
    
    /**
     * @return Number of threads that have been started
     */
    public int getStarted() {
      return started;
    }
    
    /**
     * Sometime threadFinished is not called
     * @return Number of threads that have been finished
     */
    public int getFinished() {
      return duration.getCount();
    }
    
  }

  static class ThreadData {
    long startTime;
    long iterations;

    ThreadData( long startTime ) {
      this.startTime = startTime;
    }
  }

}