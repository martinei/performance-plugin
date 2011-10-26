package hudson.plugins.performance;

import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Root object of a performance report.
 * Maps Filenames to PerformanceReports.
 */
public class PerformanceReportMap implements ModelObject {
  /**
   * The {@link PerformanceBuildAction} that this report belongs to.
   */
  private transient PerformanceBuildAction buildAction;

  /**
   * {@link PerformanceReport}s are keyed by {@link PerformanceReport#reportFileName}
   *
   * Test names are arbitrary human-readable and URL-safe string that identifies an individual report.
   */
  private Map<String, PerformanceReport> performanceReportMap = new LinkedHashMap<String, PerformanceReport>();

  private static final String PERFORMANCE_REPORTS_DIRECTORY = "performance-reports";

  /**
   * Parses the reports and build a {@link PerformanceReportMap}.
   *
   * @throws IOException
   *      If a report fails to parse.
   */
  PerformanceReportMap(PerformanceBuildAction buildAction, TaskListener listener)
      throws IOException {
    this.buildAction = buildAction;

    File repo = new File(getBuild().getRootDir(),
        PerformanceReportMap.getPerformanceReportDirRelativePath());

    // files directly under the directory are for JMeter, for compatibility reasons.
    File[] files = repo.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return !f.isDirectory();
      }
    });
    // this may fail, if the build itself failed, we need to recover gracefully
    if (files != null) {
      addAll(new JMeterParser("").parse(buildAction.getBuild(),
          Arrays.asList(files), listener));
    }

    // otherwise subdirectory name designates the parser ID.
    File[] dirs = repo.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return f.isDirectory();
      }
    });
    // this may fail, if the build itself failed, we need to recover gracefully
    if (dirs != null) {
      for (File dir : dirs) {
        PerformanceReportParser p = buildAction.getParserByDisplayName(dir.getName());
        if (p != null) {
          addAll(p.parse(getBuild(), Arrays.asList(dir.listFiles()), listener));
        }
      }
    }
  }

  private void addAll(Collection<PerformanceReport> reports) {
    for (PerformanceReport r : reports) {
      r.setBuildAction(buildAction);
      performanceReportMap.put(r.getReportFileName(), r);
    }
  }

  public AbstractBuild<?, ?> getBuild() {
    return buildAction.getBuild();
  }

  PerformanceBuildAction getBuildAction() {
    return buildAction;
  }

  public String getDisplayName() {
    return Messages.Report_DisplayName();
  }

  public List<PerformanceReport> getPerformanceListOrdered() {
    List<PerformanceReport> listPerformance = new ArrayList<PerformanceReport>(
        getPerformanceReportMap().values());
    Collections.sort(listPerformance);
    return listPerformance;
  }

  public Map<String, PerformanceReport> getPerformanceReportMap() {
    return performanceReportMap;
  }

  /**
   * <p>
   * Give the Performance report with the parameter for name in Bean
   * </p>
   * 
   * @param performanceReportName
   * @return
   */
  public PerformanceReport getPerformanceReport(String performanceReportName) {
    return performanceReportMap.get(performanceReportName);
  }

  /**
   * Get a URI report within a Performance report file
   * 
   * @param uriReport
   *            "Performance report file name";"URI name"
   * @return
   */
  public UriReport getUriReport(String uriReport) {
    if (uriReport != null) {
      String uriReportDecoded;
      try {
        uriReportDecoded = URLDecoder.decode(uriReport.replace(
            UriReport.END_PERFORMANCE_PARAMETER, ""), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        return null;
      }
      StringTokenizer st = new StringTokenizer(uriReportDecoded,
          GraphConfigurationDetail.SEPARATOR);
      return getPerformanceReportMap().get(st.nextToken()).getUriReportMap().get(
          st.nextToken());
    } else {
      return null;
    }
  }

  public String getUrlName() {
    return "performanceReportList";
  }

  void setBuildAction(PerformanceBuildAction buildAction) {
    this.buildAction = buildAction;
  }

  public void setPerformanceReportMap(
      Map<String, PerformanceReport> performanceReportMap) {
    this.performanceReportMap = performanceReportMap;
  }

  public static String getPerformanceReportFileRelativePath(
      String parserDisplayName, String reportFileName) {
    return getRelativePath(parserDisplayName, reportFileName);
  }

  public static String getPerformanceReportDirRelativePath() {
    return getRelativePath();
  }

  private static String getRelativePath(String... suffixes) {
    StringBuilder sb = new StringBuilder(100);
    sb.append(PERFORMANCE_REPORTS_DIRECTORY);
    for (String suffix : suffixes) {
      sb.append(File.separator).append(suffix);
    }
    return sb.toString();
  }

  /**
   * <p>
   * Verify if the PerformanceReport exist the performanceReportName must to be like it
   * is in the build
   * </p>
   * 
   * @param performanceReportName
   * @return boolean
   */
  public boolean isFailed(String performanceReportName) {
    return getPerformanceReport(performanceReportName) == null;
  }
}
