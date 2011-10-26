package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A report about a particular tested URI.
 * Contains number of samples.
 * 
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements ModelObject,
    Comparable<UriReport> {

  public final static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

  /**
   * Individual HTTP invocations to this URI and how they went.
   */
  private final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

  /**
   * The parent object to which this object belongs.
   */
  private final PerformanceReport performanceReport;

  /**
   * Escaped {@link #uri} that doesn't contain any letters that cannot be used
   * as a token in URL.
   */
  private final String staplerUri;

  private String uri;

  private double throughput = -1;

  private double operationsPerSecond = -1;
  
  private long totalDuration = -1;

  UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
    this.performanceReport = performanceReport;
    this.staplerUri = staplerUri;
    this.uri = uri;
  }

  public void addHttpSample(HttpSample httpSample) {
    httpSampleList.add(httpSample);
  }

  public int compareTo(UriReport uriReport) {
    if (uriReport == this) {
      return 0;
    }
    return uriReport.getUri().compareTo(this.getUri());
  }

  public int countErrors() {
    int nbError = 0;
    for (HttpSample currentSample : httpSampleList) {
      if (!currentSample.isSuccessful()) {
        nbError++;
      }
    }
    return nbError;
  }

  public double errorPercent() {
    return ((double) countErrors()) / size() * 100;
  }

  public long getAverage() {
    long average = 0;
    for (HttpSample currentSample : httpSampleList) {
      average += currentSample.getDuration();
    }
    return average / size();
  }

  public long get90Line() {
    long result = 0;
    Collections.sort(httpSampleList);
    if (httpSampleList.size() > 0) {
      result = httpSampleList.get((int) (httpSampleList.size() * .9)).getDuration();
    }
    return result;
  }

  public long getMedian() {
    long result = 0;
    Collections.sort(httpSampleList);
    if (httpSampleList.size() > 0) {
      result = httpSampleList.get((int) (httpSampleList.size() * .5)).getDuration();
    }
    return result;
  }

  public AbstractBuild<?, ?> getBuild() {
    return performanceReport.getBuild();
  }

  public String getDisplayName() {
    return getUri();
  }

  public List<HttpSample> getHttpSampleList() {
    return httpSampleList;
  }

  public PerformanceReport getPerformanceReport() {
    return performanceReport;
  }

  public long getMax() {
    long max = Long.MIN_VALUE;
    for (HttpSample currentSample : httpSampleList) {
      max = Math.max(max, currentSample.getDuration());
    }
    return max;
  }

  public long getMin() {
    long min = Long.MAX_VALUE;
    for (HttpSample currentSample : httpSampleList) {
      min = Math.min(min, currentSample.getDuration());
    }
    return min;
   }

  /**
  * Return the Throughput in KBytesBytes / Sec 
  * @return
  */
  public double getThroughput() {
      if (throughput == -1) calculateThroughputs();
      return throughput;
  }
  
  private void calculateThroughputs() {
      long totalSize = 0;
      long startTime = Long.MAX_VALUE;
      long endTime = Long.MIN_VALUE;
      
      // The Timestamp of Each Sample donates the momentan where sampling stopped
      // we also need the duration of the first Sample to get the complete duration
      long durationOfFirstSample = 0;
      
      for (HttpSample currentSample : httpSampleList) {
          totalSize += currentSample.getSize();
          if (startTime  > currentSample.getDate().getTime()) {
              startTime = currentSample.getDate().getTime();
              durationOfFirstSample  = currentSample.getDuration();
          }
          endTime = Math.max(endTime, currentSample.getDate().getTime());
      }
      totalDuration = endTime - startTime + durationOfFirstSample;
      if (totalDuration == 0) {
          throughput = 0;
          operationsPerSecond = 0;
      } else {
          throughput =  (totalSize/ 1024.0) / (totalDuration / 1000.0) ;
          operationsPerSecond =  1000* size() / (double) totalDuration;
      }
      
  }
  public double getOperationsPerSecond() {
      if (operationsPerSecond == -1) calculateThroughputs();
      return operationsPerSecond;  
  }
  
  public String getOperationsPerSecondAsString() {
     return String.format ("%2.2f", getOperationsPerSecond());
  }
  
  public String getThroughputAsString() {
      return String.format ("%2.2f", getThroughput());
  }
  
  public String getStaplerUri() {
    return staplerUri;
  }

  public String getUri() {
    return uri;
  }

  public boolean isFailed() {
    return countErrors() != 0;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public int size() {
    return httpSampleList.size();
  }

  public String encodeUriReport() throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder(120);
    sb.append(performanceReport.getReportFileName()).append(
        GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(
        END_PERFORMANCE_PARAMETER);
    return URLEncoder.encode(sb.toString(), "UTF-8");
  }

}
