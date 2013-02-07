package org.uhas.jmeter.util;


/**
 * Calculates various items that don't require all previous results to be saved: 
 *   mean, standard deviation, minimum, and maximum
 */
public class Stat {

  private int count = 0;
  private long sum = 0;
  private long maximum = Long.MIN_VALUE;
  private long minimum = Long.MAX_VALUE;

  private double sumOfSquares = 0;

  public Stat() {
    clear();
  }

  public void clear() {
    maximum = Long.MIN_VALUE;
    minimum = Long.MAX_VALUE;
    sum = 0;
    sumOfSquares = 0;
    count = 0;
  }

  /**
   * Add the value for (possibly multiple) samples. Updates the count, sum, min, max, sumOfSqaures, mean and deviation.
   * 
   * @param newValue
   *          the total value for all the samples.
   * @param sampleCount
   *          number of samples included in the value
   */
  public void addValue( long value, int sampleCount ) {
    count += sampleCount;
    double currentVal = value;
    sum += currentVal;
    minimum = Math.min( value / sampleCount, minimum );
    maximum = Math.max( value / sampleCount, maximum );
    // For n values in an aggregate sample the average value = (val/n)
    // So need to add n * (val/n) * (val/n) = val * val / n
    sumOfSquares += ( currentVal * currentVal ) / ( sampleCount );
  }
  
  public double getMean() {
    return count > 0 ? (double) sum / count : 0;
  }

  public long getMin() {
    return minimum;
  }

  public long getMax() {
    return maximum;
  }

  public int getCount() {
    return count;
  }

  public double getStandardDeviation() {
    if( count > 0 ) {
      double mean = getMean();
      return Math.sqrt( ( sumOfSquares / count ) - ( mean * mean ) );
    }
    
    return 0;
  }


}