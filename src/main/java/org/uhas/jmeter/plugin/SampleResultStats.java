package org.uhas.jmeter.plugin;

import org.apache.jmeter.samplers.SampleResult;
import org.uhas.jmeter.util.Stat;

/**
 * Most of the code was taken from the @see {@link org.apache.jmeter.util.Calculator}. Modifications were made for accessing the class only once at
 * the end. <br>
 * Calculates various items that don't require all previous results to be saved: - mean = average - standard deviation - minimum - maximum
 */
public class SampleResultStats {

  private Stat time = new Stat(); 
  private Stat bytes = new Stat(); 
  private Stat latency = new Stat(); 
      
  private final String label;
  private int errors = 0;

  private long startTime = 0;
  private long endTime = 0;

  public SampleResultStats() {
    this( "" );
  }

  public SampleResultStats( String label ) {
    this.label = label;
  }

  /**
   * Add details for a sample result, which may consist of multiple samples. Updates the number of bytes read, error count, startTime and elapsedTime
   * 
   * @param res
   *          the sample result; might represent multiple values
   * @see #addValue(long, int)
   */
  public void addSample( SampleResult res ) {
    int sampleCount = res.getSampleCount();
    
    bytes.addValue( res.getBytes(), sampleCount);
    time.addValue( res.getTime(), sampleCount );
    latency.addValue(res.getLatency(), sampleCount);
    
    errors += res.getErrorCount(); // account for multiple samples
    
    if( startTime == 0 ) { // not yet intialised
      startTime = res.getStartTime();
    } else {
      startTime = Math.min( startTime, res.getStartTime() );
    }
    endTime = Math.max( endTime, res.getEndTime() );
  }

  public String getLabel() {
    return label;
  }

  public int getErrors() {
    return errors;
  }

  public long getElapsedTime() {
    if( endTime > 0 ) {
      return endTime - startTime;
    }

    return 0;
  }
  
  public Stat getTimeStat() {
    return time;
  }

  public Stat getBytesStat() {
    return bytes;
  }

  public Stat getLatencyStat() {
    return latency;
  }
  
  public long getCount() {
    return time.getCount();
  }
}