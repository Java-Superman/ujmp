package org.uhas.jmeter.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.gui.UnsharedComponent;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.visualizers.Visualizer;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import org.uhas.jmeter.plugin.ThreadGroupsStats.ThreadGroupStats;
import org.uhas.jmeter.util.Stat;

public class NonGuiSummaryListener extends AbstractTestElement implements TestBean, SampleListener, Serializable, TestStateListener, Visualizer, UnsharedComponent, NoThreadClone,
    ThreadListener, TestIterationListener
{
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggingManager.getLoggerForClass();

  // bean properties
  private File file = new File( "summary-results.xml" );
  private String testId = "${__time(yyyyMMdd-HHmmss)}";
  private Collection<String> includedLabels = new ArrayList<String>(); 
  private Collection<String> excludedLabels = new ArrayList<String>();

  private Map<String, SampleResultStats> sampleStats = new HashMap<String, SampleResultStats>();
  private SampleResultStats summary = new SampleResultStats( "TOTALS" );
  private Collection<Pattern> includedRegEx = null; 
  private Collection<Pattern> excludedRegEx = null;
  
  
  private ThreadGroupsStats threadStats = new ThreadGroupsStats();
  
  private Date startDate;
  private Date endDate;

  public NonGuiSummaryListener() {
  }

  @Override
  public void sampleOccurred( SampleEvent e ) {
    SampleResult res = e.getResult();
    final String sampleLabel = res.getSampleLabel( false );
    SampleResultStats row = null;

    
    if( isLabelIncluded( sampleLabel ) ) {
      synchronized( sampleStats ) {
        row = sampleStats.get( sampleLabel );
        if( row == null ) {
          row = new SampleResultStats( sampleLabel );
          sampleStats.put( row.getLabel(), row );
        }
      }
  
      // synch is needed because multiple threads can update the counts.
      synchronized( row ) {
        row.addSample( res );
      }
  
      if( ! e.isTransactionSampleEvent() ) {
        synchronized( summary ) {
          summary.addSample( res );
        }
      }
    }
  }

  @Override
  public void sampleStarted( SampleEvent e ) {
  }

  @Override
  public void sampleStopped( SampleEvent e ) {
  }

  // TESTITTERATION
  
  @Override
  public void testIterationStart( LoopIterationEvent event ) {
    TestElement testElement = event.getSource();
    if( testElement instanceof AbstractThreadGroup ) {
      AbstractThreadGroup tg = (AbstractThreadGroup) testElement;
      if( ! tg.isDone() )
        threadStats.testIterationStart();
    }
  }
  
  // THREADLISTENER

  @Override
  public void threadStarted() {
    threadStats.threadStarted();
  }

  @Override
  public void threadFinished() {
    threadStats.threadFinished();
  }

  // TESTLISTENER

  @Override
  public void testStarted() {
    startDate = new Date();
  }

  @Override
  public void testStarted( String host ) {
    startDate = new Date();
  }

  @Override
  public void testEnded() {
    testEnded( getHostAddress() );
  }

  @Override
  public void testEnded( String host ) {
    endDate = new Date();

    try {
      // sort by label name
      SampleResultStats[] sortedCalcs = sampleStats.values().toArray( new SampleResultStats[sampleStats.size()] );
      Arrays.sort( sortedCalcs, new Comparator<SampleResultStats>() {
        @Override
        public int compare( SampleResultStats o1, SampleResultStats o2 ) {
          return o1.getLabel().compareTo( o2.getLabel() );
        }
      } );

      BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
      writer.append( "<?xml version='1.0' encoding='UTF-8'?>\n" );

      writer.write( "<test" );
      write( writer, "id", testId );
      write( writer, "host", host );
      write( writer, "startDate", startDate );
      write( writer, "endDate", endDate );
      write( writer, "startTime", startDate.getTime() );
      write( writer, "duration", endDate.getTime() - startDate.getTime() );
      writer.append( ">\n" );

      
      
      for(ThreadGroupStats tgStats : threadStats.getStats() ) {
        writer.write( "  <threads" );
        write( writer, "name", tgStats.getName() );
        write( writer, "count", tgStats.getStarted() );
        writer.append( ">\n" );
        write( writer, tgStats.getDurationStat(), "duration" );
        write( writer, tgStats.getIterationStat(), "iterations" );
        writer.append( "  </threads>\n" );
      }
      
      
      for( SampleResultStats sStat : sortedCalcs ) {
        write( writer, sStat, "sample", false );
      }

      write( writer, summary, "totals", true );
      
      
      writer.append( "</test>\n" );
      writer.close();
    } catch( Exception e ) {
      log.error( "Unable to create samples file", e );
    }
  }

  // ////////////////////////////////////////////////////
  // VISUALIZER METHODS

  @Override
  public void add( SampleResult sample ) {
  }

  @Override
  public boolean isStats() { // Needed by Visualizer interface
    return false;
  }

  // ////////////////////////////////////////////////////
  // SETTER/GETTER
  
  
  public File getFile() {
    return file;
  }

  public void setFile( File fileName ) {
    this.file = fileName;
  }

  public String getTestId() {
    return testId;
  }

  public void setTestId( String testId ) {
    this.testId = testId;
  }

  public Collection<String> getIncludedLabels() {
    return includedLabels;
  }

  public void setIncludedLabels( Collection<String> includedLabels ) {
    this.includedLabels = includedLabels;
    if(includedLabels != null ) {
      this.includedRegEx = new ArrayList<Pattern>();
      for( String regEx : includedLabels ) {
        this.includedRegEx.add( Pattern.compile( regEx ) );
      }
    }
  }

  public Collection<String> getExcludedLabels() {
    return excludedLabels;
  }

  public void setExcludedLabels( Collection<String> excludedLabels ) {
    this.excludedLabels = excludedLabels;
    if( excludedLabels != null ) {
      this.excludedRegEx = new ArrayList<Pattern>();
      for( String regEx : excludedLabels ) {
        this.excludedRegEx.add( Pattern.compile( regEx ) );
      }
    }
  }

  // HELPER METHODS

  private boolean isLabelIncluded( final String sampleLabel ) {
    // check if label is included
    boolean included = true;
    if( includedRegEx != null && ! includedRegEx.isEmpty() ) {
      included = false;
      for( Pattern regEx : includedRegEx ) {
        if( regEx.matcher( sampleLabel ).matches() ) {
          included = true;
          break;
        }
      }
    }

    if( included && excludedRegEx != null && ! excludedRegEx.isEmpty() ) {
      for( Pattern regEx : excludedRegEx ) {
        if( regEx.matcher( sampleLabel ).matches() ) {
          included = false;
          break;
        }
      }
    }
    return included;
  }

  private void write( Writer writer, String attribute, Object value ) throws IOException {
    writer.append( " " ).append( attribute ).append( "='" ).append( String.valueOf( value ) ).append( "'" );
  }

  private void write( Writer writer, SampleResultStats c, String name, boolean skipName ) throws IOException {
    writer.append( "  <" ).append( name );
    if( ! skipName )
      write( writer, "name", c.getLabel() );
    write( writer, "requests", c.getCount() );
    write( writer, "errors", c.getErrors() );
    writer.append( ">\n" );
    write( writer, c.getTimeStat(), "time" );
    write( writer, c.getBytesStat(), "bytes" );
    write( writer, c.getLatencyStat(), "latency" );
    writer.append( "  </" ).append( name ).append( ">\n" );
  }

  private void write( Writer writer, Stat s, String name ) throws IOException {
    write(writer,s,name, false);
  }

  private void write( Writer writer, Stat s, String name, boolean includeCount ) throws IOException {
    DecimalFormat df = new DecimalFormat( "#0.00" );
    writer.append( "    <" ).append( name );
    if( includeCount ) 
      write( writer, "count", s.getCount() );
    
    write( writer, "avg", df.format( s.getMean() ) );
    write( writer, "min", s.getMin() );
    write( writer, "max", s.getMax() );
    write( writer, "std", df.format( s.getStandardDeviation() ) );
    writer.append( " />\n" );
  }

  private String getHostAddress() {
    try {
      String rVal = null;
      outer: for( Enumeration<?> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface ni = (NetworkInterface)en.nextElement();
        if( !"lo".equals( ni.getName() ) ) {
          for( Enumeration<?> eIA = ni.getInetAddresses(); eIA.hasMoreElements(); ) {
            InetAddress ia = (InetAddress)eIA.nextElement();
            if( !ia.isLoopbackAddress() ) {
              rVal = ia.getCanonicalHostName();
              if( rVal == null || "".equals( rVal ) ) {
                rVal = ia.getHostAddress();
              }

              if( rVal == null || "".equals( rVal ) )
                break outer;
            }
          }
        }
      }
      return rVal;
    } catch( Exception e ) {
    }
    return "Unknown";
  }

}
